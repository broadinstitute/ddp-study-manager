package org.broadinstitute.dsm.route;

import static org.broadinstitute.dsm.TestHelper.cfg;
import static org.broadinstitute.dsm.TestHelper.setupDB;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.model.ddp.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.ddp.FamilyMemberDetails;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddFamilyMemberRouteTest {

    private static Config localCfg;

    private static AddFamilyMemberRoute addFamilyMemberRoute;

    private static Gson gson;

    private static String participantId = "RBMJW6ZIXVXBMXUX6M3Q";
    private static String realm = "testboston";
    private static Integer userId = 94;
    private static Map<String, String> data = new HashMap<>();

    @BeforeClass
    public static void doFirst() {
        setupDB();
        localCfg = cfg;

        addFamilyMemberRoute = new AddFamilyMemberRoute();
        gson = new Gson();

        data.put("firstName", "Family");
        data.put("lastName", "Member");
        data.put("memberType", "Sister");
        data.put("familyId", "PE3LHB");
        data.put("collaboratorParticipantId", "PE3LHB_1_2");
    }

    @AfterClass
    public static void finish() {

    }

    @Test
    public void noGuidProvided() {
        String payload = payloadFactory(null, realm, data, userId);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getParticipantGuid().orElseThrow(() -> new NoSuchElementException("Participant Guid is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("Participant Guid is not provided", nsee.getMessage());
        }
    }

    @Test
    public void noRealmProvided() {
        String payload = payloadFactory(participantId, null, data, userId);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getRealm().orElseThrow(() -> new NoSuchElementException("Realm is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("Realm is not provided", nsee.getMessage());
        }
    }

    @Test
    public void noFamilyMemberDataProvided(){
        String payload = payloadFactory(participantId, realm, Map.of(), userId);
        Result res = new Result(200);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        if (addFamilyMemberPayload.getData().isEmpty() || addFamilyMemberRoute.isFamilyMemberFieldsEmpty(addFamilyMemberPayload.getData())) {
            res = new Result(400, "Family member information is not provided");
        }
        Assert.assertEquals(res.getCode(), 400);
        Assert.assertEquals(res.getBody(), "Family member information is not provided");
    }

    @Test
    public void noUserIdProvided() {
        String payload = payloadFactory(participantId, realm, Map.of(), null);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getUserId().orElseThrow(() -> new NoSuchElementException("User id is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("User id is not provided", nsee.getMessage());
        }
    }


    public static String payloadFactory(String participantGuid, String realm, Map<String, String> data, Integer userId) {
        FamilyMemberDetails familyMemberDetails;
        if (data == null) {
            familyMemberDetails = null;
        } else {
            familyMemberDetails = new FamilyMemberDetails(
                data.get("firstName"),
                data.get("lastName"),
                data.get("memberType"),
                data.get("familyId"),
                data.get("collaboratorParticipantId")
            );
        }
        AddFamilyMemberPayload addFamilyMemberPayload = new AddFamilyMemberPayload(participantGuid, realm, familyMemberDetails, userId);

        return gson.toJson(addFamilyMemberPayload);
    }

}