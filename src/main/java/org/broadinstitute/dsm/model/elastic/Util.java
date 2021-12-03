package org.broadinstitute.dsm.model.elastic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.Json;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
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
            "r", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT, false),
            "p", new BaseGenerator.PropertyInfo(ESObjectConstants.PARTICIPANT, false),
            "o", new BaseGenerator.PropertyInfo(ESObjectConstants.ONC_HISTORY, false)
    );
    public static final int FIRST_ELEMENT_INDEX = 0;
    public static final String UNDERSCORE_SEPARATOR = "_";
    public static final String DOC = "_doc";
    private static final Pattern CAMEL_CASE_REGEX = Pattern.compile("(([a-z])+([A-z]))*");
    public static final Gson GSON = new Gson();

    public static String getQueryTypeFromId(String id) {
        String type;
        if (ParticipantUtil.isHruid(id)) {
            type = Constants.PROFILE_HRUID;
        } else if (ParticipantUtil.isGuid(id)) {
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
        String[] splittedWords = fieldName.split(UNDERSCORE_SEPARATOR);
        if (hasNoUnderscores(splittedWords)) return handleAllUppercase(fieldName);
        List<StringBuilder> words = Arrays.stream(splittedWords)
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

    private static String handleAllUppercase(String word) {
        return CAMEL_CASE_REGEX.matcher(word).matches() ? word : word.toLowerCase();
    }

    private static boolean hasNoUnderscores(String[] splittedWords) {
        return splittedWords.length < 2;
    }

    public static List<Map<String, Object>> transformObjectCollectionToCollectionMap(List<Object> values) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : values) {
            result.add(transformObjectToMap(obj));
        }
        return result;
    }

    public static Map<String, Object> transformObjectToMap(Object obj) {
        Map<String, Object> map = new HashMap<>();
        List<Field> declaredFields = new ArrayList(List.of(obj.getClass().getDeclaredFields()));
        List<Field> declaredFieldsSuper = new ArrayList(List.of(obj.getClass().getSuperclass().getDeclaredFields()));
        declaredFields.addAll(declaredFieldsSuper);
        for (Field declaredField : declaredFields) {
            ColumnName annotation = declaredField.getAnnotation(ColumnName.class);
            if (annotation == null) {
                continue;
            }
            try {
                declaredField.setAccessible(true);
                Object fieldValue = declaredField.get(obj);
                if (Objects.isNull(fieldValue)) {
                    continue;
                }
                map.putAll(convertToMap(annotation.value(), fieldValue));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }

    static Map<String, Object> convertToMap(String fieldName, Object fieldValue) {
        Map<String, Object> finalResult;
        switch (fieldName) {
            case "follow_ups":
                finalResult = new HashMap<>(Map.of(underscoresToCamelCase(fieldName), new Gson().toJson(fieldValue)));
                break;
            case "additional_tissue_value_json":
            case "additional_values_json":
            case "data":
                Map<String, Object> objectMap = dynamicFieldsSpecialCase(fieldValue);
                Map<String, Object> transformedMap = new HashMap<>();
                for (Map.Entry object: objectMap.entrySet()) {
                    String field = (String) object.getKey();
                    Object value = object.getValue();
                    String camelCaseField = underscoresToCamelCase(field);
                    transformedMap.put(camelCaseField, value);
                }
                finalResult = Map.of("dynamicFields", transformedMap);
                break;
            default:
                finalResult = new HashMap<>(Map.of(underscoresToCamelCase(fieldName), fieldValue));
                break;
        }
        return finalResult;
    }

    public static List<Map<String, Object>> convertObjectListToMapList(Object fieldValue) {
        return new ObjectMapper().convertValue(fieldValue, new TypeReference<List<Map<String, Object>>>() {});
    }

    private static Map<String, Object> dynamicFieldsSpecialCase(Object fieldValue) {
        Map<String, Object> dynamicMap = new HashMap<>();
        if (isJsonInString(fieldValue)) {
            String strValue = (String) fieldValue;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                dynamicMap = objectMapper.readValue(strValue, Map.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return dynamicMap;
    }

    private static boolean isJsonInString(Object fieldValue) {
        return fieldValue instanceof String && StringUtils.isNotBlank((String) fieldValue) && isJson((String) fieldValue);
    }

    private static boolean isJson(String str) {
        return getFirstChar(str) == '{' && getLastChar(str) == '}';
    }

    private static char getLastChar(String strValue) {
        if (Objects.isNull(strValue) || strValue.length() == 0) {
            throw new IllegalArgumentException();
        }
        return strValue.charAt(strValue.length() - 1);
    }

    private static char getFirstChar(String strValue) {
        if (Objects.isNull(strValue) || strValue.length() == 0) {
            throw new IllegalArgumentException();
        }
        return strValue.charAt(0);
    }

    public static Class<?> getParameterizedType(Type genericType) throws ClassNotFoundException {
        String typeAsString = genericType.toString();
        String[] types = typeAsString.contains("<") ? typeAsString.split("<") : typeAsString.split("\\[L");
        if (types.length < 2) {
//            class NullClass {}
//            return NullClass.class;
            return (Class) genericType;
        }
        String parameterizedType = types[1];
        parameterizedType = parameterizedType.replace(">", "");
        parameterizedType = parameterizedType.replace(";", "");
        return Class.forName(parameterizedType);
    }

    public static class Constants {
        public static final String PROFILE = "profile";
        public static final String PROFILE_HRUID = PROFILE + ".hruid";
        public static final String PROFILE_GUID = PROFILE + ".guid";
        public static final String PROFILE_LEGACYALTPID = PROFILE + ".legacyAltPid";
        public static final String PROFILE_LEGACYSHORTID = PROFILE + ".legacyShortId";
    }
}
