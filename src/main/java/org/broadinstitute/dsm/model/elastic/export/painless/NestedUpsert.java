package org.broadinstitute.dsm.model.elastic.export.painless;

import org.broadinstitute.dsm.model.elastic.export.RequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;

public class NestedUpsert extends BaseUpsertPainless {

    private final static String SCRIPT = "if (ctx._source.dsm.#propertyName == null) {ctx._source.dsm.#propertyName = [params.dsm" +
            ".#propertyName]} " +
            "else {def targets = ctx._source.dsm.#propertyName.findAll(obj -> obj.#uniqueId == params.dsm.#propertyName.#uniqueId);" +
            " " +
            "for(target in targets) { for (entry in params.dsm.#propertyName.entrySet()) { target.put(entry.getKey(), entry.getValue()) } " +
            "}}";

    public NestedUpsert(Generator generator,
                        RequestPayload requestPayload) {
        super(processScript(), generator, requestPayload);
    }

    private static String processScript() {
        return null;
    }
}
