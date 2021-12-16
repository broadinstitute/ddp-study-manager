package org.broadinstitute.dsm.model.elastic.filter;

import java.util.Arrays;

import org.broadinstitute.dsm.model.Filter;

public class GreaterThanOrEqualVisitor implements SpliteratorVisitor {

    @Override
    public void visit(Unit greaterThanOrEqualUnit) {
        greaterThanOrEqualUnit.splittedWords = Arrays.asList(greaterThanOrEqualUnit.valueToSplit.split(Filter.LARGER_EQUALS_TRIMMED));
    }
}
