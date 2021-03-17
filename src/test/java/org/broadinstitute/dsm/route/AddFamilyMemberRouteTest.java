package org.broadinstitute.dsm.route;

import static org.broadinstitute.dsm.TestHelper.setupDB;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.Gson;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.model.NewParticipantData;
import org.broadinstitute.dsm.model.ddp.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.ddp.FamilyMemberDetails;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import spark.utils.StringUtils;

public class AddFamilyMemberRouteTest {

    private static Gson gson;

    private static final String participantId = "RBMJW6ZIXVXBMXUX6M3Q";

    private static User user;

    private static String ddpParticipantDataId;

    private static final Map<String, String> data = new HashMap<>();

    private static DDPInstanceDto ddpInstanceDto;
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();

    private static final UserDao userDao = new UserDao();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();

    @BeforeClass
    public static void doFirst() {
        setupDB();
        gson = new Gson();

        createTestDdpInstance();

        createTestDsmUser();

        createTestDataOfFamilyMember();

    }

    private static void createTestDdpInstance() {
        ddpInstanceDto = DDPInstanceDto.of(true, true, true);
        ddpInstanceDto.setInstanceName("AddFamilyMemberInstance");
        int testCreatedInstanceId = ddpInstanceDao.create(ddpInstanceDto);
        ddpInstanceDto.setDdpInstanceId(testCreatedInstanceId);
    }

    private static void createTestDsmUser() {
        user = new User(null, "AddFamilyMemberUser", "addfamilymember@family.com");
        user.setId(String.valueOf(userDao.create(user)));
    }

    private static void createTestDataOfFamilyMember() {
        data.put("firstName", "Family");
        data.put("lastName", "Member");
        data.put("memberType", "Sister");
        data.put("familyId", "PE3LHB");
        data.put("collaboratorParticipantId", "PE3LHB_1_2");
    }


    @AfterClass
    public static void finish() {
        if (StringUtils.isNotBlank(ddpParticipantDataId)) {
            participantDataDao.delete(Integer.parseInt(ddpParticipantDataId));
        }
        userDao.delete(user.getUserId());
        ddpInstanceDao.delete(ddpInstanceDto.getDdpInstanceId());
    }

    @Test
    public void noGuidProvided() {
        String payload = payloadFactory(null, ddpInstanceDto.getInstanceName(), data, user.getUserId());
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getParticipantGuid().orElseThrow(() -> new NoSuchElementException("Participant Guid is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("Participant Guid is not provided", nsee.getMessage());
        }
    }

    @Test
    public void noRealmProvided() {
        String payload = payloadFactory(participantId, null, data, Integer.parseInt(user.getId()));
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getRealm().orElseThrow(() -> new NoSuchElementException("Realm is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("Realm is not provided", nsee.getMessage());
        }
    }

    @Test
    public void noFamilyMemberDataProvided(){
        String payload = payloadFactory(participantId, ddpInstanceDto.getInstanceName(), Map.of(), user.getUserId());
        Result res = new Result(200);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        if (addFamilyMemberPayload.getData().isEmpty() || addFamilyMemberPayload.getData().orElseGet(FamilyMemberDetails::new).isFamilyMemberFieldsEmpty()) {
            res = new Result(400, "Family member information is not provided");
        }
        Assert.assertEquals(400, res.getCode());
        Assert.assertEquals("Family member information is not provided", res.getBody());
    }

    @Test
    public void noUserIdProvided() {
        String payload = payloadFactory(participantId, ddpInstanceDto.getInstanceName(), Map.of(), null);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            addFamilyMemberPayload.getUserId().orElseThrow(() -> new NoSuchElementException("User id is not provided"));
        } catch (NoSuchElementException nsee) {
            Assert.assertEquals("User id is not provided", nsee.getMessage());
        }
    }

    @Test
    public void addFamilyMemberToParticipant() {
        String payload = payloadFactory(participantId, ddpInstanceDto.getInstanceName(), data, user.getUserId());
        Result result = new Result(200);
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);
        try {
            ddpParticipantDataId = ParticipantData.createNewParticipantData(
                    addFamilyMemberPayload.getParticipantGuid().get(),
                    String.valueOf(ddpInstanceDto.getDdpInstanceId()),
                    AddFamilyMemberRoute.FIELD_TYPE,
                    gson.toJson(addFamilyMemberPayload.getData().get()),
                    user.getEmail()
                    );
        } catch (Exception e) {
            result = new Result(500);
        }
        Assert.assertEquals(200, result.getCode());
    }

    @Test
    public void copyProbandDatatToFamilyMember() {
        String payload = payloadFactory(participantId, ddpInstanceDto.getInstanceName(), data, user.getUserId());
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(payload, AddFamilyMemberPayload.class);

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
        AddFamilyMemberPayload addFamilyMemberPayload = new AddFamilyMemberPayload(participantGuid, realm, familyMemberDetails, userId,
                false, null);

        return gson.toJson(addFamilyMemberPayload);
    }

}