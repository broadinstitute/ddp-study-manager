package org.broadinstitute.dsm.db.dao.ddp.tissue;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.statics.DBConstants;

import java.lang.annotation.Annotation;
import java.util.Map;

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

    public void createFilterForType(Map<String, String> queryConditions, String type, String tmp) {
        if (StringUtils.isBlank(type)) {
            return;
        }
        Annotation annotation = TissueSmIdType.class.getAnnotation(TableName.class);
        TableName tableNameAnnotation = (TableName) annotation;
        String tableName = tableNameAnnotation.name();
        String tableAlias = tableNameAnnotation.alias();
        String primaryKey = tableNameAnnotation.primaryKey();
        DBElement dbElementForType = new DBElement(tableName, tableAlias, primaryKey, DBConstants.SM_ID_TYPE);
        Filter filterForType = new Filter();
        filterForType.setType(Filter.TEXT);
        filterForType.setExactMatch(true);
        filterForType.setFilter1(new NameValue("smIdType", type));
        queryConditions.put(tmp, queryConditions.get(tmp).concat(Filter.getQueryStringForFiltering(filterForType, dbElementForType)));
    }
}
