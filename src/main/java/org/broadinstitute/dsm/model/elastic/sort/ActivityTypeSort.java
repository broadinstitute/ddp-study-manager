package org.broadinstitute.dsm.model.elastic.sort;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.mapping.TypeExtractor;
import org.broadinstitute.dsm.model.settings.field.FieldSettings;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.broadinstitute.dsm.util.proxy.jackson.ObjectMapperSingleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActivityTypeSort extends Sort {

    ActivityTypeSort(SortBy sortBy, TypeExtractor<Map<String, String>> typeExtractor) {
        super(sortBy, typeExtractor);
    }

    @Override
    String handleOuterPropertySpecialCase() {
        return String.join(DBConstants.ALIAS_DELIMITER, ElasticSearchUtil.ACTIVITIES, ElasticSearchUtil.QUESTIONS_ANSWER);
    }

    @Override
    public String handleInnerPropertySpecialCase() {
        FieldSettingsDao fieldSettingsDao = FieldSettingsDao.of();
        Optional<FieldSettingsDto> maybeFieldSettings = fieldSettingsDao.getFieldSettingsByFieldTypeAndColumnName(sortBy.getOuterProperty(), sortBy.getInnerProperty());
        Optional<String> maybePossibleValues = maybeFieldSettings
                .map(FieldSettingsDto::getPossibleValues);
        if (maybePossibleValues.isPresent()) {
            String possibleValuesString = maybePossibleValues.get();
            List<Map<String, String>> possibleValues = ObjectMapperSingleton.readValue(possibleValuesString,
                    new TypeReference<List<Map<String, String>>>() {});
            return getFieldNameToSortBy(possibleValues);
        }
        return StringUtils.EMPTY;
    }

    @Override
    String getAliasValue(Alias alias) {
        return StringUtils.EMPTY;
    }

    private String getFieldNameToSortBy(List<Map<String, String>> possibleValues) {
        return possibleValues.stream()
                .findFirst()
                .map(mapValue -> mapValue.get(FieldSettings.KEY_VALUE))
                .map(value -> value.split(ElasticSearchUtil.ESCAPE_CHARACTER_DOT_SEPARATOR)[1])
                .orElse(StringUtils.EMPTY);
    }
}
