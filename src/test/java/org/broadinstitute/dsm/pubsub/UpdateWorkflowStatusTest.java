package org.broadinstitute.dsm.pubsub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.util.DBTestUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.dsm.TestHelper.setupDB;

public class UpdateWorkflowStatusTest {

    public static final String UPDATE_WORKFLOW_TEST = "Update workflow test";

    private static Gson gson;

    private static final String participantId = "RBMJW6ZIXVXBMXUX6M3Q";

    private static User user;

    private static int participantDataId;

    private static ParticipantDataDto participantDataDto;

    private static final Map<String, String> participantData = new HashMap<>();

    private static DDPInstanceDto ddpInstanceDto;
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();

    private static final UserDao userDao = new UserDao();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();

    @BeforeClass
    public static void doFirst() {
        setupDB();
        gson = new Gson();

        ddpInstanceDto =  DBTestUtil.createTestDdpInstance(ddpInstanceDto, ddpInstanceDao, "UpdateWorkflowInstance");

        user = DBTestUtil.createTestDsmUser("UpdateWorkflowUser", "UpdateWorkflow@status.com", userDao, user);

        createDataForParticipant();

        createParticipantData();

    }

    @Test
    public void testUpdateProbandStatusInDB() {
        String workflow = "REGISTRATION_STATUS";
        String status = "ENROLLED";
        UpdateWorkflowStatusSubscription.updateProbandStatusInDB(workflow, status, participantDataDto, UpdateWorkflowStatusSubscription.RGP);
        String data = participantDataDao.get(participantDataId).orElseThrow().getData();
        JsonObject dataJsonObject = gson.fromJson(data, JsonObject.class);
        Assert.assertEquals(status, dataJsonObject.get(workflow).getAsString());
    }

    private static void createDataForParticipant() {
        participantData.put("REGISTRATION_STATUS", "REGISTERED");
        participantData.put("MEMBER_TYPE", "SELF");
    }

    private static void createParticipantData() {
        participantDataDto =
                new ParticipantDataDto(participantId, ddpInstanceDto.getDdpInstanceId(), UPDATE_WORKFLOW_TEST,
                        gson.toJson(participantData), System.currentTimeMillis(), user.getEmail());
        participantDataId = participantDataDao.create(participantDataDto);
        participantDataDto =
                new ParticipantDataDto(participantDataId, participantId, ddpInstanceDto.getDdpInstanceId(), UPDATE_WORKFLOW_TEST,
                        gson.toJson(participantData), System.currentTimeMillis(), user.getEmail());
    }

    @AfterClass
    public static void finish() {
        if (participantDataId > 0) {
            participantDataDao.delete(participantDataId);
        }
        userDao.delete(user.getUserId());
        ddpInstanceDao.delete(ddpInstanceDto.getDdpInstanceId());
    }
}
