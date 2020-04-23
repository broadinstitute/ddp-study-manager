package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RequestParameter;
import org.broadinstitute.dsm.statics.UserErrorMessages;
import org.broadinstitute.dsm.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplaySettingsRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DisplaySettingsRoute.class);

    private PatchUtil patchUtil;
    private Auth0Util auth0Util;

    public DisplaySettingsRoute(@NonNull PatchUtil patchUtil, @NonNull Auth0Util auth0Util) {
        this.patchUtil = patchUtil;
        this.auth0Util = auth0Util;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
        if (patchUtil.getColumnNameMap() == null) {
            throw new RuntimeException("ColumnNameMap is null!");
        }
        QueryParamsMap queryParams = request.queryMap();
        String realm = request.params(RequestParameter.REALM);
        if (StringUtils.isBlank(realm)) {
            logger.error("Realm is empty");
        }
        String ddpGroupId = DDPInstance.getDDPGroupId(realm);
        if (StringUtils.isBlank(ddpGroupId)) {
            logger.error("GroupId is empty");
        }

        String userIdRequest = UserUtil.getUserId(request);//gets checked in UserUtil
        if (!userId.equals(userIdRequest)) {
            throw new RuntimeException("User id was not equal. User Id in token " + userId + " user Id in request " + userIdRequest);
        }
        List<String> permissions = auth0Util.getUserPermissions(userId, userMail, realm);
        if (permissions != null && !permissions. isEmpty() && permissions.contains("mr:view")) {
            String parent = queryParams.get("parent").value();
            if (StringUtils.isBlank(parent)) {
                logger.error("Parent is empty");
            }
            DDPInstance instance = DDPInstance.getDDPInstance(realm);
            if (instance == null) {
                logger.error("Instance was not found");
            }
            if (StringUtils.isNotBlank(realm) && instance != null && StringUtils.isNotBlank(userIdRequest)
                    && StringUtils.isNotBlank(parent) && StringUtils.isNotBlank(ddpGroupId)) {
                Map<String, Object> displaySettings = new HashMap<>();
                displaySettings.put("assignees", Assignee.getAssignees(realm));
                displaySettings.put("fieldSettings", FieldSettings.getFieldSettings(realm));
                displaySettings.put("drugs", Drug.getDrugList());
                displaySettings.put("cancers", Cancer.getCancers());
                displaySettings.put("activityDefinitions", ElasticSearchUtil.getActivityDefinitions(instance));
                displaySettings.put("filters", ViewFilter.getAllFilters(userIdRequest, patchUtil.getColumnNameMap(), parent, ddpGroupId, instance.getDdpInstanceId()));
                displaySettings.put("abstractionFields", AbstractionUtil.getFormControls(realm));
                InstanceSettings instanceSettings = InstanceSettings.getInstanceSettings(realm);
                if (instanceSettings != null && instanceSettings.getMrCoverPdf() != null && !instanceSettings.getMrCoverPdf().isEmpty()) {
                    displaySettings.put("mrCoverPDF", instanceSettings.getMrCoverPdf());
                }
                return displaySettings;
            }
        }
        else {
            logger.warn(UserErrorMessages.NO_RIGHTS);
            return new Result(500, UserErrorMessages.NO_RIGHTS);
        }
        logger.warn(UserErrorMessages.CONTACT_DEVELOPER);
        return new Result(500, UserErrorMessages.CONTACT_DEVELOPER);
    }
}
