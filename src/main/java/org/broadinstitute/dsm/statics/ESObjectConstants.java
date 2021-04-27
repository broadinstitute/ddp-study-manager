package org.broadinstitute.dsm.statics;

import java.util.Arrays;
import java.util.List;

public class ESObjectConstants {

    public static final String PARTICIPANT_DATA_ID = "participantDataId";

    //workflows
    public static final String ELASTIC_EXPORT_WORKFLOWS = "ELASTIC_EXPORT.workflows";

    //medical records
    public static final String MEDICAL_RECORDS = "medicalRecords";
    public static final String MEDICAL_RECORDS_ID = "medicalRecordId";
    public static final List<String> MEDICAL_RECORDS_FIELD_NAMES = Arrays.asList("name", "type", "requested", "received");

    //tissue records
    public static final String TISSUE_RECORDS = "tissueRecords";
    public static final String TISSUE_RECORDS_ID = "tissueRecordsId";
    public static final List<String> TISSUE_RECORDS_FIELD_NAMES =
            Arrays.asList("typePX", "locationPX", "datePX", "histology", "accessionNumber", "requested", "received", "sent");

    //samples
    public static final String SAMPLES = "samples";
    public static final String SENT = "sent";
    public static final String KIT_REQUEST_ID = "kitRequestId";
}
