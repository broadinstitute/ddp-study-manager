package org.broadinstitute.dsm.db.dto.kit;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.KitRequestShipping;
import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.TissueSmId;
import org.broadinstitute.dsm.model.KitType;
import org.broadinstitute.dsm.model.elasticsearch.ESProfile;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

import java.util.HashMap;
import java.util.Optional;

@Data
public class ClinicalKitDto {

    @SerializedName ("participant_id")
    String collaboratorParticipantId;

    @SerializedName ("sample_id")
    String sampleId;

    @SerializedName ("sample_collection")
    String sampleCollection;

    @SerializedName ("material_type")
    String materialType;

    @SerializedName ("vessel_type")
    String vesselType;

    @SerializedName ("first_name")
    String firstName;

    @SerializedName ("last_name")
    String lastName;

    @SerializedName ("date_of_birth")
    String dateOfBirth;

    @SerializedName("sample_type")
    String sampleType;

    @SerializedName("gender")
    String gender;

    @SerializedName("accession_number")
    String accessionNumber;

    @SerializedName("kit_label")
    String mfBarcode;

    private final String FFPE_SECTION_KIT_TYPE = "FFPE-SECTION";
    private final String FFPE_SCROLLS_KIT_TYPE = "FFPE-SCROLL";
    private final String USS = "uss";
    private final String SCROLLS = "scrolls";

    public ClinicalKitDto(){}

    public void setSampleType(String kitType){
        switch (kitType.toLowerCase()){
            case "saliva":
                this.sampleType = "Normal";
                break;
            case "blood":
                this.sampleType = "N/A";
                break;
            default: //tissue
                this.sampleType = "Tumor";
                break;
        }
    }

    public void setGender(String genderString){
        switch (genderString.toLowerCase()){
            case "male":
                this.gender = "M";
                break;
            case "female":
                this.gender = "F";
                break;
            default: //intersex or prefer_not_answer
                this.gender = "U";
                break;
        }
    }

    public void setAllInfoBasedOnTissue(TissueSmId tissueSmId, String ddpParticipantId, DDPInstance ddpInstance) {
        OncHistoryDetail oncHistoryDetail = OncHistoryDetail.getOncHistoryDetailByTissueId(tissueSmId.getTissueId(), ddpInstance);
        Optional<ElasticSearchParticipantDto> maybeParticipantByParticipantId = ElasticSearchUtil.getParticipantESDataByParticipantId(ddpInstance.getParticipantIndexES(), ddpParticipantId);
        maybeParticipantByParticipantId.ifPresent(participant -> {
            String shortId = maybeParticipantByParticipantId.get().getProfile().map(ESProfile::getHruid).get();
            KitType kitType = getKitType(tissueSmId, ddpInstance);
            String collaboratorParticipantId = KitRequestShipping.getCollaboratorParticipantId(ddpInstance.getBaseUrl(), ddpInstance.getDdpInstanceId(), ddpInstance.isMigratedDDP(),
                    ddpInstance.getCollaboratorIdPrefix(), ddpParticipantId, shortId, null);
            this.setAccessionNumber(oncHistoryDetail.getAccessionNumber());
            this.setCollaboratorParticipantId(collaboratorParticipantId);
            this.setSampleType(kitType.getKitTypeName());
            this.setMaterialType(kitType.getBspMaterialType());
            this.setVesselType(kitType.getBspReceptableType());
            this.setSampleCollection(ddpInstance.getBspCollection());
            this.setSampleId(ddpInstance.getBspCollection());
        });
        maybeParticipantByParticipantId.orElseThrow(() -> {
            throw new RuntimeException("Participant Info in ES not found! Participant id : "+ddpParticipantId);});

    }


    private KitType getKitType(TissueSmId tissueSmId , DDPInstance ddpInstance) {
        KitType kitType = null;
        HashMap<String, KitType> map = KitType.getKitLookup();
        switch (tissueSmId.getSmIdType()){
            case USS:
                kitType = map.get(FFPE_SECTION_KIT_TYPE + "_" + ddpInstance.getDdpInstanceId());
                break;
            case SCROLLS:
                kitType = map.get(FFPE_SCROLLS_KIT_TYPE + "_" + ddpInstance.getDdpInstanceId());
                break;

        }
        return kitType;
    }
}
