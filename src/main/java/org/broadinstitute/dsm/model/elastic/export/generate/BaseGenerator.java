package org.broadinstitute.dsm.model.elastic.export.generate;

import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.db.structure.TableName;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.model.elastic.Util;
import org.broadinstitute.dsm.model.elastic.export.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseGenerator implements Generator, Collector, GeneratorHelper {

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
        return Util.getDBElement(getNameValue().getName());
    }

    private PropertyInfo getOuterPropertyByAlias() {
        return Util.TABLE_ALIAS_MAPPINGS.get(getDBElement().getTableAlias());
    }

    protected String getPrimaryKey() {
        return getOuterPropertyByAlias().getPrimaryKey();
    }

    public String getPropertyName() {
        return getOuterPropertyByAlias().getPropertyName();
    }

    public String getFieldName() {
        return generatorPayload.getCamelCaseFieldName();
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

    public static class PropertyInfo {

        private Class<?> propertyClass;
        private boolean isCollection;
        private String fieldName;


        public PropertyInfo(Class<?> propertyClass, boolean isCollection) {
            this.propertyClass = Objects.requireNonNull(propertyClass);
            this.isCollection = isCollection;
        }

        public void setIsCollection(boolean isCollection) {
            this.isCollection = isCollection;
        }

        public String getPropertyName() {
            return Util.capitalCamelCaseToLowerCamelCase(propertyClass.getSimpleName());
        }

        public String getPrimaryKey() {
            TableName tableName = Objects.requireNonNull(propertyClass.getAnnotation(TableName.class));
            return Util.underscoresToCamelCase(tableName.primaryKey());
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

        public Class<?> getPropertyClass() {
            return propertyClass;
        }
    }
    
}

