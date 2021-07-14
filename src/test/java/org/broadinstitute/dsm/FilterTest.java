package org.broadinstitute.dsm;

import com.google.gson.Gson;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dao.ddp.participant.ParticipantDataDao;
import org.broadinstitute.dsm.db.dao.user.UserDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.db.dto.ddp.participant.ParticipantDataDto;
import org.broadinstitute.dsm.model.Filter;
import org.broadinstitute.dsm.model.NameValue;
import org.broadinstitute.dsm.route.FilterRoute;
import org.broadinstitute.dsm.util.DBTestUtil;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.junit.AfterClass;
import org.junit.Assert;
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

    private static int participantDataId1;

    private static final Map<String, String> participantData1 = new HashMap<>();

    private static DDPInstanceDto ddpInstanceDto;
    private static final DDPInstanceDao ddpInstanceDao = new DDPInstanceDao();

    private static final UserDao userDao = new UserDao();

    private static final ParticipantDataDao participantDataDao = new ParticipantDataDao();

    @BeforeClass
    public static void doFirst() {
        setupDB();
        gson = new Gson();

        ddpInstanceDto =  DBTestUtil.createTestDdpInstance(ddpInstanceDto, ddpInstanceDao, "FilterTestInstance");

        user = DBTestUtil.createTestDsmUser("testFilterUser", "testFilter@family.com", userDao, user);

        createDataForParticipant();

        createParticipantData();

    }

    private static void createDataForParticipant() {
        participantData.put("PARTICIPANT_DEATH_DATE", "2040-10-30");
        participantData1.put("PARTICIPANT_DEATH_DATE", "2040-10-30");
    }

    private static void createParticipantData() {
        ParticipantDataDto participantDataDto =
                new ParticipantDataDto.Builder()
                    .withDdpParticipantId(participantId)
                    .withDdpInstanceId(ddpInstanceDto.getDdpInstanceId())
                    .withFieldTypeId(FILTER_TEST)
                    .withData(gson.toJson(participantData))
                    .withLastChanged(System.currentTimeMillis())
                    .withChangedBy(user.getEmail())
                    .build();
        participantDataId = participantDataDao.create(participantDataDto);

        ParticipantDataDto participantDataDto1 =
                new ParticipantDataDto.Builder()
                    .withDdpParticipantId(participantId)
                    .withDdpInstanceId(ddpInstanceDto.getDdpInstanceId())
                    .withFieldTypeId(FILTER_TEST)
                    .withData(gson.toJson(participantData1))
                    .withLastChanged(System.currentTimeMillis())
                    .withChangedBy(user.getEmail())
                    .build();
        participantDataId1 = participantDataDao.create(participantDataDto1);
    }

    @Test
    public void testAddOneParticipantDataFilter() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-30"), null, null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("null AND (profile.guid = " + participantId + " OR profile.guid = " + participantId + ")",
                queryConditions.get(ElasticSearchUtil.ES));
    }

    @Test
    public void testAddOneParticipantDataFilterNotEmpty() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, false, true, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-35"), null, null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("null AND (profile.guid = " + participantId + " OR profile.guid = " + participantId + ")",
                queryConditions.get(ElasticSearchUtil.ES));
    }

    @Test
    public void testAddOneParticipantDataFilterEmpty() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, true, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_NONEXISTENT_DATE", "2040-10-35"), null, null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("null AND (profile.guid = " + participantId + " OR profile.guid = " + participantId + ")",
                queryConditions.get(ElasticSearchUtil.ES));
    }

    @Test
    public void testAddOneParticipantDataWithRange() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-29"),
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-31"), null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("null AND (profile.guid = " + participantId + " OR profile.guid = " + participantId + ")",
                queryConditions.get(ElasticSearchUtil.ES));
    }

    @Test
    public void testAddOneParticipantDataWithRangeFalse() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filter = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2020-10-29"),
                new NameValue("PARTICIPANT_DEATH_DATE", "2020-10-31"), null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filter, filter.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("profile.guid = empty", queryConditions.get(ElasticSearchUtil.ES));
    }

    @Test
    public void testAddSeveralParticipantDataFilter() {
        Map<String, String> queryConditions = new HashMap<>();
        Filter filterA = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-30"), null, null, null);
        Filter filterB = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-30"), null, null, null);
        Filter filterC = new Filter(false, true, false, false, "DATE", "participantData",
                new NameValue("PARTICIPANT_DEATH_DATE", "2040-10-30"), null, null, null);
        List<ParticipantDataDto> allParticipantData = participantDataDao
                .getParticipantDataByInstanceid(Integer.parseInt(String.valueOf(ddpInstanceDto.getDdpInstanceId())));
        FilterRoute.addParticipantDataFilters(queryConditions, filterA, filterA.getFilter1().getName(), allParticipantData);
        FilterRoute.addParticipantDataFilters(queryConditions, filterB, filterB.getFilter1().getName(), allParticipantData);
        FilterRoute.addParticipantDataFilters(queryConditions, filterC, filterC.getFilter1().getName(), allParticipantData);
        Assert.assertEquals("null AND (profile.guid = RBMJW6ZIXVXBMXUX6M3Q" + " OR profile.guid = " + participantId + ")" +
                        " AND (profile.guid = RBMJW6ZIXVXBMXUX6M3Q" + " OR profile.guid = " + participantId + ")" +
                        " AND (profile.guid = RBMJW6ZIXVXBMXUX6M3Q" + " OR profile.guid = " + participantId + ")",
                queryConditions.get(ElasticSearchUtil.ES));
    }

    @AfterClass
    public static void finish() {
        if (participantDataId > 0) {
            participantDataDao.delete(participantDataId);
        }
        if (participantDataId1 > 0) {
            participantDataDao.delete(participantDataId1);
        }
        userDao.delete(user.getUserId());
        ddpInstanceDao.delete(ddpInstanceDto.getDdpInstanceId());
    }
}
