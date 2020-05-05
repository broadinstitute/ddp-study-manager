package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.security.RequestHandler;
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

    private static final String PERMISSION = "mr:view";

    private PatchUtil patchUtil;

    public DisplaySettingsRoute(@NonNull PatchUtil patchUtil, @NonNull Auth0Util auth0Util) {
        super(auth0Util, PERMISSION);
        this.patchUtil = patchUtil;
    }

    @Override
    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
        if (patchUtil.getColumnNameMap() == null) {
            throw new RuntimeException("ColumnNameMap is null!");
        }

        String realm = null;
        if (StringUtils.isNotBlank(getRealm())) {
            realm = getRealm();
        }
        else {
            throw new RuntimeException("No realm was sent!");
        }
        String ddpGroupId = DDPInstance.getDDPGroupId(realm);
        if (StringUtils.isBlank(ddpGroupId)) {
            logger.error("GroupId is empty");
        }

        QueryParamsMap queryParams = request.queryMap();
        String parent = queryParams.get("parent").value();
        if (StringUtils.isBlank(parent)) {
            logger.error("Parent is empty");
        }
        DDPInstance instance = DDPInstance.getDDPInstance(realm);
        if (instance == null) {
            logger.error("Instance was not found");
        }
        if (StringUtils.isNotBlank(realm) && instance != null && StringUtils.isNotBlank(userId)
                && StringUtils.isNotBlank(parent) && StringUtils.isNotBlank(ddpGroupId)) {
            Map<String, Object> displaySettings = new HashMap<>();
            displaySettings.put("assignees", Assignee.getAssignees(realm));
            displaySettings.put("fieldSettings", FieldSettings.getFieldSettings(realm));
            displaySettings.put("drugs", Drug.getDrugList());
            displaySettings.put("cancers", Cancer.getCancers());
            displaySettings.put("activityDefinitions", ElasticSearchUtil.getActivityDefinitions(instance));
            displaySettings.put("filters", ViewFilter.getAllFilters(userId, patchUtil.getColumnNameMap(), parent, ddpGroupId, instance.getDdpInstanceId()));
            displaySettings.put("abstractionFields", AbstractionUtil.getFormControls(realm));
            InstanceSettings instanceSettings = InstanceSettings.getInstanceSettings(realm);
            if (instanceSettings != null && instanceSettings.getMrCoverPdf() != null && !instanceSettings.getMrCoverPdf().isEmpty()) {
                displaySettings.put("mrCoverPDF", instanceSettings.getMrCoverPdf());
            }
            return displaySettings;
        }
        logger.warn(UserErrorMessages.CONTACT_DEVELOPER);
        return new Result(500, UserErrorMessages.CONTACT_DEVELOPER);
    }
}
