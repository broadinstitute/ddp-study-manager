package org.broadinstitute.dsm.model.elastic.export.painless;

import org.apache.lucene.search.join.ScoreMode;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.ESObjectConstants;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

public class NestedUpsertPainlessFacade extends UpsertPainlessFacade {

    NestedUpsertPainlessFacade(Object source, DDPInstanceDto ddpInstanceDto,
                                      String uniqueIdentifier, String fieldName, Object fieldValue) {
        super(source, ddpInstanceDto, uniqueIdentifier, fieldName, fieldValue);
    }

    @Override
    protected ScriptBuilder buildScriptBuilder() {
        return new NestedScriptBuilder(generator.getPropertyName(), uniqueIdentifier);
    }

    @Override
    protected QueryBuilder buildQueryBuilder() {
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder(fieldName, fieldValue);
        if ("_id".equals(fieldName)) return matchQueryBuilder;
        return new NestedQueryBuilder(String.join(DBConstants.ALIAS_DELIMITER, ESObjectConstants.DSM, generator.getPropertyName(),
                fieldName), matchQueryBuilder, ScoreMode.Avg);
    }
}
