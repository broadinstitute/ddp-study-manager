package org.broadinstitute.dsm.model.elastic;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.broadinstitute.dsm.db.*;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
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
    List<Tissue> tissue; // todo: change to Tissue

    @SerializedName(ESObjectConstants.MEDICAL_RECORDS)
    List<MedicalRecord> medicalRecord;

    @SerializedName(ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS)
    List<OncHistoryDetail> oncHistoryDetail;

    @SerializedName(ESObjectConstants.PARTICIPANT_DATA)
    List<ParticipantDataDto> participantData;

    @SerializedName(ESObjectConstants.PARTICIPANT)
    Participant participant;

    List<KitRequestShipping> kitRequestShipping;

    OncHistory oncHistory;

}
