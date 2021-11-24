package org.broadinstitute.dsm.analytics;

public class GoogleAnalyticsMetrics {

//    public static final String EVENT_CATEGORY_USER_REGISTRATION = "user-registration";
//    public static final String EVENT_ACTION_USER_REGISTRATION = "register-user";
//    public static final String EVENT_LABEL_USER_REGISTRATION = "registration"; we may need it for FON

    public static final String EVENT_CATEGORY_USER_LOGIN = "user-login";
    public static final String EVENT_ACTION_USER_LOGIN = "user-logged-in";
    public static final String EVENT_LABEL_USER_LOGIN = "login"; //studyGuid appended

    public static final String  EVENT_CATEGORY_PARTICIPANT_LIST = "participant-list";
    public static final String EVENT_PARTICIPANT_LIST_LOAD_TIME = "participant-list-load-time";

    public static final String  EVENT_CATEGORY_DASHBOARD = "dashboard";
    public static final String EVENT_DASHBOARD_LOAD_TIME = "dashboard-load-time";
    public static final String DASHBOARD_LABEL_SHIPPING_REPORT = ":shipping-report";
    public static final String DASHBOARD_LABEL_SHIPPING_DASHBOARD = ":shipping-dashboard";
    public static final String DASHBOARD_LABEL_SHIPPING_REPORT_DOWNLOAD = ":shipping-report-download";
    public static final String DASHBOARD_LABEL_ALL_REALM = ":all-realm";
    public static final String DASHBOARD_LABEL_MR = ":medical-record";

    public static final String  EVENT_SERVER_START= "server-start";

    public static final String EVENT_CATEGORY_PATCH_DATA = "patch-data";
    public static final String EVENT_PATCH_DATA_ANSWERS = "patch-data-answers";

    public static final String EVENT_CATEGORY_TISSUE_LIST = "tissue-list";
    public static final String EVENT_TISSUE_LIST_LOADED = "tissue-list-loaded";

    public static int getTimeDifferenceToNow(Long start){
        return Math.toIntExact((System.currentTimeMillis() - start)/1000);
    }

}
