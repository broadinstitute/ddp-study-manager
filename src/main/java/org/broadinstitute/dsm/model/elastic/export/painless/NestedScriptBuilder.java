package org.broadinstitute.dsm.model.elastic.export.painless;

public class NestedScriptBuilder implements ScriptBuilder {

    private final static String SCRIPT = "if (ctx._source.dsm.#propertyName == null) {ctx._source.dsm.#propertyName = [params.dsm.#propertyName]} " +
            "else {def targets = ctx._source.dsm.#propertyName.findAll(obj -> obj.#uniqueIdentifier == params.dsm.#propertyName.#uniqueIdentifier); " +
            "for(target in targets) { for (entry in params.dsm.#propertyName.entrySet()) { target.put(entry.getKey(), entry.getValue()) } }}";

    private final String propertyName;
    private final String uniqueIdentifier;

    public NestedScriptBuilder(String propertyName, String uniqueIdentifier) {
        this.propertyName = propertyName;
        this.uniqueIdentifier = uniqueIdentifier;
    }

    @Override
    public String build() {
        return SCRIPT.replace("#propertyName", propertyName)
                .replace("#uniqueIdentifier", uniqueIdentifier);
    }
}
