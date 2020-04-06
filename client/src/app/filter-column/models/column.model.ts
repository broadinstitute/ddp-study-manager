export class ParticipantColumn {

  //participant columns
  public static REALM = new ParticipantColumn("DDP", "ddp", "data");
  public static SHORT_ID = new ParticipantColumn("Short ID", "hruid", "data", "profile");
  public static LEGACY_SHORT_ID = new ParticipantColumn("Legacy Short ID", "legacyShortId", "data", "profile");
  public static PARTICIPANT_ID = new ParticipantColumn("Participant ID", "guid", "data", "profile");
  public static LEGACY_PARTICIPANT_ID = new ParticipantColumn("Legacy Participant ID", "legacyAltPid", "data", "profile");
  public static FIRST_NAME = new ParticipantColumn("First Name", "firstName", "data", "profile");
  public static LAST_NAME = new ParticipantColumn("Last Name", "lastName", "data", "profile");
  public static COUNTRY = new ParticipantColumn("Country", "country", "data", "address");
  public static EMAIL = new ParticipantColumn("Email", "email", "data", "profile");
  public static REGISTRATION_DATE = new ParticipantColumn( "Registration Date", "createdAt", "data", "profile" );
  public static ENROLLMENT_STATUS = new ParticipantColumn("Status", "status", "data");
  public static DO_NOT_CONTACT = new ParticipantColumn("Do Not Contact", "doNotContact", "data", "profile");
  public static DATE_OF_MAJORITY = new ParticipantColumn( "Date of Majority", "dateOfMajority", "data", "dsm" );
  public static HAS_CONSENTED_TO_BLOOD = new ParticipantColumn( "Consent Blood", "hasConsentedToBloodDraw", "data", "dsm" );
  public static HAS_CONSENTED_TO_TISSUE = new ParticipantColumn( "Consent Tissue", "hasConsentedToTissueSample", "data", "dsm" );
  public static DATE_OF_BIRTH = new ParticipantColumn( "Date of Birth", "dateOfBirth", "data", "dsm" );
  public static DIAGNOSIS_MONTH = new ParticipantColumn( "Diagnosis Month", "diagnosisMonth", "data", "dsm" );
  public static DIAGNOSIS_YEAR = new ParticipantColumn( "Diagnosis Year", "diagnosisYear", "data", "dsm" );

  public static ONC_HISTORY_CREATED = new ParticipantColumn("Onc History Created", "createdOncHistory", "o");
  public static ONC_HISTORY_REVIEWED = new ParticipantColumn("Onc History Reviewed", "reviewedOncHistory", "o");
  public static PAPER_CR_SENT = new ParticipantColumn("Paper C/R Sent", "paperCRSent", "r");
  public static PAPER_CR_RECEIVED = new ParticipantColumn("Paper C/R Received", "paperCRReceived", "r");
  public static PARTICIPANT_NOTES = new ParticipantColumn("Participant Notes", "ptNotes", "r");
  public static MINIMAL_RECORDS = new ParticipantColumn("Incomplete/Minimal Medical Records", "minimalMR", "r");
  public static ABSTRACTION_READY = new ParticipantColumn("Ready for Abstraction", "abstractionReady", "r");
  public static ASSIGNEE_MR = new ParticipantColumn("MR Assignee", "assigneeMr", "p");
  public static ASSIGNEE_TISSUE = new ParticipantColumn("Tissue Assignee", "assigneeTissue", "p");
  public static EXIT_DATE = new ParticipantColumn("Date Withdrawn", "exitDate", "ex");

  //mr columns
  public static MR_TYPE = new ParticipantColumn("Type", "type", "inst");
  public static MR_INSTITUTION_NAME = new ParticipantColumn("Institution Name", "name", "m");
  public static MR_INSTITUTION_CONTACT = new ParticipantColumn("Institution Contact Person", "contact", "m");
  public static MR_INSTITUTION_PHONE = new ParticipantColumn("Institution Phone", "phone", "m");
  public static MR_INSTITUTION_FAX = new ParticipantColumn("Institution Fax", "fax", "m");
  public static MR_FAX_SENT = new ParticipantColumn("Initial MR Request", "faxSent", "m");
  public static MR_FAX_SENT_2 = new ParticipantColumn("Initial MR Request 2", "faxSent2", "m");
  public static MR_FAX_SENT_3 = new ParticipantColumn("Initial MR Request 3", "faxSent3", "m");
  public static MR_RECEIVED = new ParticipantColumn("Initial MR Received", "mrReceived", "m");
  public static MR_DOCUMENT = new ParticipantColumn("MR Document", "mrDocument", "m");
  public static MR_DOCUMENT_FILES = new ParticipantColumn("MR Document File Names", "mrDocumentFileNames", "m");
  public static MR_PROBLEM = new ParticipantColumn("MR Problem", "mrProblem", "m");
  public static MR_PROBLEM_TEXT = new ParticipantColumn("MR Problem Text", "mrProblemText", "m");
  public static MR_UNABLE_TO_OBTAIN = new ParticipantColumn("MR Unable to Obtain", "unableObtain", "m");
  public static MR_DUPLICATE = new ParticipantColumn("Duplicate", "duplicate", "m");
  public static MR_INTERNATIONAL = new ParticipantColumn("International", "international", "m");
  public static MR_PAPER_CR = new ParticipantColumn("Paper C/R required", "crRequired", "m");
  public static PATHOLOGY_RESENT = new ParticipantColumn("Pathology Present", "pathologyPresent", "m");
  public static MR_NOTES = new ParticipantColumn("MR Notes", "mrNotes", "m");
  public static MR_REVIEW = new ParticipantColumn("MR Review", "reviewMedicalRecord", "m");
  public static MR_FOLLOW_UP = new ParticipantColumn("Follow-Up ", "followUps", "m");
  public static MR_FOLLOW_REQUIRED = new ParticipantColumn("Follow-Up required", "followUpRequired", "m");
  public static MR_FOLLOW_REQUIRED_TEXT = new ParticipantColumn("Follow-Up required Text", "followUpRequiredText", "m");

  //oncHistory columns
  public static ACCESSION_NUMBER = new ParticipantColumn("Accession Number", "accessionNumber", "oD");
  public static DATE_PX = new ParticipantColumn("Date of PX", "datePX", "oD");
  public static TYPE_PX = new ParticipantColumn("Type of PX", "typePX", "oD");
  public static FACILITY = new ParticipantColumn("Facility", "facility", "oD");
  public static FACILITY_PHONE = new ParticipantColumn("Facility Phone", "fPhone", "oD");
  public static FACILITY_FAX = new ParticipantColumn("Facility Fax", "fFax", "oD");
  public static HISTOLOGY = new ParticipantColumn("Histology", "histology", "oD");
  public static LOCATION_PX = new ParticipantColumn("Location of PX", "locationPX", "oD");
  public static ONC_HISTORY_NOTES = new ParticipantColumn("OncHistory Notes", "oncHisNotes", "oD");
  public static ONC_HISTORY_REQUEST = new ParticipantColumn("Request Status", "request", "oD");
  public static TISSUE_FAX = new ParticipantColumn("Tissue Request Date", "tFaxSent", "oD");
  public static TISSUE_FAX_2 = new ParticipantColumn("Tissue Request Date 2", "tFaxSent2", "oD");
  public static TISSUE_FAX_3 = new ParticipantColumn("Tissue Request Date 3", "tFaxSent3", "oD");
  public static TISSUE_RECEIVED = new ParticipantColumn("Tissue Received", "tissueReceived", "oD");
  public static GENDER = new ParticipantColumn("Gender", "gender", "oD");
  public static DESTRUCTION_POLICY = new ParticipantColumn("Destruction Policy (years)", "destructionPolicy", "oD");
  public static TISSUE_PROBLEM_OPTION = new ParticipantColumn("Problem with Tissue", "tissueProblemOption", "oD");
  public static UNABLE_OBTAIN_TISSUE = new ParticipantColumn("Unable To Obtain", "unableToObtain", "oD");

  //tissue column
  public static COUNT_RECEIVED = new ParticipantColumn("Count Received", "countReceived", "t");
  public static TISSUE_NOTES = new ParticipantColumn("Tissue Notes", "tNotes", "t");
  public static TISSUE_TYPE = new ParticipantColumn("Tissue Type", "tissueType", "t");
  public static TISSUE_SITE = new ParticipantColumn("Tissue Site", "tissueSite", "t");
  public static TUMOR_TYPE = new ParticipantColumn("Tumor Type", "tumorType", "t");
  public static H_E = new ParticipantColumn("H&E", "hE", "t");
  public static PATHOLOGY_REPORT = new ParticipantColumn("Pathology Report", "pathologyReport", "t");
  public static COLLABORATOR_SAMPLE_ID = new ParticipantColumn("Collaborator Sample ID", "collaboratorSampleId", "t");
  public static BLOCK_SENT = new ParticipantColumn("Block to SHL", "blockSent", "t");
  public static SCROLL_RECEIVED = new ParticipantColumn("Scrolls back from SHL", "scrollsReceived", "t");
  public static SK_ID = new ParticipantColumn("SK ID", "skId", "t");
  public static SM_ID = new ParticipantColumn("SM ID for H&E", "smId", "t");
  public static SENT_GP = new ParticipantColumn("Date sent to GP", "sentGp", "t");
  public static TISSUE_RETURNED = new ParticipantColumn("Return Date", "tissueReturnDate", "t");
  public static TISSUE_EXPECTED_RETURN = new ParticipantColumn("Expected Return Date", "expectedReturn", "t");
  public static TISSUE_TRACKING_NUMBER = new ParticipantColumn("Tracking Number", "returnFedexId", "t");
  public static TISSUE_SHL_NUMBER = new ParticipantColumn("SHL Work Number", "shlWorkNumber", "t");
  public static TISSUE_FIRST_SM_ID = new ParticipantColumn("First SM ID", "firstSmId", "t");
  public static TISSUE_TUMOR_PERCENT = new ParticipantColumn("Tumor Percentage as reported by SHL", "tumorPercentage", "t");
  public static TISSUE_SEQUENCE = new ParticipantColumn("Sequencing Results", "sequenceResults", "t");
  public static SCROLLS_COUNT = new ParticipantColumn("Scroll(s)", "scrollsCount", "t");
  public static BLOCKS_COUNT = new ParticipantColumn("Block(s)", "blocksCount", "t");
  public static USS_COUNT = new ParticipantColumn("USS (unstained slides)", "ussCount", "t");
  public static H_E_COUNT = new ParticipantColumn("H&E(s)", "hECount", "t");


  //abstraction column
  public static ABSTRACTION = new ParticipantColumn("Abstraction", "abstraction");
  public static ABSTRACTION_STATUS = new ParticipantColumn("Status", "aStatus", "a");
  public static ABSTRACTION_ACTIVITY = new ParticipantColumn("Activity", "activity", "a");
  public static ABSTRACTION_USER = new ParticipantColumn("User", "user", "a");
  public static REVIEW = new ParticipantColumn("Review", "review");
  public static QC = new ParticipantColumn("QC", "qc");

  //kit
  public static COLLABORATOR_SAMPLE = new ParticipantColumn("Collaborator Sample ID", "bspCollaboratorSampleId", "k");
  public static SAMPLE_TYPE = new ParticipantColumn("Sample Type", "kitType", "k");
  public static SAMPLE_SENT = new ParticipantColumn("Sample Sent", "scanDate", "k");
  public static SAMPLE_RECEIVED = new ParticipantColumn("Sample Received", "receiveDate", "k");
  public static SAMPLE_DEACTIVATION = new ParticipantColumn("Sample Deactivation", "deactivatedDate", "k");
  public static SAMPLE_QUEUE = new ParticipantColumn("Status", "sampleQueue", "k");
  public static TRACKING_TO_PARTICIPANT = new ParticipantColumn("Tracking Number", "trackingNumberTo", "k");
  public static TRACKING_RETURN = new ParticipantColumn("Tracking Return", "scannedTrackingNumber", "k");
  public static MF_BARCODE = new ParticipantColumn("MF code", "kitLabel", "k");

  constructor (public display: string, public name: string, public tableAlias?: string, public object?: string, public esData?: boolean) {
    this.display = display;
    this.name = name;
    this.tableAlias = tableAlias;
    this.object = object;
    this.esData = esData;
  }

  public static parse (json): ParticipantColumn {
    if ( json === undefined ) {
      return null;
    }
    return new ParticipantColumn(json.display, json.name, json.tableAlias);
  }
}
