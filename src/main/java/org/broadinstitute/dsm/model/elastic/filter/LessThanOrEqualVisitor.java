package org.broadinstitute.dsm.model.elastic.filter;

import java.util.Arrays;

import org.broadinstitute.dsm.model.Filter;

public class LessThanOrEqualVisitor implements SpliteratorVisitor {


    @Override
    public void visit(Unit lessThanOrEqualUnit) {
        lessThanOrEqualUnit.splittedWords = Arrays.asList(lessThanOrEqualUnit.valueToSplit.split(Filter.SMALLER_EQUALS_TRIMMED));
    }
}
