package org.broadinstitute.dsm.model.elastic.sort;

import static org.junit.Assert.*;

import org.junit.Test;

public class CustomSortBuilderTest {

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
        String fieldName = CustomSortBuilder.buildFieldName(sortBy);
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
        String fieldName = CustomSortBuilder.buildFieldName(sortBy);
        assertEquals("dsm.medicalRecord.dynamicFields.hello", fieldName);
    }

    @Test
    public void buildSingleFieldName() {
        SortBy sortBy = new SortBy.Builder()
                .withType("TEXT")
                .withOrder("ASC")
                .withInnerProperty("notes")
                .withTableAlias("m")
                .build();
        String fieldName = CustomSortBuilder.buildFieldName(sortBy);
        assertEquals("dsm.medicalRecord.notes", fieldName);
    }
}