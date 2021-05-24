package org.broadinstitute.dsm.route;

import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.db.TransactionWrapper;
import org.broadinstitute.ddp.exception.FileProcessingException;
import org.broadinstitute.ddp.handlers.util.MedicalInfo;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.ddp.util.GoogleBucket;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.InstanceSettings;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.files.CoverPDFProcessor;
import org.broadinstitute.dsm.files.PDFProcessor;
import org.broadinstitute.dsm.files.RequestPDFProcessor;
import org.broadinstitute.dsm.model.Value;
import org.broadinstitute.dsm.model.ddp.DDPParticipant;
import org.broadinstitute.dsm.model.ddp.PDF;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.*;
import org.broadinstitute.dsm.util.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class DownloadPDFRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DownloadPDFRoute.class);

    public static final String PDF = "/pdf";
    public static final String BUNDLE = "/bundle";

    private static final String COVER = "cover";
    private static final String CONSENT = "consent";
    private static final String RELEASE = "release";
    private static final String TISSUE = "tissue";
    private static final String IRB = "irb";
    private static final String JSON_START_DATE = "startDate";
    private static final String JSON_END_DATE = "endDate";

    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        logger.info(request.url());
        QueryParamsMap queryParams = request.queryMap();
        String realm = null;
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        if (StringUtils.isNotBlank(realm)) {
            if (UserUtil.checkUserAccess(realm, userId, "pdf_download")) {
                if (request.url().contains(RoutePath.DOWNLOAD_PDF)) {
                    String requestBody = request.body();
                    if (StringUtils.isNotBlank(requestBody)) {
                        JSONObject jsonObject = new JSONObject(requestBody);
                        String userIdR = (String) jsonObject.get(RequestParameter.USER_ID);
                        Integer userIdRequest = Integer.parseInt(userIdR);
                        if (!userId.equals(userIdR)) {
                            throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdR);
                        }

                        String ddpParticipantId = (String) jsonObject.get(RequestParameter.DDP_PARTICIPANT_ID);
                        if (StringUtils.isNotBlank(ddpParticipantId)) {
                            String configName = null;
                            if (jsonObject.has(RequestParameter.CONFIG_NAME)) {
                                configName = (String) jsonObject.get(RequestParameter.CONFIG_NAME);
                            }
                            String medicalRecord = null;
                            if (jsonObject.has("medicalRecordId")) {
                                medicalRecord = (String) jsonObject.get("medicalRecordId");
                            }
                            List<String> oncHistoryIDs = null;
                            if (jsonObject.has("requestId")) {
                                oncHistoryIDs = Arrays.asList(new Gson().fromJson((String) jsonObject.get("requestId"), String[].class));
                            }
                            List<PDF> pdfs = null;
                            if (jsonObject.has("pdfs")) {
                                pdfs = Arrays.asList(new Gson().fromJson((String) jsonObject.get("pdfs"), PDF[].class));
                            }
                            getPDFs(response, ddpParticipantId, configName, medicalRecord, oncHistoryIDs, pdfs, realm, requestBody, userIdRequest);
                            return new Result(200);
                        }
                        else {
                            response.status(500);
                            throw new RuntimeException("Error missing ddpParticipantId");
                        }
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

    public byte[] getPDFBundle(@NonNull String ddpParticipantId, String medicalRecordId,
                             List<String> oncHistoryIDs, List<PDF> pdfs, @NonNull String realm, String requestBody) {
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);
        if (ddpInstance != null && StringUtils.isNotBlank(ddpParticipantId)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                PDFMergerUtility pdfMerger = new PDFMergerUtility();
                pdfMerger.setDestinationStream(output);
                pdfs.sort(Comparator.comparing(org.broadinstitute.dsm.model.ddp.PDF::getOrder));

                //make cover pdf first
                if (pdfs != null && pdfs.size() > 0) {
                    pdfs.forEach( pdf -> {
                        if (pdf.getOrder() > 0) {
                            if (pdf.getConfigName().equals("cover")) {
                                pdfMerger.addSource(new ByteArrayInputStream(mrRequestPDF(ddpInstance, ddpParticipantId, medicalRecordId, requestBody)));
                            }
                            else if (pdf.getConfigName().equals("request")) {
                                pdfMerger.addSource(new ByteArrayInputStream(tissueRequestPDF(ddpInstance, ddpParticipantId, oncHistoryIDs)));
                            }
                            else if (pdf.getConfigName().equals("irb")) {
                                pdfMerger.addSource(new ByteArrayInputStream(getIRBLetter(ddpInstance)));
                            }
                            else {
                                pdfMerger.addSource(new ByteArrayInputStream(requestPDF(ddpInstance, ddpParticipantId, pdf.getConfigName())));
                            }
                        }
                    });
                }
                pdfMerger.mergeDocuments();
                //todo get page count and add them to the cover/request pdf
            }
            catch (IOException e) {
                throw new FileProcessingException("Unable to merge documents ", e);
            }
            return output.toByteArray();
        }
        return null;
    }

    private byte[] mrRequestPDF(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId, @NonNull String medicalRecordId,
                                    @NonNull String requestBody) {
        JSONObject jsonObject = new JSONObject(requestBody);
        Set keySet = jsonObject.keySet();
        String startDate = null;
        String endDate = null;
        if (keySet.contains(JSON_START_DATE)) {
            startDate = (String) jsonObject.get(JSON_START_DATE);
            if (!"0/0".equals(startDate) && !startDate.contains("/") && startDate.contains("-")) {
                startDate = SystemUtil.changeDateFormat(SystemUtil.DATE_FORMAT, SystemUtil.US_DATE_FORMAT, startDate);
            }
            else if (StringUtils.isNotBlank(startDate) && startDate.startsWith("0/") && !startDate.equals("0/0")) {
                startDate = "01/" + startDate.split("/")[1];
            }
        }
        if (keySet.contains(JSON_END_DATE)) {
            endDate = (String) jsonObject.get(JSON_END_DATE);
            endDate = SystemUtil.changeDateFormat(SystemUtil.DATE_FORMAT, SystemUtil.US_DATE_FORMAT, endDate);
        }
        if (StringUtils.isBlank(medicalRecordId)) {
            throw new RuntimeException("MedicalRecordID is missing. Can't create cover pdf");
        }
        //get information from db
        MedicalRecord medicalRecord = MedicalRecord.getMedicalRecord(ddpInstance.getName(), ddpParticipantId, medicalRecordId);

        PDFProcessor processor = new CoverPDFProcessor(ddpInstance.getName());
        Map<String, Object> valueMap = new HashMap<>();
        //values same no matter from where participant/institution data comes from
        valueMap.put(CoverPDFProcessor.FIELD_CONFIRMED_INSTITUTION_NAME, medicalRecord.getName());
        valueMap.put(CoverPDFProcessor.FIELD_CONFIRMED_INSTITUTION_NAME_2, medicalRecord.getName() + ",");
        valueMap.put(CoverPDFProcessor.FIELD_CONFIRMED_FAX, medicalRecord.getFax());
        String today = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
        valueMap.put(CoverPDFProcessor.FIELD_DATE, today);
        valueMap.put(CoverPDFProcessor.FIELD_DATE_2, StringUtils.isNotBlank(endDate) ? endDate : today); //end date

        //adding checkboxes configured under instance_settings
        InstanceSettings instanceSettings = InstanceSettings.getInstanceSettings(ddpInstance.getName());
        if (instanceSettings != null && instanceSettings.getMrCoverPdf() != null && !instanceSettings.getMrCoverPdf().isEmpty()) {
            for (Value mrCoverSetting : instanceSettings.getMrCoverPdf()) {
                if (keySet.contains(mrCoverSetting.getValue())) {
                    valueMap.put(mrCoverSetting.getValue(), BooleanUtils.toBoolean((Boolean) jsonObject.get(mrCoverSetting.getValue())));
                }
            }
        }
        addDDPParticipantDataToValueMap(ddpInstance, ddpParticipantId, valueMap, true);

        valueMap.put(CoverPDFProcessor.START_DATE_2, StringUtils.isNotBlank(startDate) ? startDate : valueMap.get(CoverPDFProcessor.FIELD_DATE_OF_DIAGNOSIS)); //start date
        InputStream stream = null;
        try {
            stream = processor.generateStream(valueMap);
            stream.mark(0);
            return IOUtils.toByteArray(stream);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't get pdf for participant " + ddpParticipantId + " of ddpInstance " + ddpInstance.getName(), e);
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Error closing stream", e);
            }
        }
    }

    private byte[] tissueRequestPDF(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId, @NonNull List<String> oncHistoryIDs) {
        logger.info("Generating request pdf for onc history ids {}", StringUtils.join(oncHistoryIDs, ","));
        RequestPDFProcessor processor = new RequestPDFProcessor(ddpInstance.getName());
        Map<String, Object> valueMap = new HashMap<>();
        String today = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
        valueMap.put(RequestPDFProcessor.FIELD_DATE, today);
        valueMap.put(RequestPDFProcessor.FIELD_DATE_2, "(" + today + ")");
        addDDPParticipantDataToValueMap(ddpInstance, ddpParticipantId, valueMap, false);

        InputStream stream = null;
        try {
            int counter = 0;
            if (oncHistoryIDs != null) {
                for (int i = 0; i < oncHistoryIDs.size(); i++) {
                    OncHistoryDetail oncHistoryDetail = OncHistoryDetail.getOncHistoryDetail(oncHistoryIDs.get(i), ddpInstance.getName());
                    // facility information is the same in all of the requests so only need to be set ones!
                    if (i == 0) {
                        valueMap.put(RequestPDFProcessor.FIELD_CONFIRMED_INSTITUTION_NAME, oncHistoryDetail.getFacility());
                        valueMap.put(RequestPDFProcessor.FIELD_CONFIRMED_PHONE, oncHistoryDetail.getFPhone());
                        valueMap.put(RequestPDFProcessor.FIELD_CONFIRMED_FAX, oncHistoryDetail.getFFax());
                    }
                    valueMap.put(RequestPDFProcessor.FIELD_DATE_PX + i, oncHistoryDetail.getDatePX());
                    valueMap.put(RequestPDFProcessor.FIELD_TYPE_LOCATION + i, oncHistoryDetail.getTypePX());
                    valueMap.put(RequestPDFProcessor.FIELD_ACCESSION_NUMBER + i, oncHistoryDetail.getAccessionNumber());
                    counter = i;
                }
            }
            valueMap.put(RequestPDFProcessor.BLOCK_COUNTER, counter + 1);

            stream = processor.generateStream(valueMap);
            stream.mark(0);
            return IOUtils.toByteArray(stream);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't get pdf for participant " + ddpParticipantId + " of ddpInstance " + ddpInstance.getName(), e);
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Error closing stream", e);
            }
        }
    }

    private byte[] getIRBLetter(@NonNull DDPInstance ddpInstance) {
        String groupId = DDPInstance.getDDPGroupId(ddpInstance.getName());
        return PDFProcessor.getTemplateFromGoogleBucket(groupId + "_IRB_Letter.pdf");
    }

    /**
     * get pdf
     * for the given participant and pdfType
     *
     * @throws Exception
     */
    public void getPDFs(@NonNull Response response, @NonNull String ddpParticipantId, String pdfType, String medicalRecordId,
                        List<String> oncHistoryIDs, List<PDF> pdfs, String realm, String requestBody, Integer userId) {
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);
        if (ddpInstance != null && StringUtils.isNotBlank(ddpParticipantId)) {
            byte[] pdfBytes = null;
            String fileName = "";
            if (COVER.equals(pdfType)) {
                pdfBytes = mrRequestPDF(ddpInstance, ddpParticipantId, medicalRecordId, requestBody);
            }
            else if (TISSUE.equals(pdfType)) {
                pdfBytes = tissueRequestPDF(ddpInstance, ddpParticipantId, oncHistoryIDs);
            }
            else if (IRB.equals(pdfType)) {
                pdfBytes = getIRBLetter(ddpInstance);
            }
            else if (pdfType != null){
                pdfBytes = requestPDF(ddpInstance, ddpParticipantId, pdfType);
            }
            else if (pdfType == null) {
                pdfBytes = getPDFBundle(ddpParticipantId, medicalRecordId, oncHistoryIDs, pdfs, realm, requestBody);
            }
            if (pdfBytes != null) {
                try {
                    savePDFinBucket(ddpInstance.getName(), ddpParticipantId, new ByteArrayInputStream(pdfBytes), fileName, userId);

                    HttpServletResponse rawResponse = response.raw();
                    rawResponse.getOutputStream().write(pdfBytes);
                    rawResponse.setStatus(200);
                    rawResponse.getOutputStream().flush();
                    rawResponse.getOutputStream().close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Couldn't make pdf for participant " + ddpParticipantId + " of ddpInstance " + ddpInstance.getName(), e);
                }
            }
            else {
                throw new RuntimeException("byte[] was null");
            }
        }
        else {
            throw new RuntimeException("DDPInstance of participant " + ddpParticipantId + " not found");
        }
    }

    private byte[] requestPDF(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId, @NonNull String pdfType) {
        String dsmRequest = ddpInstance.getBaseUrl() + RoutePath.DDP_PARTICIPANTS_PATH + "/" + ddpParticipantId + "/pdfs/" + pdfType;
        logger.info("Requesting pdf for participant  " + dsmRequest);
        try {
            return DDPRequestUtil.getPDFByteArray(dsmRequest, ddpInstance.getName(), ddpInstance.isHasAuth0Token());
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't get pdf for participant " + ddpParticipantId + " of ddpInstance " + ddpInstance.getName(), e);
        }
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

    private void addDDPParticipantDataToValueMap(@NonNull DDPInstance ddpInstance, @NonNull String ddpParticipantId,
                                                 @NonNull Map<String, Object> valueMap, boolean addDateOfDiagnosis) {
        DDPParticipant ddpParticipant = null;
        MedicalInfo medicalInfo = null;
        String dob = null;
        if (StringUtils.isNotBlank(ddpInstance.getParticipantIndexES())) {
            Map<String, Map<String, Object>> participantsESData = ElasticSearchUtil.getDDPParticipantsFromES(ddpInstance.getName(), ddpInstance.getParticipantIndexES());
            ddpParticipant = ElasticSearchUtil.getParticipantAsDDPParticipant(participantsESData, ddpParticipantId);
            medicalInfo = ElasticSearchUtil.getParticipantAsMedicalInfo(participantsESData, ddpParticipantId);
            dob = SystemUtil.changeDateFormat(SystemUtil.DATE_FORMAT, SystemUtil.US_DATE_FORMAT, medicalInfo.getDob());
        }

        //fill fields of pdf (needs to match the fields in template!)
        valueMap.put(CoverPDFProcessor.FIELD_FULL_NAME, ddpParticipant.getFirstName() + " " + ddpParticipant.getLastName());
        valueMap.put(CoverPDFProcessor.FIELD_DATE_OF_BIRTH, dob);
        if (addDateOfDiagnosis) {
            String dod = medicalInfo.getDateOfDiagnosis();
            if (StringUtils.isNotBlank(dod) && dod.startsWith("0/") && !dod.equals("0/0")) {
                dod = "01/" + dod.split("/")[1];
                valueMap.put(CoverPDFProcessor.FIELD_DATE_OF_DIAGNOSIS, dod);
            }
            else {
                valueMap.put(CoverPDFProcessor.FIELD_DATE_OF_DIAGNOSIS, medicalInfo.getDateOfDiagnosis());
            }
        }
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

    private void savePDFinBucket(@NonNull String realm, @NonNull String ddpParticipantId, @NonNull InputStream stream, @NonNull String fileType, @NonNull Integer userId) {
        String gcpName = TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GOOGLE_PROJECT_NAME);
        if (StringUtils.isNotBlank(gcpName)) {
            String bucketName = gcpName + "_dsm_" + realm.toLowerCase();
            try {
                String credentials = null;
                if (TransactionWrapper.hasConfigPath(ApplicationConfigConstants.GOOGLE_CREDENTIALS)) {
                    String tmp = TransactionWrapper.getSqlFromConfig(ApplicationConfigConstants.GOOGLE_CREDENTIALS);
                    if (StringUtils.isNotBlank(tmp) && new File(tmp).exists()) {
                        credentials = tmp;
                    }
                }
                if (GoogleBucket.bucketExists(credentials, gcpName, bucketName)) {
                    long time = System.currentTimeMillis();
                    GoogleBucket.uploadFile(credentials, gcpName, bucketName,
                            ddpParticipantId + "/readonly/" + ddpParticipantId + "_" + fileType + "_" + userId + "_download_" + time + ".pdf", stream);
                }
            }
            catch (Exception e) {
                logger.error("Failed to check for GCP bucket " + bucketName, e);
            }
        }
    }
}
