package org.broadinstitute.dsm.db.dao.ddp.tissue;

import lombok.Data;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.statics.DBConstants;

@Data
@TableName (
        name = DBConstants.SM_ID_TYPE_TABLE,
        alias = DBConstants.SM_ID_TYPE_TABLE_ALIAS,
        primaryKey = DBConstants.SM_ID_TYPE_ID,
        columnPrefix = "")
public class TissueSmIdType {

    @ColumnName (DBConstants.SM_ID_TYPE_ID)
    private String smIdTypeId;

    @ColumnName (DBConstants.SM_ID_TYPE)
    private String smIdType;

}
