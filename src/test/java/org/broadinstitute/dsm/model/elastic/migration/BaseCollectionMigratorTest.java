package org.broadinstitute.dsm.model.elastic.migration;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.Tissue;
import org.junit.Test;

public class BaseCollectionMigratorTest {

    @Test
    public void transformObject() {
        BaseCollectionMigrator baseCollectionMigrator = new MockBaseCollectionMigrator("index", "realm", "object", "primaryId");
        baseCollectionMigrator.transformObject(something());
        System.out.println();
    }

    private List something() {
        List<Tissue> fieldValue = new ArrayList<>(List.of(new Tissue("11", "22",
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null)));
        OncHistoryDetail oncHistoryDetail =
                new OncHistoryDetail(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, fieldValue, null, null, false);
        return Collections.singletonList(oncHistoryDetail);
    }

    static class MockBaseCollectionMigrator extends BaseCollectionMigrator {

        public MockBaseCollectionMigrator(String index, String realm, String object, String primaryId) {
            super(index, realm, object, primaryId);
        }

        @Override
        protected Map<String, Object> getDataByRealm() {
            return Map.of();
        }
    }
}