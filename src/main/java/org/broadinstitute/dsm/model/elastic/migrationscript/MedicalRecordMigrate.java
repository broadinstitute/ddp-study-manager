package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.TypeParser;
import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;

public class MedicalRecordMigrate {


    private static final Map<String, Object> medicalRecordMapping1 = Map.of (
            "ddpParticipantId", TEXT_KEYWORD_MAPPING,
            "ddpInstanceId", TEXT_KEYWORD_MAPPING,
            "institutionId", TEXT_KEYWORD_MAPPING,
            "ddpInstitutionId", TEXT_KEYWORD_MAPPING,
            "type", TEXT_KEYWORD_MAPPING,
            "participantId", TEXT_KEYWORD_MAPPING,
            "medicalRecordId", TEXT_KEYWORD_MAPPING,
            "name", TEXT_KEYWORD_MAPPING,
            "contact", TEXT_KEYWORD_MAPPING );

    private static final Map<String, Object> medicalRecordMapping2 = Map.of (
            "phone", TEXT_KEYWORD_MAPPING,
            "fax", TEXT_KEYWORD_MAPPING,
            "faxSent", DATE_MAPPING, //DATE?
            "faxSentBy", TEXT_KEYWORD_MAPPING,
            "faxConfirmed", DATE_MAPPING, //DATE?
            "faxSent2", DATE_MAPPING, //DATE?
            "faxSent2By", TEXT_KEYWORD_MAPPING,
            "faxConfirmed2", DATE_MAPPING, //DATE?
            "faxSent3", DATE_MAPPING, //DATE?
            "faxSent3By", TEXT_KEYWORD_MAPPING);

    private static final Map<String, Object> medicalRecordMapping3 = Map.of (
            "faxConfirmed3", DATE_MAPPING, //DATE?
            "mrReceived", DATE_MAPPING, //DATE?
            "followUps", TEXT_KEYWORD_MAPPING,
            "mrDocument", TEXT_KEYWORD_MAPPING,
            "mrDocumentFileName", TEXT_KEYWORD_MAPPING,
            "mrProblem", BOOLEAN_MAPPING,
            "mrProblemText", TEXT_KEYWORD_MAPPING,
            "unableObtain", BOOLEAN_MAPPING,
            "unableObtainText", TEXT_KEYWORD_MAPPING,
            "duplicate", BOOLEAN_MAPPING);

    private static final Map<String, Object> medicalRecordMapping4 = Map.of (
            "followUpRequired", BOOLEAN_MAPPING,
            "followUpRequiredText", TEXT_KEYWORD_MAPPING,
            "international", BOOLEAN_MAPPING,
            "crRequired", BOOLEAN_MAPPING,
            "pathologyPresent", TEXT_KEYWORD_MAPPING,
            "notes", TEXT_KEYWORD_MAPPING,
            "additionalValuesJson", TEXT_KEYWORD_MAPPING,
            "reviewMedicalRecord", TEXT_KEYWORD_MAPPING
    );

    protected static final Map<String, Object> medicalRecordMappingMerged = MapAdapter.of(medicalRecordMapping1, medicalRecordMapping2,
            medicalRecordMapping3,
            medicalRecordMapping4);;


    protected List<String> collectMedicalRecordColumns() {
        Class<MedicalRecord> medicalRecordClass = MedicalRecord.class;
        Field[] fields = medicalRecordClass.getDeclaredFields();

        List<String> columnNames = Arrays.stream(fields)
                .map(AccessibleObject::getAnnotations)
                .map(annotations -> Arrays.stream(annotations).filter(this::isColumnNameType).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(annotation -> ((ColumnName) annotation).value())
                .collect(Collectors.toList());
        return columnNames;
    }

    protected List<String> swapToCamelCases(List<String> columnNames) {
        return columnNames.stream()
                .map(Util::underscoresToCamelCase)
                .collect(Collectors.toList());
    }

    private boolean isColumnNameType(Annotation annotation) {
        return annotation instanceof ColumnName;
    }
}