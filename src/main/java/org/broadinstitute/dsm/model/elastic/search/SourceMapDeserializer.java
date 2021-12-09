package org.broadinstitute.dsm.model.elastic.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.model.elastic.ESDsm;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.broadinstitute.dsm.util.ObjectMapperSingleton;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SourceMapDeserializer implements Deserializer {

    String outerProperty;

    public Optional<ElasticSearchParticipantDto> deserialize(Map<String, Object> sourceMap) {
        Map<String, Object> dsmLevel = (Map<String, Object>) sourceMap.get(ESObjectConstants.DSM);

        if (Objects.isNull(dsmLevel)) return Optional.of(ObjectMapperSingleton.instance().convertValue(sourceMap, ElasticSearchParticipantDto.class));

        Map<String, Object> updatedPropertySourceMap = updatePropertySourceMapIfSpecialCases(dsmLevel);
        if (!updatedPropertySourceMap.isEmpty()) dsmLevel.putAll(updatedPropertySourceMap);

        return Optional.of(ObjectMapperSingleton.instance().convertValue(sourceMap, ElasticSearchParticipantDto.class));
    }

    private Map<String, Object> updatePropertySourceMapIfSpecialCases(Map<String, Object> dsmLevel) {
        Map<String, Object> updatedPropertySourceMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : dsmLevel.entrySet()) {
            outerProperty = entry.getKey();
            Object outerPropertyValue = entry.getValue();
            if (!hasDynamicFields(outerProperty)) continue;
            if (outerPropertyValue instanceof List) {
                List<Map<String, Object>> outerPropertyValues = (List<Map<String, Object>>) outerPropertyValue;
                List<Map<String, Object>> updatedOuterPropertyValues = handleSpecialCases(outerPropertyValues);;
                if (!updatedOuterPropertyValues.isEmpty())
                    updatedPropertySourceMap.put(outerProperty, updatedOuterPropertyValues);
            } else {
                Map<String, Object> singleOuterPropertyValue = (Map<String, Object>) outerPropertyValue;
                Map<String, Object> updatedSingleOuterPropertyValue = new HashMap<>(singleOuterPropertyValue);
                if (singleOuterPropertyValue.containsKey(ESObjectConstants.DYNAMIC_FIELDS))
                    updatedSingleOuterPropertyValue.put(ESObjectConstants.DYNAMIC_FIELDS, getDynamicFieldsValueAsJson(updatedSingleOuterPropertyValue));

                updatedPropertySourceMap.put(outerProperty, updatedSingleOuterPropertyValue);
            }
        }
        return updatedPropertySourceMap;
    }

    private List<Map<String, Object>> handleSpecialCases(List<Map<String, Object>> outerPropertyValues) {
        List<Map<String, Object>> updatedOuterPropertyValues = new ArrayList<>();
        for (Map<String, Object> object : outerPropertyValues) {
            Map<String, Object> clonedMap = new HashMap<>(object);
            if (object.containsKey(ESObjectConstants.DYNAMIC_FIELDS)) {
                clonedMap.put(ESObjectConstants.DYNAMIC_FIELDS, getDynamicFieldsValueAsJson(clonedMap));
            }
            if (object.containsKey(ESObjectConstants.FOLLOW_UPS)) {
                clonedMap.put(ESObjectConstants.FOLLOW_UPS, convertFollowUpsJsonToList(clonedMap));
            }
            updatedOuterPropertyValues.add(clonedMap);
        }
        return updatedOuterPropertyValues;
    }

    private List<Map<String, Object>> convertFollowUpsJsonToList(Map<String, Object> clonedMap) {
        String followUps = (String) clonedMap.get(ESObjectConstants.FOLLOW_UPS);
        try {
            return Objects.isNull(followUps)
                    ? Collections.emptyList()
                    : ObjectMapperSingleton.instance().readValue(followUps, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    String getDynamicFieldsValueAsJson(Map<String, Object> clonedMap) {
        Map<String, Object> dynamicFields = (Map<String, Object>) clonedMap.get(ESObjectConstants.DYNAMIC_FIELDS);
        if (ESObjectConstants.PARTICIPANT_DATA.equals(outerProperty)) {
            Map<String, Object> updatedParticipantDataDynamicFields = new HashMap<>();
            for (Map.Entry<String, Object> entry: dynamicFields.entrySet()) {
                updatedParticipantDataDynamicFields.put(Util.camelCaseToPascalSnakeCase(entry.getKey()), entry.getValue());
            }
            dynamicFields = updatedParticipantDataDynamicFields;
        }
        try {
            return dynamicFields.isEmpty()
                    ? StringUtils.EMPTY
                    : ObjectMapperSingleton.instance().writeValueAsString(dynamicFields);
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }

    private boolean hasDynamicFields(String outerProperty) {
        try {
            Field property = ESDsm.class.getDeclaredField(outerProperty);
            Class<?> propertyType = Util.getParameterizedType(property.getGenericType());
            Field[] declaredFields = propertyType.getDeclaredFields();
            return Arrays.stream(declaredFields).anyMatch(this::isDynamicField);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isDynamicField(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (Objects.isNull(jsonProperty)) return false;
        else return jsonProperty.value().equals(ESObjectConstants.DYNAMIC_FIELDS);
    }
}