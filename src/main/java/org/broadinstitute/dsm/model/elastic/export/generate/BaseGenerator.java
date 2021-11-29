package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseGenerator implements Generator, Collector, Merger {

    private static final Logger logger = LoggerFactory.getLogger(BaseGenerator.class);

    public static final String DSM_OBJECT = "dsm";
    public static final String PROPERTIES = "properties";
    protected static final Gson GSON = new Gson();
    protected Parser parser;
    protected GeneratorPayload generatorPayload;
    private DBElement dbElement;

    public BaseGenerator(Parser parser, GeneratorPayload generatorPayload) {
        this.parser = Objects.requireNonNull(parser);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
        dbElement = Util.getDBElement(getNameValue().getName());
    }

    public BaseGenerator() {

    }

    @Override
    public void setParser(Parser parser) {
        this.parser = Objects.requireNonNull(parser);
    }

    @Override
    public void setPayload(GeneratorPayload generatorPayload) {
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
    }

    //setter method to set dbElement for testing only!!!
    public void setDBElement(DBElement dbElement) {
        this.dbElement = dbElement;
    }

    protected NameValue getNameValue() {
        return generatorPayload.getNameValue();
    }

    //wrap Util.getDBElement in protected method so that we can override it in testing class for tests
    protected DBElement getDBElement() {
//        if (dbElement == null)
//            dbElement = Util.getDBElement(getNameValue().getName());
        return Util.getDBElement(getNameValue().getName());
    }

    private PropertyInfo getOuterPropertyByAlias() {
        return Util.TABLE_ALIAS_MAPPINGS.get(getDBElement().getTableAlias());
    }

    public String getPropertyName() {
        return getOuterPropertyByAlias().getPropertyName();
    }

    public String getFieldName() {
        return Util.underscoresToCamelCase(getDBElement().getColumnName());
    }

    @Override
    public Object collect() {
        Object sourceToUpsert;
        try {
            sourceToUpsert = parseJson();
        } catch (JsonSyntaxException jse) {
            sourceToUpsert = parseSingleElement();
        }
        return sourceToUpsert;
    }

    protected abstract <T> T parseJson();

    protected Map<String, Object> parseJsonToMapFromValue() {
        return GSON.fromJson(String.valueOf(getNameValue().getValue()), Map.class);
    }

    protected Object parseSingleElement() {
        Object element = parser.parse(String.valueOf(getNameValue().getValue()));
        return getElement(element);
    };

    protected abstract Object getElement(Object type);

    public abstract Object construct();

    @Override
    public Map<String, Object> merge(Map<String, Object> base, Map<String, Object> toMerge) {
        return new HashMap<>();
    }

    public static class PropertyInfo {

        private String propertyName;
        private boolean isCollection;
        private String fieldName;

        public PropertyInfo(String propertyName, boolean isCollection) {
            this.propertyName = Objects.requireNonNull(propertyName);
            this.isCollection = isCollection;
        }

        public void setIsCollection(boolean isCollection) {
            this.isCollection = isCollection;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public boolean isCollection() {
            return isCollection;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = Objects.requireNonNull(fieldName);
        }

        public String getFieldName() {
            if (StringUtils.isBlank(this.fieldName)) this.fieldName = "";
            return this.fieldName;
        }
    }
    
}

