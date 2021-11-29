package org.broadinstitute.dsm.db;

import lombok.Data;
import org.broadinstitute.dsm.db.dao.ddp.tissue.TissueSMIDDao;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@TableName (
        name = DBConstants.SM_ID_TABLE,
        alias = DBConstants.SM_ID_TABLE_ALIAS,
        primaryKey = DBConstants.SM_ID_PK,
        columnPrefix = "")
public class TissueSmId {

    @ColumnName (DBConstants.SM_ID_VALUE)
    private String smIdValue;

    @ColumnName (DBConstants.SM_ID_TYPE_ID)
    private String smIdType;

    @ColumnName (DBConstants.SM_ID_TISSUE_ID)
    private String tissueId;

    @ColumnName (DBConstants.SM_ID_PK)
    private String smIdPk;

    @ColumnName (DBConstants.DELETED)
    private boolean deleted;

    public static String HE = "he";
    public static String USS = "uss";
    public static String SCROLLS = "scrolls";
    private static final Logger logger = LoggerFactory.getLogger(TissueSmId.class);
    public TissueSmId() {
    }

    public TissueSmId(String smIdPk, String smIdType, String smIdValue, String tissueId) {
        this.smIdPk = smIdPk;
        this.smIdType = smIdType;
        this.smIdValue = smIdValue;
        this.tissueId = tissueId;
    }


    public static Map<String, List<TissueSmId>> getSMIdsForTissueId(ResultSet rs) {
        Map<String, List<TissueSmId>> map = new HashMap<>();
        TissueSmId tissueSmId  = null;
        try {
            tissueSmId = new TissueSmId(
                    rs.getString(DBConstants.SM_ID_PK),
                    rs.getString(DBConstants.SM_ID_TYPE_ID),
                    rs.getString(DBConstants.SM_ID_VALUE),
                    rs.getString(DBConstants.TISSUE_ID)
            );
        }
        catch (SQLException e) {
            logger.error("problem getting tissue sm ids", e);
        }

        List<TissueSmId> list = map.getOrDefault(tissueSmId.getSmIdType(), new ArrayList<>());
            list.add(tissueSmId);
            map.put(tissueSmId.getSmIdType(), list);

        return map;
    }

    public String createNewSmId(String tissueId, String userId, String smIdType) {
        String smIdId = new TissueSMIDDao().createNewSMIDForTissue(tissueId, userId, smIdType);
        return smIdId;
    }
}
