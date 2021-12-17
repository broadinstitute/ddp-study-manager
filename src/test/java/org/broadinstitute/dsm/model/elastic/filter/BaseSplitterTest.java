package org.broadinstitute.dsm.model.elastic.filter;

import static org.junit.Assert.*;

import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.junit.Test;

public class BaseSplitterTest {

    @Test
    public void getAlias() {
        DiamondEqualsSplitter diamondEqualsSplitter = getDiamondEqualsSplitter();
        assertEquals("m", diamondEqualsSplitter.getAlias());
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