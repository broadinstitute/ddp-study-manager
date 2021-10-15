package org.broadinstitute.dsm.analytics;

public class GoogleAnalyticsMetrics {

//    public static final String EVENT_CATEGORY_USER_REGISTRATION = "user-registration";
//    public static final String EVENT_ACTION_USER_REGISTRATION = "register-user";
//    public static final String EVENT_LABEL_USER_REGISTRATION = "registration"; we may need it for FON

    public static final String EVENT_CATEGORY_USER_LOGIN = "user-login";
    public static final String EVENT_ACTION_USER_LOGIN = "user-logged-in";
    public static final String EVENT_LABEL_USER_LOGIN = "login"; //studyGuid appended

    public static final String EVENT_CATEGORY_PARTICIPANT_LIST = "participant-list";
    public static final String EVENT_ACTION_PARTICIPANT_LIST = "participant-list-loaded";
    public static final String EVENT_LABEL_PARTICIPANT_LIST = "participant-list-loaded";//studyGuid appended

    public static final String EVENT_CATEGORY_PATCH_DATA = "patch-data";
    public static final String EVENT_ACTION_PATCH_DATA = "patch-data-answers";
    public static final String EVENT_LABEL_PATCH_DATA = "patch-answers"; //studyGuid & original page appended

    public static final String EVENT_CATEGORY_TISSUE_LIST = "tissue-list";
    public static final String EVENT_ACTION_TISSUE_LIST = "tissue-list-loaded";
    public static final String EVENT_LABEL_TISSUE_LIST = "tissue-list-loaded";//studyGuid appended

}
