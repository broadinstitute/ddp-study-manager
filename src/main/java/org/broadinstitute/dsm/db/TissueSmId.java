package org.broadinstitute.dsm.db;

import lombok.Data;
import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueSMIDDao;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.DBConstants;

import java.util.List;

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


    public static TissueSmId[] getSMIdsForTissueId(String tissueId, String type) {
        TissueSMIDDao tissueSMIDDao = new TissueSMIDDao();
        List<TissueSmId> list = tissueSMIDDao.getSMIdsForTissueId(tissueId, type);
        return list.toArray(new TissueSmId[list.size()]);
    }

    public String createNewSmId(String tissueId, String userId, String smIdType){
        String smIdId = new TissueSMIDDao().createNewSMIDForTissue(tissueId, userId, smIdType);
        return smIdId;
    }
}
