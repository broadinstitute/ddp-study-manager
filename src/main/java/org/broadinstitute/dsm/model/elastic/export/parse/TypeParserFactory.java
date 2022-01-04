package org.broadinstitute.dsm.model.elastic.export.parse;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.model.elastic.export.ExportFacadePayload;
import org.broadinstitute.dsm.model.elastic.migration.DynamicFieldsTypeParser;
import org.broadinstitute.dsm.statics.ESObjectConstants;

import java.util.Optional;

public class TypeParserFactory {

    public static TypeParser of(ExportFacadePayload exportFacadePayload) {
        TypeParser typeParser = new TypeParser();
        if (isDynamicFields(exportFacadePayload.getCamelCaseFieldName())) {
            DynamicFieldsTypeParser dynamicFieldsTypeParser = new DynamicFieldsTypeParser();
            dynamicFieldsTypeParser.setRealm(exportFacadePayload.getRealm());
            typeParser = dynamicFieldsTypeParser;
        }
        return typeParser;
    }

    private static boolean isDynamicFields(String fieldName) {
        return ESObjectConstants.ADDITIONAL_VALUES_JSON.equals(fieldName) || ESObjectConstants.DATA.equals(fieldName);
    }
}
