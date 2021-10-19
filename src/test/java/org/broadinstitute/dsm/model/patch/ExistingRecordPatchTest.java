package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.db.structure.DBElement;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.util.NotificationUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExistingRecordPatchTest {

    private static Patch patch;
    // String id, String parent, String parentId, String user, NameValue nameValue, List<NameValue> nameValues, String ddpParticipantId

    @BeforeClass
    public static void setUp() {
        String id = "id";
        String parent = "parent";
        String parentId = "parentId";
        String user = "user";
        NameValue nameValue = new NameValue("field", "value");
        int randNumb = new Random().nextInt(11);
        List<NameValue> nameValues = Stream.generate(() -> new NameValue("field" + randNumb, "value" + randNumb))
                .limit(10)
                .collect(Collectors.toList());
        String ddpParticipantId = "participantId";
        patch = new Patch(id, parent, parentId, user, nameValue, nameValues, ddpParticipantId);
    }

    @Test
    public void generateSource() {
        ExistingRecordPatch existingRecordPatch = new TestExistingRecordPatch(patch, null);
        Map<String, Object> obj = existingRecordPatch.generate(patch.getNameValue());
        Assert.assertEquals("value", obj.get("field"));
        existingRecordPatch.dbElement = new DBElement("tableName", "tbA", "key", "data");
        obj = existingRecordPatch.generate(patch.getNameValue());
        Assert.assertEquals("value", ((Map<String, Object>)obj.get("data")).get("field"));
    }

    private static class TestExistingRecordPatch extends ExistingRecordPatch {

        public TestExistingRecordPatch(Patch patch, NotificationUtil notificationUtil) {
            super(patch, notificationUtil);
            dbElement = new DBElement("tableName", "tbA", "key", "field");
        }

        @Override
        protected void prepareCommonData() {

        }
    }

}