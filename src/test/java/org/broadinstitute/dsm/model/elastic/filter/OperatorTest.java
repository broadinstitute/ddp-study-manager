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
            Operator unknown = Operator.getOperator(Filter.TODAY);
            assertEquals(Operator.LIKE, like);
            assertEquals(Operator.EQUALS, equals);
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue(true);
        }
    }
}