package org.broadinstitute.dsm.model.elastic.sort;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.mapping.TypeExtractor;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;

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
        maybeFieldSettings.ifPresentOrElse();
        return super.handleInnerPropertySpecialCase();
    }
}
