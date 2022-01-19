package org.broadinstitute.dsm.model.elastic.export.painless;

import org.broadinstitute.dsm.model.elastic.export.RequestPayload;
import org.broadinstitute.dsm.model.elastic.export.generate.Generator;

public class SingleUpsert extends BaseUpsertPainless {

    private final static String SCRIPT = "" +
            "if (ctx._source.dsm.#propertyName == null) " +
                "{ctx._source.dsm.#propertyName = params.dsm.#propertyName} " +
            "else {" +
                "for (entry in params.dsm.#propertyName.entrySet()) " +
                    "{ ctx._source.dsm.#propertyName.put(entry.getKey(), entry.getValue()) }" +
            "}";

    public SingleUpsert(Generator generator,
                        RequestPayload requestPayload) {
        super(SCRIPT, generator, requestPayload);
    }
}
