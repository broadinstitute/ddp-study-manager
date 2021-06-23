package org.broadinstitute.dsm.statics;

import java.util.Arrays;
import java.util.List;

public class ESObjectConstants {

    public static final String PARTICIPANT_DATA_ID = "participantDataId";

    //workflows
    public static final String ELASTIC_EXPORT_WORKFLOWS = "ELASTIC_EXPORT.workflows";
    public static final String WORKFLOWS = "workflows";
    public static final String WORKFLOW = "workflow";
    public static final String STATUS = "status";
    public static final String DATE = "date";
    public static final String DATA = "data";

    //medical records
    public static final String MEDICAL_RECORDS = "medicalRecords";
    public static final String MEDICAL_RECORDS_ID = "medicalRecordId";
    public static final List<String> MEDICAL_RECORDS_FIELD_NAMES = Arrays.asList("name", "type", "requested", "received");

    //tissue records
    public static final String TISSUE_RECORDS = "tissueRecords";
    public static final String TISSUE_RECORDS_ID = "tissueRecordId";
    public static final List<String> TISSUE_RECORDS_FIELD_NAMES =
            Arrays.asList("typePX", "locationPX", "datePX", "histology", "accessionNumber", "requested", "received", "sent");

    //samples
    public static final String SAMPLES = "samples";
    public static final String SENT = "sent";
    public static final String RECEIVED = "received";
    public static final String KIT_REQUEST_ID = "kitRequestId";

    //common
    public static final String DDP_PARTICIPANT_ID = "ddpParticipantId";

    //dsm
    public static final String DSM = "dsm";
    public static final String FAMILY_ID = "familyId"; //needed for RGP so far
    public static final String SUBJECT_ID = "subjectId";
    public static final String LASTNAME = "firstname";
    public static final String FIRSTNAME = "lastname";
}
