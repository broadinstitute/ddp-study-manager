package org.broadinstitute.dsm.model.elastic.export;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.PatchUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class BaseGenerator implements Generator {


    public static final String DSM_OBJECT = "dsm";
    public static final String PROPERTIES = "properties";
    public static final Map<String, String> TABLE_ALIAS_MAPPINGS = Map.of(
            "m", "medicalRecords",
            "t", "tissueRecords",
            "oD", "oncHistoryDetailRecords",
            "r", "participant",
            "p", "participant",
            "d", "participant"
    );
    protected static final Gson GSON = new Gson();
    protected final Parser parser;

    protected NameValue nameValue;
    protected DBElement dbElement;

    public BaseGenerator(Parser parser) {
        this.parser = parser;
    }

    protected void initializeNecessaryFields(NameValue nameValue) {
        this.nameValue = nameValue;
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

    protected String getOuterPropertyByAlias() {
        return TABLE_ALIAS_MAPPINGS.get(dbElement.getTableAlias());
    }

    protected abstract Map<String, Object> parseJson();
    
    protected abstract Map<String, Object> parseSingleElement();
    
}

