package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.user.UserDto;
import org.broadinstitute.dsm.model.PDF.DownloadPDF;
import org.broadinstitute.dsm.model.PDF.MiscPDFDownload;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.UserUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;


public class DownloadPDFRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadPDFRoute.class);

    public static final String PDF = "/pdf";
    public static final String BUNDLE = "/bundle";

    private final String PDF_ROLE = "pdf_download";


    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        logger.info(request.url());
        QueryParamsMap queryParams = request.queryMap();
        String realm = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        if (StringUtils.isNotBlank(realm)) {
            String requestBody = request.body();
            UserUtil userUtil = new UserUtil();
            String userIdR = userUtil.getUserId(request);
            if (userUtil.checkUserAccess(realm, userId, PDF_ROLE, userIdR)) {
                if (request.url().contains(RoutePath.DOWNLOAD_PDF)) {
                    if (StringUtils.isNotBlank(requestBody)) {
                        Long userIdRequest = Long.parseLong((String) new JSONObject(requestBody).get(RequestParameter.USER_ID));
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
                    MiscPDFDownload miscPDFDownload = new MiscPDFDownload();
                    String ddpParticipantId = null;
                    if (queryParams.value(RequestParameter.DDP_PARTICIPANT_ID) != null) {
                        ddpParticipantId = queryParams.get(RequestParameter.DDP_PARTICIPANT_ID).value();
                    }
                    if (StringUtils.isNotBlank(ddpParticipantId)) {
                        return miscPDFDownload.returnPDFS(ddpParticipantId, realm);
                    }
                    else {// it is the misc download
                        return miscPDFDownload.getPDFRole(realm);
                    }
                }
            }
            else {
                response.status(500);
                return UserErrorMessages.NO_RIGHTS;
            }
        }
        response.status(500);
        throw new RuntimeException("Realm was missing from the request");
    }





}
