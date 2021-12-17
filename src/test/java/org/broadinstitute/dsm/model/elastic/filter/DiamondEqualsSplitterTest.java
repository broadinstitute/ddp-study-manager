package org.broadinstitute.dsm.model.elastic.filter;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiamondEqualsSplitterTest {

    @Test
    public void getAlias() {
        DiamondEqualsSplitter diamondEqualsSplitter = getDiamondEqualsSplitter();
        assertEquals("m", diamondEqualsSplitter.getAlias());
    }

    @Test
    public void getValue() {
        DiamondEqualsSplitter diamondEqualsSplitter = getDiamondEqualsSplitter();
        assertEquals("'1'", diamondEqualsSplitter.getValue());
    }

    @Test
    public void getInnerProperty() {
        DiamondEqualsSplitter diamondEqualsSplitter = getDiamondEqualsSplitter();
        assertEquals("international", diamondEqualsSplitter.getInnerProperty());
    }

    private DiamondEqualsSplitter getDiamondEqualsSplitter() {
        String not = "NOT m.international <=> 1";
        DiamondEqualsSplitter diamondEqualsSplitter = new DiamondEqualsSplitter();
        diamondEqualsSplitter.setFilter(not);
        return diamondEqualsSplitter;
    }
}