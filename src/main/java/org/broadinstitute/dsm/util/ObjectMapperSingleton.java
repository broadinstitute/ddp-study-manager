package org.broadinstitute.dsm.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.broadinstitute.dsm.model.elastic.export.generate.JsonParseException;

import java.io.IOException;

public class ObjectMapperSingleton {

    private ObjectMapperSingleton() {}

    public static ObjectMapper instance() {
        return Helper.objectMapperInstance;
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        try {
            return Helper.objectMapperInstance.readValue(content, valueType);
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            throw new JsonParseException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Helper {
        private static final ObjectMapper objectMapperInstance = new ObjectMapper();
    }


}
