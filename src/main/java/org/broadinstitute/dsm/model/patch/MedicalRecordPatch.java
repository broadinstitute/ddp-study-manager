package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.Patch;
import org.broadinstitute.dsm.util.MedicalRecordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MedicalRecordPatch extends BasePatch {

    private Number mrID;
    private String oncHistoryDetailId;
    Map<String, Object> resultMap;

    static final Logger logger = LoggerFactory.getLogger(MedicalRecordPatch.class);


    public MedicalRecordPatch(Patch patch) {
        super(patch);
    }

    {

    }
    
    static {
        NULL_KEY = new HashMap<>();
        NULL_KEY.put(NAME_VALUE, null);
    }

    private void checkBeforePatch() {
        mrID = MedicalRecordUtil.isInstitutionTypeInDB(patch.getParentId());
        if (mrID == null) {
            // mr of that type doesn't exist yet, so create an institution and mr
            MedicalRecordUtil.writeInstitutionIntoDb(patch.getParentId(), MedicalRecordUtil.NOT_SPECIFIED);
            mrID = MedicalRecordUtil.isInstitutionTypeInDB(patch.getParentId());
        }
        if (mrID != null) {
            oncHistoryDetailId = OncHistoryDetail.createNewOncHistoryDetail(mrID.toString(), patch.getUser());
        }
    }

    @Override
    protected Object patchNameValuePairs() {
        checkBeforePatch();
        if (mrID == null) {
            logger.error("No medical record id for oncHistoryDetails ");
            return NULL_KEY;
        }
        processMultipleNameValues();
        Map<String, Object>

        return null;
    }

    @Override
    protected Object patchNameValuePair() {
        return null;
    }

    @Override
    Object handleSingleNameValue(DBElement dbElement) {
        return null;
    }

    @Override
    Optional<Object> processEachNameValue(NameValue nameValue, DBElement dbElement) {
        if (!Patch.patch(oncHistoryDetailId, patch.getUser(), nameValue, dbElement)) {
            throw new RuntimeException("An error occurred while attempting to patch ");
        }
        return Optional.empty();
    }
}
