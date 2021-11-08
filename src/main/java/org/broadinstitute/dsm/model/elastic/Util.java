package org.broadinstitute.dsm.model.elastic;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.structure.ColumnName;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.elastic.export.generate.BaseGenerator;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ParticipantUtil;
import org.broadinstitute.dsm.util.PatchUtil;

public class Util {

    public static final Map<String, BaseGenerator.PropertyInfo> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", new BaseGenerator.PropertyInfo(ESObjectConstants.MEDICAL_RECORDS, true),
            "t", new BaseGenerator.PropertyInfo(ESObjectConstants.TISSUE_RECORDS, true),
            "oD", new BaseGenerator.PropertyInfo(ESObjectConstants.ONC_HISTORY_DETAIL_RECORDS, true),
            "d", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT_DATA, true),
            "r", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT_RECORD, false),
            "p", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT, false),
            "o", new BaseGenerator.PropertyInfo(ESObjectConstants.ONC_HISTORY, false)
            );
    public static final int FIRST_ELEMENT_INDEX = 0;
    public static final String UNDERSCORE_SEPARATOR = "_";
    public static final String DOC = "_doc";

    public static String getQueryTypeFromId(String id) {
        String type;
        if (ParticipantUtil.isHruid(id)) {
            type = Constants.PROFILE_HRUID;
        } else if (ParticipantUtil.isGuid(id)){
            type = Constants.PROFILE_GUID;
        } else if (ParticipantUtil.isLegacyAltPid(id)) {
            type = Constants.PROFILE_LEGACYALTPID;
        } else {
            type = Constants.PROFILE_LEGACYSHORTID;
        }
        return type;
    }

    public static DBElement getDBElement(String fieldName) {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(fieldName));
    }

    public static String underscoresToCamelCase(String fieldName) {
        List<StringBuilder> words = Arrays.stream(fieldName.split(UNDERSCORE_SEPARATOR))
                .map(word -> new StringBuilder(word.toLowerCase()))
                .collect(Collectors.toList());
        for (int i = FIRST_ELEMENT_INDEX; i < words.size(); i++) {
            StringBuilder word = words.get(i);
            if (i != FIRST_ELEMENT_INDEX && word.length() > FIRST_ELEMENT_INDEX) {
                word.replace(FIRST_ELEMENT_INDEX, 1, String.valueOf(word.charAt(FIRST_ELEMENT_INDEX)).toUpperCase());
            }
        }
        return String.join("", words);
    }

    public static List<Map<String, Object>> transformObjectCollectionToCollectionMap(List<Object> values, Class aClass) {
        List<Map<String, Object>> result = new ArrayList<>();
        Field[] declaredFields = aClass.getDeclaredFields();
        for (Object obj: values) {
            Map<String, Object> map = new HashMap<>();
            for (Field declaredField : declaredFields) {
                ColumnName annotation = declaredField.getAnnotation(ColumnName.class);
                if (annotation == null) continue;
                try {
                    declaredField.setAccessible(true);
                    Object fieldValue = declaredField.get(obj);
                    if (Objects.isNull(fieldValue)) continue;
                    String key = underscoresToCamelCase(annotation.value());
                    if (key.equals("followUps")) {
                        fieldValue = new Gson().toJson(fieldValue);
                    }
                    map.put(key, fieldValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            result.add(map);
        }
        return result;
    }

    public static class Constants {
        public static final String PROFILE = "profile";
        public static final String PROFILE_HRUID = PROFILE + ".hruid";
        public static final String PROFILE_GUID = PROFILE + ".guid";
        public static final String PROFILE_LEGACYALTPID = PROFILE + ".legacyAltPid";
        public static final String PROFILE_LEGACYSHORTID = PROFILE + ".legacyShortId";
    }
}
