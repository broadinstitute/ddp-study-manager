package org.broadinstitute.dsm.model.elastic.filter;

import static org.junit.Assert.*;

import org.junit.Test;

public class MultipleOptionsSplitterTest {

    @Test
    public void getInnerProperty() {
        String filter = "( oD.fax_sent = 'review' OR oD.fax_sent = 'no' OR oD.fax_sent = 'hold' OR oD.fax_sent = 'request' OR oD.fax_sent = " +
                "'unable To Obtain' OR oD.fax_sent = 'sent' OR oD.fax_sent = 'received' OR oD.fax_sent = 'returned' )";
        BaseSplitter multipleSplitter = SplitterFactory.createSplitter(Operator.MULTIPLE_OPTIONS);
        multipleSplitter.setFilter(filter);
        assertEquals("faxSent", multipleSplitter.getInnerProperty());
    }
}