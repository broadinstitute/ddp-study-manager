package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.Map;
import java.util.Objects;

public abstract class BaseGenerator implements Generator {


    public static final String DSM_OBJECT = "dsm";
    public static final String PROPERTIES = "properties";
    public static final Map<String, PropertyInfo> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", new PropertyInfo("medicalRecords", true),
            "t", new PropertyInfo("tissueRecords", true),
            "oD", new PropertyInfo("oncHistoryDetailRecords", true),
            "r", new PropertyInfo("participant", false),
            "p", new PropertyInfo("participant", false),
            "d", new PropertyInfo("participant", false)
    );
    public static final String ID = "id";
    protected static final Gson GSON = new Gson();
    protected final Parser parser;
    protected GeneratorPayload generatorPayload;

    public BaseGenerator(Parser parser, GeneratorPayload generatorPayload) {
        this.parser = Objects.requireNonNull(parser);
        this.generatorPayload = Objects.requireNonNull(generatorPayload);
    }

    protected DBElement getDBElement() {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(getNameValue()).getName());
    }

    protected NameValue getNameValue() {
        return generatorPayload.getNameValue();
    }
    
    protected PropertyInfo getOuterPropertyByAlias() {
        return TABLE_ALIAS_MAPPINGS.get(getDBElement().getTableAlias());
    }
    
    protected Map<String, Object> collect() {
        Map<String, Object> sourceToUpsert;
        try {
            sourceToUpsert = parseJson();
        } catch (JsonSyntaxException jse) {
            sourceToUpsert = parseSingleElement();
        }
        return sourceToUpsert;
    }

    protected abstract Map<String, Object> parseJson();
    
    protected abstract Map<String, Object> parseSingleElement();

    protected Map<String, Object> getFieldWithElement() {
        Map<String, Object> fieldElementMap;
        Object element = parser.parse((String) getNameValue().getValue());
        if (getOuterPropertyByAlias().isCollection) {
            fieldElementMap = getElementWithId(element);
        } else {
            fieldElementMap = getElement(element);
        }
        return fieldElementMap;
    }

    protected abstract Map<String, Object> getElementWithId(Object element);

    protected abstract Map<String, Object> getElement(Object element);

    public static class PropertyInfo {

        String propertyName;
        boolean isCollection;

        public PropertyInfo(String propertyName, boolean isCollection) {
            this.propertyName = Objects.requireNonNull(propertyName);
            this.isCollection = isCollection;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public boolean isCollection() {
            return isCollection;
        }
    }
    
}

