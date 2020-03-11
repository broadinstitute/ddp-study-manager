import {Injectable} from "@angular/core";

@Injectable()
export class Statics {

  public static HOME: string = "home";
  public static PERMALINK: string = "permalink";
  public static URL: string = "/";

  public static MEDICALRECORD: string = "medicalRecord";
  public static TISSUELIST: string = "tissueList";

  public static SHIPPING: string = "shipping";
  public static SHIPPING_QUEUE: string = "shippingQueue";
  public static SHIPPING_ERROR: string = "shippingError";
  public static SHIPPING_SENT: string = "shippingSent";
  public static SHIPPING_RECEIVED: string = "shippingReceived";
  public static SHIPPING_OVERVIEW: string = "shippingOverview";
  public static SHIPPING_DEACTIVATED: string = "shippingDeactivated";
  public static SHIPPING_UPLOADED: string = "shippingUploaded";
  public static SHIPPING_TRIGGERED: string = "shippingTriggered";

  public static UNSENT_OVERVIEW: string = "unsentOverview";
  public static SHIPPING_DASHBOARD: string = "shippingDashboard";
  public static MEDICALRECORD_DASHBOARD: string = "medicalRecordDashboard";

  public static EMAIL_EVENT: string = "emailEvent";
  public static EMAIL_EVENT_FOLLOW_UP: string = "emailEventFollowUp";

  public static SURVEY_CREATION: string = "surveyCreation";
  public static PARTICIPANT_EVENT: string = "participantEvent";
  public static PARTICIPANT_EXIT: string = "participantExit";
  public static MAILING_LIST: string = "mailingList";
  public static DISCARD_SAMPLES: string = "discardSamples";
  public static PDF_DOWNLOAD_MENU: string = "pdfDownload";

  public static YES: string = "yes";
  public static UNSENT: string = "unsent";
  public static CSV_FILE_EXTENSION: string = ".csv";
  public static PDF_FILE_EXTENSION: string = ".pdf";
  public static TXT_FILE_EXTENSION: string = ".txt";
  public static DISPLAY_NONE: string = "none";
  public static DISPLAY_BLOCK: string = "block";
  public static COLOR_PRIMARY: string = "primary";
  public static COLOR_WARN: string = "warn";
  public static COLOR_BASIC: string = "basic";
  public static COLOR_ACCENT: string = "accent";
  public static COLOR_RESLOVED: string = "resolved";

  public static HOME_URL: string = Statics.URL + Statics.HOME;
  public static MEDICALRECORD_URL: string = Statics.URL + Statics.MEDICALRECORD;
  public static TISSUEVIEW_URL: string = Statics.URL + Statics.TISSUELIST;
  public static PERMALINK_URL: string = Statics.URL + Statics.PERMALINK;
  public static SHIPPING_URL: string = Statics.URL + Statics.SHIPPING;
  public static UNSENT_OVERVIEW_URL: string = Statics.URL + Statics.UNSENT_OVERVIEW;
  public static SHIPPING_DASHBOARD_URL: string = Statics.URL + Statics.SHIPPING_DASHBOARD;
  public static MEDICALRECORD_DASHBOARD_URL: string = Statics.URL + Statics.MEDICALRECORD_DASHBOARD;
  public static SCAN_URL: string = Statics.URL + "scan";
  public static TISSUE_URL: string = Statics.URL + "tissue";
  public static PARTICIPANT_PAGE_URL: string = Statics.URL + "participantPage";

  public static TISSUE_ALIAS = "t";
  public static ONCDETAIL_ALIAS = "oD";
  public static ES_ALIAS = "data";
  public static PT_ALIAS = "p";
  public static MR_ALIAS = "m";
  public static DRUG_ALIAS = "d";
  public static DELIMITER_ALIAS = ".";

  public static EXITED = "EXITED";
  public static CONSENT_SUSPENDED = "CONSENT_SUSPENDED";

}
