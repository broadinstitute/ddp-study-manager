package org.broadinstitute.dsm.model.elastic.filter.splitter;

import org.junit.Test;
import static org.junit.Assert.*;

public class JsonContainsSplitterTest {

    @Test
    public void getValue() {
//        JSON_CONTAINS(k.test_result, JSON_OBJECT('isCorrected', 'true'))
        BaseSplitter splitter = new JsonContainsSplitter();
        String filter = "JSON_CONTAINS(k.test_result, JSON_OBJECT('isCorrected', 'true'))";
        splitter.setFilter(filter);
        assertEquals("true", splitter.getValue()[0]);
    }


}
