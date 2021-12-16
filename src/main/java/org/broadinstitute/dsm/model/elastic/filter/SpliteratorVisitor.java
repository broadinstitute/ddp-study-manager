package org.broadinstitute.dsm.model.elastic.filter;

import org.broadinstitute.dsm.model.Filter;

public interface SpliteratorVisitor {

    void visit(Unit unit);

    static SpliteratorVisitor instance(String filterValues) {
        SpliteratorVisitor spliteratorVisitor;
        if (filterValues.contains(Filter.EQUALS_TRIMMED)) {
            spliteratorVisitor = new EqualsVisitor();
        } else if (filterValues.contains(Filter.LIKE_TRIMMED)) {
            spliteratorVisitor = new LikeVisitor();
        } else {
            throw new IllegalArgumentException("Unknown operator");
        }
        return spliteratorVisitor;
    }

}
