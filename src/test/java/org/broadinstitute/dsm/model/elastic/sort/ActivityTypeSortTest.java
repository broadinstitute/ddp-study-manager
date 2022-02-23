package org.broadinstitute.dsm.model.elastic.sort;

import static org.junit.Assert.*;

import java.util.Optional;

import org.broadinstitute.dsm.db.dao.settings.FieldSettingsDao;
import org.broadinstitute.dsm.db.dto.settings.FieldSettingsDto;
import org.broadinstitute.dsm.model.elastic.MockFieldTypeExtractor;
import org.junit.Before;
import org.junit.Test;

public class ActivityTypeSortTest {

    @Before
    public void setUp() {
        FieldSettingsDao.setInstance(new MockFieldSettingsDao());
    }


    @Test
    public void handleInnerPropertySpecialCase() {
        SortBy sortBy = new SortBy.Builder()
                .withType("ACTIVITY")
                .withOrder("ASC")
                .withInnerProperty("DATSTAT_GENDER")
                .withOuterProperty("AT_PARTICIPANT_INFO")
                .withTableAlias("participantData")
                .build();
        Sort sort = new ActivityTypeSort(sortBy, SortTest.mockFieldTypeExractorByFieldAndType("REGISTRATION_GENDER", ""));
        String fieldName = sort.buildFieldName();
        assertEquals("activities.questionsAnswers.REGISTRATION_GENDER", fieldName);
    }
}


class MockFieldSettingsDao extends FieldSettingsDao {

    @Override
    public Optional<FieldSettingsDto> getFieldSettingsByFieldTypeAndColumnName(String fieldType, String columnName) {
        String possibleValues = "[{\"value\":\"REGISTRATION.REGISTRATION_GENDER\"}]";
        FieldSettingsDto fieldSettingsDto = new FieldSettingsDto.Builder(0)
                .withPossibleValues(possibleValues)
                .build();
        return Optional.of(fieldSettingsDto);
    }
}