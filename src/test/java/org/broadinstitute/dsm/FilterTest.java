package org.broadinstitute.dsm;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.participant.data.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.db.dto.participant.data.ParticipantDataDto;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.route.FilterRoute;
import org.broadinstitute.dsm.util.DBTestUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.dsm.TestHelper.setupDB;

public class FilterTest {
    private static final String FILTER_TEST = "FILTER_TEST";
    private static Gson gson;

    private static final String participantId = "RBMJW6ZIXVXBMXUX6M3Q";

    private static User user;

    private static int participantDataId;

    private static final Map<String, String> participantData = new HashMap<>();

    private static DDPInstanceDto ddpInstanceDto;
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();

    private static final UserDao userDao = new UserDao();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();

    @BeforeClass
    public static void doFirst() {
        setupDB();
        gson = new Gson();

        DBTestUtil.createTestDdpInstance(ddpInstanceDto, ddpInstanceDao, "AddFamilyMemberInstance");

        DBTestUtil.createTestDsmUser("testFilterUser", "testFilter@family.com", userDao, user);

        createDataForParticipant();

        createParticipantData();

    }

    private static void createDataForParticipant() {
        participantData.put("PARTICIPANT_DEATH_DATE", "2040-10-30");
    }

    private static void createParticipantData() {
        ParticipantDataDto participantDataDto =
                new ParticipantDataDto(participantId, ddpInstanceDto.getDdpInstanceId(), FILTER_TEST,
                        gson.toJson(participantData), System.currentTimeMillis(), user.getEmail());
        participantDataId = participantDataDao.create(participantDataDto);
    }

    @Test
    public void testAddParticipantDataFilters() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-30"), null, null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);

    }
}
