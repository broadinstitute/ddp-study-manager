package org.broadinstitute.dsm.util;

import com.google.gson.Gson;
import spark.ResponseTransformer;

public class TestJsonTransformer implements ResponseTransformer {

    public TestJsonTransformer() {
    }

    public String render(Object model) {
        return new Gson().toJson(model);
    }
}
