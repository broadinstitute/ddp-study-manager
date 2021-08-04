package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.handlers.util.MedicalInfo;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.user.UserDto;
import org.broadinstitute.dsm.model.PDF.DownloadPDF;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.*;
import org.broadinstitute.dsm.util.DDPRequestUtil;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.UserUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class DownloadPDFRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadPDFRoute.class);

    public static final String PDF = "/pdf";
    public static final String BUNDLE = "/bundle";

    private static final String COVER = "cover";
    private static final String TISSUE = "tissue";
    private static final String IRB = "irb";
    private static final String JSON_START_DATE = "startDate";
    private static final String JSON_END_DATE = "endDate";
    private final String PDF_ROLE= "pdf_download";


    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        logger.info(request.url());
        QueryParamsMap queryParams = request.queryMap();
        String realm = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        if (StringUtils.isNotBlank(realm)) {
            if (UserUtil.checkUserAccess(realm, userId, PDF_ROLE)) {
                if (request.url().contains(RoutePath.DOWNLOAD_PDF)) {
                    String requestBody = request.body();
                    if (StringUtils.isNotBlank(requestBody)) {
                        JSONObject jsonObject = new JSONObject(requestBody);
                        String userIdR = (String) jsonObject.get(RequestParameter.USER_ID);
                        Long userIdRequest = Long.parseLong(userIdR);
                        if (!userId.equals(userIdR)) {
                            throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdR);
                        }

                        DownloadPDF downloadPDFRequest = new DownloadPDF(requestBody);
                        UserDto user = new UserDao().get(userIdRequest).orElseThrow();
                        return downloadPDFRequest.getPDFs(response, user, realm, requestBody);

                    }
                    else {
                        response.status(500);
                        throw new RuntimeException("Error missing requestBody");
                    }
                }
                else {
                    String ddpParticipantId = null;
                    if (queryParams.value(RequestParameter.DDP_PARTICIPANT_ID) != null) {
                        ddpParticipantId = queryParams.get(RequestParameter.DDP_PARTICIPANT_ID).value();
                    }
                    if (StringUtils.isNotBlank(ddpParticipantId)) {
                        DDPInstance instance = DDPInstance.getDDPInstance(realm);
                        Map<String, Map<String, Object>> participantESData = ElasticSearchUtil.getFilteredDDPParticipantsFromES(instance,
                                ElasticSearchUtil.BY_GUID + ddpParticipantId);
                        //filter ES with GUID
                        if (participantESData != null && !participantESData.isEmpty()) {
                            return returnPDFS(participantESData, ddpParticipantId);
                        }
                        else {
                            //pt was not found with GUID check for legacyAltPid
                            participantESData = ElasticSearchUtil.getFilteredDDPParticipantsFromES(instance, ElasticSearchUtil.BY_LEGACY_ALTPID + ddpParticipantId);
                            return returnPDFS(participantESData, ddpParticipantId);
                        }
                    }else{// it is the misc download
                        return getPDFs(realm);
                    }
                }
            } else {
                return new Result(500, UserErrorMessages.NO_RIGHTS);
            }
        }
        response.status(500);
        throw new RuntimeException("Something went wrong");
    }

    public static Object returnPDFS(Map<String, Map<String, Object>> participantESData, @NonNull String ddpParticipantId) {
        if (participantESData != null && !participantESData.isEmpty() && participantESData.size() == 1) {
            Map<String, Object> participantData = participantESData.get(ddpParticipantId);
            if (participantData != null) {
                Map<String, Object> dsm = (Map<String, Object>) participantData.get(ElasticSearchUtil.DSM);
                if (dsm != null) {
                    Object pdf = dsm.get(ElasticSearchUtil.PDFS);
                    return pdf;
                }
            }
            else {
                for (Object value : participantESData.values()) {
                    participantData = (Map<String, Object>) value;
                    //check that it is really right participant
                    if (participantData != null) {
                        Map<String, Object> profile = (Map<String, Object>) participantData.get(ElasticSearchUtil.PROFILE);
                        if (profile != null) {
                            String guid = (String) profile.get(ElasticSearchUtil.GUID);
                            if (ddpParticipantId.equals(guid)) {
                                Map<String, Object> dsm = (Map<String, Object>) participantData.get(ElasticSearchUtil.DSM);
                                if (dsm != null) {
                                    Object pdf = dsm.get(ElasticSearchUtil.PDFS);
                                    return pdf;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }





    private MedicalInfo getMedicalInfo(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId) {
        //get medical record information from ddp
        MedicalInfo medicalInfo = null;
        String dsmRequest = ddpInstance.getBaseUrl() + RoutePath.DDP_INSTITUTION_PATH.replace(RequestParameter.PARTICIPANTID, ddpParticipantId);
        try {
            medicalInfo = DDPRequestUtil.getResponseObject(MedicalInfo.class, dsmRequest, ddpInstance.getName(), ddpInstance.isHasAuth0Token());
            if (medicalInfo == null) {
                throw new RuntimeException("Couldn't get participant information " + ddpInstance.getName());
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't get participants and institutions for ddpInstance " + ddpInstance.getName(), e);
        }
        return medicalInfo;
    }



    public List<String> getPDFs(@NonNull String realm) {
        List<String> listOfPDFs = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            String query = TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GET_ROLES_LIKE);
            query = query.replace("%1", DBConstants.PDF_DOWNLOAD);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        listOfPDFs.add(rs.getString(DBConstants.NAME));
                    }
                }
                catch (SQLException e) {
                    throw new RuntimeException("Error getting pdfs for " + realm, e);
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Couldn't get pdfs for realm " + realm, results.resultException);
        }
        logger.info("Found " + listOfPDFs.size() + " pdfs for realm " + realm);
        return listOfPDFs;
    }


}
