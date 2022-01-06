package org.broadinstitute.dsm.model.elastic.filter;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.model.Filter;
import org.junit.Assert;
import org.junit.Test;

public class OperatorTest {

    @Test
    public void getOperator() {
        try {
            Operator like = Operator.getOperator(Filter.LIKE_TRIMMED);
            Operator equals = Operator.getOperator(Filter.EQUALS_TRIMMED);
            Operator larger = Operator.getOperator(Filter.LARGER_EQUALS_TRIMMED);
            Operator smaller = Operator.getOperator(Filter.SMALLER_EQUALS_TRIMMED);
            Operator isNotNull = Operator.getOperator(Filter.IS_NOT_NULL_TRIMMED);
            Operator unknown = Operator.getOperator(Filter.TODAY);
            assertEquals(Operator.LIKE, like);
            assertEquals(Operator.EQUALS, equals);
            assertEquals(Operator.GREATER_THAN_EQUALS, larger);
            assertEquals(Operator.LESS_THAN_EQUALS, smaller);
            assertEquals(Operator.IS_NOT_NULL, isNotNull);
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void extractLogicalOperator() {
        String filterEquals = "m.medicalRecordId = 15";
        String filterLike = "m.medicalRecordId LIKE 15";
        String notFilter = "NOT m.mrProblem <=> 1";

        Operator equalsOperator = Operator.extract(filterEquals);
        Operator likeOperator = Operator.extract(filterLike);
        Operator diamondsOperator = Operator.extract(notFilter);

        assertEquals(Operator.EQUALS, equalsOperator);
        assertEquals(Operator.LIKE, likeOperator);
        assertEquals(Operator.DIAMOND_EQUALS, diamondsOperator);
    }
}