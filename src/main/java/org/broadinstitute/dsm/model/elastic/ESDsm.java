package org.broadinstitute.dsm.model.elastic;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.OncHistory;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.Participant;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.statics.ESObjectConstants;

@Getter
public class ESDsm {

    @SerializedName(ESObjectConstants.DATE_OF_MAJORITY)
    Object dateOfMajority;

    @SerializedName(ESObjectConstants.FAMILY_ID)
    String familyId;

    @SerializedName(ESObjectConstants.HAS_CONSENTED_TO_BLOODD_RAW)
    boolean hasConsentedToBloodDraw;

    @SerializedName(ESObjectConstants.PDFS)
    List pdfs;

    @SerializedName(ESObjectConstants.DATE_OF_BIRTH)
    String dateOfBirth;

    @SerializedName(ESObjectConstants.DIAGNOSIS_MONTH)
    Object diagnosisMonth;

    @SerializedName(ESObjectConstants.HAS_CONSENTED_TO_TISSUE_SAMPLE)
    boolean hasConsentedToTissueSample;

    @SerializedName(ESObjectConstants.DIAGNOSIS_YEAR)
    Object diagnosisYear;

    @SerializedName(ESObjectConstants.TISSUE_RECORDS)
    List<Map<String, Object>> tissueRecords; // todo: change to Tissue

    @SerializedName(ESObjectConstants.MEDICAL_RECORDS)
    List<MedicalRecord> medicalRecords;

    @SerializedName(ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS)
    List<OncHistoryDetail> oncHistoryDetails;

    @SerializedName(ESObjectConstants.PARTICIPANT_DATA)
    List<ParticipantData> participantData;

    @SerializedName(ESObjectConstants.PARTICIPANT)
    Participant participant;

    List<KitRequestShipping> kitRequestShipping;

    OncHistory oncHistory;
}
