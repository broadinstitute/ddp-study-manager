package org.broadinstitute.dsm.model.elastic.export.generate;

import com.mysql.cj.xdevapi.Column;
import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.BaseParser;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import org.broadinstitute.dsm.model.elastic.migration.BaseCollectionMigrator;
import org.broadinstitute.dsm.model.elastic.migration.MedicalRecordMigrator;

import javax.lang.model.element.TypeParameterElement;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MedicalRecordMappingGenerator implements Generator {

    private Parser parser;
    private GeneratorPayload generatorPayload;

    public MedicalRecordMappingGenerator(Parser parser, GeneratorPayload generatorPayload) {
        this.parser = parser;
        this.generatorPayload = generatorPayload;
    }


    @Override
    public Map<String, Object> generate() {

        TypeParser typeParser = new TypeParser();
        BaseCollectionMigrator baseCollectionMigrator = new MedicalRecordMigrator("asdad", "Asdad");

        Field[] declaredFields = MedicalRecord.class.getDeclaredFields();

        Map<String, Object> map = new HashMap<>();

        for (Field declaredField : declaredFields) {

            ColumnName annotation = declaredField.getAnnotation(ColumnName.class);

            boolean fieldListType = baseCollectionMigrator.isFieldListType(declaredField);
            if (fieldListType) {
                try {
                    Class<?> parameterizedType = Util.getParameterizedType(declaredField.getGenericType());

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }   
            } else {
                String fieldName = annotation.value();
                String camelCaseFieldName = Util.underscoresToCamelCase(fieldName);
                Object type = typeParser.parse(String.valueOf(generatorPayload.getValue()));
                map.put(camelCaseFieldName, type);
            }

        }



        return null;
    }

}
