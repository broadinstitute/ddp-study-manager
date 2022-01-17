package org.broadinstitute.dsm.model.elastic.filter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.broadinstitute.dsm.model.Filter;
import org.junit.Assert;
import org.junit.Ignore;
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

    @Test
    public void extractJsonExtractValue() {
        String filter = " ( d.additional_values_json , '$.status' )   LIKE  '%EXITED_BEFORE_ENROLLMENT%'";
        Operator likeOperator = Operator.extract(filter);
        assertEquals(Operator.LIKE, likeOperator);
    }

    @Test
    @Ignore
    public void collectOperatorsFromFilter() {
        String filter = "m.mr_received  >= STR_TO_DATE('1964-01-14','%Y-%m-%d')";
        List<String> operators = Operator.collectOperatorsFromFilter(filter);
        assertEquals(Arrays.asList(">=", "STR_TO_DATE"), operators);
    }

    @Test
    public void extractLikeOperator() {
        String filter = "m.mr_received LIKE 'someLIKEthing LIKE '";
        Operator operator = Operator.extractOperator(filter);
        assertEquals(Operator.LIKE, operator);
    }

    @Test
    public void extractEqualsOperator() {
        String filter = "m.mr_received = 'someLIKEthing LIKE'";
        String filter2 = "m.mr_received <= 18";
        Operator operator = Operator.extractOperator(filter);
//        Operator operator2 = Operator.extractOperator(filter2);
        assertEquals(Operator.EQUALS, operator);
    }
}