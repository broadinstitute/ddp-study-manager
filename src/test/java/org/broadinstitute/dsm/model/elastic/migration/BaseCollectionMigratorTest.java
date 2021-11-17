package org.broadinstitute.dsm.model.elastic.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.broadinstitute.dsm.db.OncHistoryDetail;
import org.broadinstitute.dsm.db.Tissue;
import org.junit.Assert;
import org.junit.Test;

public class BaseCollectionMigratorTest {

    @Test
    public void transformObject() {
        BaseCollectionMigrator baseCollectionMigrator = new MockBaseCollectionMigrator("index", "realm", "object");
        baseCollectionMigrator.transformObject(mockData());
        Map<String, Object> objectMap = baseCollectionMigrator.transformedList.get(0);
        Object primaryId = objectMap.get("id");
        Assert.assertEquals("23", primaryId);
        Map<String, Object> tissue = (Map<String, Object>) ((List) objectMap.get("tissues")).get(0);
        Object tissuePrimaryId = tissue.get("id");
        Assert.assertEquals("11", tissuePrimaryId);
        Assert.assertEquals(tissuePrimaryId, tissue.get("tissueId"));
    }

    private List mockData() {
        List<Tissue> fieldValue = new ArrayList<>(List.of(new Tissue("11", "22",
                null, null, null, "awdwadawdawdawd", null, null, null, null, null, null,
                null, null, "Awdawd", null, null, null, null, null, null, null,
                null, null, null, null, null), new Tissue("555", "777",
                null, null, null, null, null, null, null, "awdawd", null, null,
                null, null, "awdawddwa", null, null, null, null, null, null, null,
                null, null, null, null, null)));
        OncHistoryDetail oncHistoryDetail =
                new OncHistoryDetail("23", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, fieldValue, null, null, false);
        return Collections.singletonList(oncHistoryDetail);
    }

    static class MockBaseCollectionMigrator extends BaseCollectionMigrator {

        public MockBaseCollectionMigrator(String index, String realm, String object) {
            super(index, realm, object);
        }

        @Override
        protected Map<String, Object> getDataByRealm() {
            return Map.of();
        }
    }
}