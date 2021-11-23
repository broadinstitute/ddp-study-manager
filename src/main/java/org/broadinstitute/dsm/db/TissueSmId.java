package org.broadinstitute.dsm.db;

import lombok.Data;
import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueSMIDDao;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.DBConstants;

import java.util.List;
import java.util.Map;

@Data
@TableName (
        name = DBConstants.SM_ID_TABLE,
        alias = DBConstants.SM_ID_TABLE_ALIAS,
        primaryKey = DBConstants.SM_ID_PK,
        columnPrefix = "")
public class TissueSmId {

    @ColumnName(DBConstants.SM_ID_VALUE)
    private String smIdValue;

    @ColumnName(DBConstants.SM_ID_TYPE_ID)
    private String smIdType;

    @ColumnName(DBConstants.SM_ID_TISSUE_ID)
    private String tissueId;

    @ColumnName(DBConstants.SM_ID_PK)
    private String smIdPk;

    @ColumnName(DBConstants.DELETED)
    private boolean deleted;

    public static String HE = "he";
    public static String USS = "uss";
    public static String SCROLLS = "scrolls";
    public TissueSmId(){}

    public TissueSmId(String smIdPk, String smIdType, String smIdValue, String tissueId){
        this.smIdPk = smIdPk;
        this.smIdType = smIdType;
        this.smIdValue = smIdValue;
        this.tissueId = tissueId;
    }


    public static Map<String, List<TissueSmId>> getSMIdsForTissueId(String tissueId) {
        TissueSMIDDao tissueSMIDDao = new TissueSMIDDao();
        Map<String, List<TissueSmId>> map = tissueSMIDDao.getSMIdsForTissueId(tissueId);
        return map;
    }

    public String createNewSmId(String tissueId, String userId, String smIdType){
        String smIdId = new TissueSMIDDao().createNewSMIDForTissue(tissueId, userId, smIdType);
        return smIdId;
    }
}
