package org.broadinstitute.dsm.model.elastic.filter;

import java.util.Arrays;

import org.broadinstitute.dsm.model.Filter;

public class EqualsVisitor implements SpliteratorVisitor {

    @Override
    public void visit(Unit equalsLikeUnit) {
        equalsLikeUnit.splittedWords = Arrays.asList(equalsLikeUnit.valueToSplit.split(Filter.EQUALS_TRIMMED));
    }


}
