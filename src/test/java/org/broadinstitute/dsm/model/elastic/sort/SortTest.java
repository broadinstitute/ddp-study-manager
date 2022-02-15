package org.broadinstitute.dsm.model.elastic.sort;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SortTest {

    @Test
    public void buildJsonArrayFieldName() {

        SortBy sortBy = new SortBy.Builder()
                .withType("JSONARRAY")
                .withAdditionalType("CHECKBOX")
                .withOrder("ASC")
                .withOuterProperty("testResult")
                .withInnerProperty("isCorrected")
                .withTableAlias("k")
                .build();

        Sort sort = new Sort(sortBy);
        String fieldName = sort.buildFieldName();

        assertEquals("dsm.kitRequestShipping.testResult.isCorrected", fieldName);
    }

    @Test
    public void buildAdditionalValueFieldName() {
        SortBy sortBy = new SortBy.Builder()
                .withType("ADDITIONALVALUE")
                .withAdditionalType("TEXT")
                .withOrder("ASC")
                .withInnerProperty("hello")
                .withTableAlias("m")
                .build();

        Sort sort = new Sort(sortBy);
        String fieldName = sort.buildFieldName();
        assertEquals("dsm.medicalRecord.dynamicFields.hello.keyword", fieldName);
    }

    @Test
    public void buildSingleFieldName() {
        SortBy sortBy = new SortBy.Builder()
                .withType("TEXT")
                .withOrder("ASC")
                .withInnerProperty("notes")
                .withTableAlias("m")
                .build();
        Sort sort = new Sort(sortBy);
        String fieldName = sort.buildFieldName();
        assertEquals("dsm.medicalRecord.notes.keyword", fieldName);
    }

    @Test
    public void buildOneLevelNestedPath() {
        SortBy sortBy = new SortBy.Builder()
                .withType("TEXT")
                .withOrder("ASC")
                .withInnerProperty("notes")
                .withTableAlias("m")
                .build();
        Sort sort = new Sort(sortBy);
        String nestedPath = sort.buildNestedPath();
        assertEquals("dsm.medicalRecord", nestedPath);
    }

    @Test
    public void buildTwoLevelNestedPath() {
        SortBy sortBy = new SortBy.Builder()
                .withType("JSONARRAY")
                .withOrder("ASC")
                .withInnerProperty("result")
                .withTableAlias("k")
                .withOuterProperty("testResult")
                .build();
        Sort sort = new Sort(sortBy);
        String nestedPath = sort.buildNestedPath();
        assertEquals("dsm.kitRequestShipping.testResult", nestedPath);
    }

    @Test
    public void handleOuterPropertySpecialCase() {
        SortBy sortBy = new SortBy.Builder()
                .withType("RADIO")
                .withOrder("ASC")
                .withInnerProperty("REGISTRATION_STATUS")
                .withTableAlias("participantData")
                .withOuterProperty("AT_GROUP_MISCELLANEOUS")
                .build();
        Sort sort = new Sort(sortBy);
        String outerProperty = sort.handleOuterPropertySpecialCase();
        assertEquals("dynamicFields", outerProperty);
    }

    @Test
    public void buildParticipantDataFieldName() {
        SortBy sortBy = new SortBy.Builder()
                .withType("RADIO")
                .withOrder("ASC")
                .withInnerProperty("REGISTRATION_STATUS")
                .withTableAlias("participantData")
                .withOuterProperty("AT_GROUP_MISCELLANEOUS")
                .build();
        Sort sort = new Sort(sortBy);
        String outerProperty = sort.buildFieldName();
        assertEquals("dsm.participantData.dynamicFields.registrationStatus", outerProperty);
    }



}