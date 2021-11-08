package org.broadinstitute.dsm.model.elastic.migrationscript;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import org.broadinstitute.dsm.db.MedicalRecord;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.Exportable;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearch;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.elasticsearch.action.update.UpdateRequest;
import spark.utils.StringUtils;

import static org.broadinstitute.dsm.model.elastic.export.parse.TypeParser.*;

public class MedicalRecordMigrate implements Exportable {


    public static final ElasticSearch elasticSearch = new ElasticSearch();

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
            medicalRecordMapping4);
    private final BulkExportFacade bulkExportFacade;

    public MedicalRecordMigrate(String index) {
        bulkExportFacade = new BulkExportFacade(index);
    }


    @Override
    public void export(Map<String, Object> source) {

    }

    public void exportMedicalRecordsToES() {
        Map<String, List<Object>> medicalRecords = (Map) MedicalRecord.getMedicalRecords("brain");
        fillBulkRequest(medicalRecords);
        bulkExportFacade.executeBulkUpsert();
    }

    private static void fillBulkRequest(Map<String, List<Object>> participantRecords) {
        for (Map.Entry<String, List<Object>> entry: participantRecords.entrySet()) {
            String participantId = entry.getKey();
            List<Object> medicalRecordList = entry.getValue();
            participantId = getParticipantGuid(participantId);
            if (StringUtils.isBlank(participantId)) continue;
            List<Map<String, Object>> transformedList = Util.transformObjectCollectionToCollectionMap(medicalRecordList, MedicalRecord.class);
            setPrimaryId(transformedList);
            bulkExportFacade.addDataToRequest(generateSource(transformedList), participantId);
        }
    }

    private static void setPrimaryId(List<Map<String, Object>> transformedList) {
        for(Map<String, Object> map: transformedList) {
            map.put("id", map.get("medicalRecordId"));
        }
    }

    private static String getParticipantGuid(String participantId) {
        if (!(ParticipantUtil.isGuid(participantId))) {
            ElasticSearchParticipantDto participantById =
                    elasticSearch.getParticipantById("participants_structured.cmi.cmi-brain", participantId);
            participantId = participantById.getParticipantId();
        }
        return participantId;
    }

    public static Map generateSource(List<Map<String, Object>> transformedList) {
        return Map.of("dsm", Map.of("medicalRecords", transformedList));
    }


    protected static List<String> collectMedicalRecordColumns() {
        Class<MedicalRecord> medicalRecordClass = MedicalRecord.class;
        Field[] fields = medicalRecordClass.getDeclaredFields();

        List<String> columnNames = Arrays.stream(fields)
                .map(AccessibleObject::getAnnotations)
                .map(annotations -> Arrays.stream(annotations).filter(MedicalRecordMigrate::isColumnNameType).findFirst())
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

    private static boolean isColumnNameType(Annotation annotation) {
        return annotation instanceof ColumnName;
    }

}
