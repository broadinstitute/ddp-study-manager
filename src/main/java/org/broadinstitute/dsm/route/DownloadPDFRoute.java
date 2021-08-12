package org.broadinstitute.dsm.route;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.PDF.DownloadPDF;
import org.broadinstitute.dsm.model.PDF.MiscPDFDownload;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;


public class DownloadPDFRoute extends BasicRoute {

    private static final Logger logger = LoggerFactory.getLogger(DownloadPDFRoute.class);

    public static final String PDF = "/pdf";
    public static final String BUNDLE = "/bundle";

    private final String PDF_ROLE = "pdf_download";

    public DownloadPDFRoute(String accessRole) {
        super(accessRole);
    }


    @Override
    public Object processRequest(Request request, Response response, String userId) throws Exception {
        if (Integer.parseInt((String) super.processRequest(request, response, userId)) == 404) {
            response.status(500);
            return UserErrorMessages.NO_RIGHTS;
        }
        String requestBody = request.body();
        String realm = null;
        QueryParamsMap queryParams = request.queryMap();
        if (queryParams.value(RoutePath.REALM) != null) {
            realm = queryParams.get(RoutePath.REALM).value();
        }
        if (request.url().contains(RoutePath.DOWNLOAD_PDF)) {
            if (StringUtils.isBlank(requestBody)) {
                response.status(500);
                throw new RuntimeException("Error missing requestBody");
            }
            Long userIdRequest = Long.parseLong((String) new JSONObject(requestBody).get(RequestParameter.USER_ID));
            DownloadPDF downloadPDFRequest = new DownloadPDF(requestBody);
            Optional<byte[]> pdfBytes = downloadPDFRequest.getPDFs(userIdRequest, realm, requestBody);
            pdfBytes.ifPresent(pdfBytesArray -> {
                try {
                    HttpServletResponse rawResponse = response.raw();
                    rawResponse.getOutputStream().write(pdfBytesArray);
                    rawResponse.setStatus(200);
                    rawResponse.getOutputStream().flush();
                    rawResponse.getOutputStream().close();
                }
                catch (IOException e) {
                    throw new RuntimeException("Couldn't make pdf of ddpInstance " + queryParams.get(RoutePath.REALM).value(), e);
                }
            });
            return null;
        }
        else {
            String ddpParticipantId = null;
            if (queryParams.value(RequestParameter.DDP_PARTICIPANT_ID) != null) {
                ddpParticipantId = queryParams.get(RequestParameter.DDP_PARTICIPANT_ID).value();
            }
            return new MiscPDFDownload(ddpParticipantId).create(realm);
        }

    }

}
