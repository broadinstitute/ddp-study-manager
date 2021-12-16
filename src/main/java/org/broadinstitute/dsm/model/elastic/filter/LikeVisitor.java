package org.broadinstitute.dsm.model.elastic.filter;

import java.util.Arrays;

import org.broadinstitute.dsm.model.Filter;

public class LikeVisitor implements SpliteratorVisitor{

    @Override
    public void visit(Unit unit) {
        unit.splittedWords = Arrays.asList(unit.valueToSplit.split(Filter.LIKE_TRIMMED));
    }
}
