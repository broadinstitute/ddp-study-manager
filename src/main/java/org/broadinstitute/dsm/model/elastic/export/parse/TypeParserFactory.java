package org.broadinstitute.dsm.model.elastic.export.parse;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.model.elastic.migration.DynamicFieldsTypeParser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.Optional;

public class TypeParserFactory {

    public static TypeParser of(String fieldName, String realm) {
        TypeParser typeParser = new TypeParser();
        if (ESObjectConstants.ADDITIONAL_VALUES_JSON.equals(fieldName) || ESObjectConstants.DATA.equals(fieldName)) {
            Optional<String> displayTypeByInstanceNameAndColumnName = FieldSettingsDao.of().getDisplayTypeByInstanceNameAndColumnName(realm, fieldName);
            if (displayTypeByInstanceNameAndColumnName.isPresent())
                typeParser = new DynamicFieldsTypeParser();
        }
        return typeParser;
    }
}
