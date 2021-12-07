package org.broadinstitute.dsm.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperSingleton {

    private ObjectMapperSingleton() {}

    public static ObjectMapper instance() {
        return Helper.objectMapperInstance;
    }

    private static class Helper {
        private static final ObjectMapper objectMapperInstance = new ObjectMapper();
    }


}
