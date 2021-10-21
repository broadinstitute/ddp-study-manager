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
    protected static final Gson GSON = new Gson();
    protected final Parser parser;

    protected NameValue nameValue;
    protected DBElement dbElement;
    protected GeneratorPayload generatorPayload;

    public BaseGenerator(Parser parser) {
        this.parser = parser;
    }

    protected void initializeNecessaryFields() {
        this.nameValue = generatorPayload.getNameValue();
        dbElement = getDBElement();
    }

    protected DBElement getDBElement() {
        return PatchUtil.getColumnNameMap().get(Objects.requireNonNull(nameValue).getName());
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

    protected PropertyInfo getOuterPropertyByAlias() {
        return TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
    }

    protected abstract Map<String, Object> parseJson();
    
    protected abstract Map<String, Object> parseSingleElement();

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

