package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.model.NameValue;
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
        ExistingRecordPatch existingRecordPatch = new ExistingRecordPatch(patch, null);
        Map<String, Object> obj = existingRecordPatch.generateSource(patch.getNameValue());
        Assert.assertEquals("value",obj.get("field"));
    }


}