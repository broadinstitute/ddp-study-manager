package org.broadinstitute.dsm.model.elastic.filter;

import org.elasticsearch.index.query.AbstractQueryBuilder;

public class CollectionQueryBuilder extends DsmAbstractQueryBuilder {

    public CollectionQueryBuilder(String filter) {
        super(filter);
    }

    @Override
    public AbstractQueryBuilder build() {

        String filter = "AND m.medicalRecordId = 15 AND m.dynamicFields.ragac = 55 OR m.medicalRecordName = 213";


        return null;
    }
}
