package org.broadinstitute.dsm.db;

import lombok.Data;
import lombok.NonNull;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

@Data
public class AbstractionActivity {

    private static final Logger logger = LoggerFactory.getLogger(AbstractionActivity.class);

    private static final String SQL_INSERT_MEDICAL_RECORD_ABSTRACTION_ACTIVITY = "INSERT INTO ddp_medical_record_abstraction_activities SET participant_id = (SELECT participant_id FROM ddp_participant pt, ddp_instance realm " +
            "WHERE realm.ddp_instance_id = pt.ddp_instance_id AND pt.ddp_participant_id = ? AND realm.instance_name = ?), start_date = ?, user_id = ?, activity = ?, status = ?";
    private static final String SQL_UPDATE_MEDICAL_RECORD_ABSTRACTION_ACTIVITY = "UPDATE ddp_medical_record_abstraction_activities SET user_id = ?, activity = ?, status = ?, files_used = ?, last_changed = ? WHERE medical_record_abstraction_activities_id = ?";
    private static final String SQL_SELECT_ALL_MEDICAL_RECORD_ABSTRACTION_ACTIVITY = "SELECT medical_record_abstraction_activities_id, abst.participant_id, user.name, activity, status, start_date, files_used, abst.last_changed " +
            "FROM ddp_medical_record_abstraction_activities abst LEFT JOIN access_user user ON (user.user_id = abst.user_id) LEFT JOIN ddp_participant pt ON (abst.participant_id = pt.participant_id) " +
            "LEFT JOIN ddp_instance realm ON (realm.ddp_instance_id = pt.ddp_instance_id) WHERE realm.instance_name = ?";
    private static final String SQL_SELECT_MEDICAL_RECORD_ABSTRACTION_ACTIVITY = "SELECT medical_record_abstraction_activities_id, abst.participant_id, user.name, activity, status, start_date, files_used, abst.last_changed " +
            "FROM ddp_medical_record_abstraction_activities abst LEFT JOIN access_user user ON (user.user_id = abst.user_id) LEFT JOIN ddp_participant pt ON (abst.participant_id = pt.participant_id) " +
            "LEFT JOIN ddp_instance realm ON (realm.ddp_instance_id = pt.ddp_instance_id) WHERE realm.instance_name = ? AND pt.ddp_participant_id = ?";
    private static final String SQL_SELECT_AND_WHERE_ACTIVITY = " AND activity = ?";

    private Integer medicalRecordAbstractionActivityId;
    private String participantId;
    private String activity;
    private String status;
    private String user;
    private Long startDate;
    private String filesUsed;
    private Long lastChanged;

    public AbstractionActivity(Integer medicalRecordAbstractionActivityId, String participantId, String activity, String status, String user, Long startDate, String filesUsed, Long lastChanged) {
        this.medicalRecordAbstractionActivityId = medicalRecordAbstractionActivityId;
        this.participantId = participantId;
        this.activity = activity;
        this.status = status;
        this.user = user;
        this.startDate = startDate;
        this.filesUsed = filesUsed;
        this.lastChanged = lastChanged;
    }

    public static AbstractionActivity startAbstractionActivity(@NonNull String participantId, @NonNull String realm, @NonNull Integer changedBy, @NonNull String activity, @NonNull String status) {
        Long startDate = System.currentTimeMillis();
        User user = User.getUser(changedBy);
        AbstractionActivity abstractionActivity = new AbstractionActivity(null, participantId, activity, status, user.getName(), startDate, null, null);
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_MEDICAL_RECORD_ABSTRACTION_ACTIVITY, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, participantId);
                stmt.setString(2, realm);
                stmt.setLong(3, startDate);
                stmt.setInt(4, changedBy);
                stmt.setString(5, activity);
                stmt.setString(6, status);
                int result = stmt.executeUpdate();
                if (result == 1) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            dbVals.resultValue = rs.getInt(1);
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Error adding new medical record abstraction activity ", e);
                    }
                }
                else {
                    throw new RuntimeException("Error adding new medical record abstraction activity for participant w/ id " + participantId + " it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error adding new medical record abstraction activity for participantId w/ id " + participantId, results.resultException);
        }
        else {
            abstractionActivity.setMedicalRecordAbstractionActivityId((int) results.resultValue);
            return abstractionActivity;
        }
    }

    public static AbstractionActivity changeAbstractionActivity(@NonNull AbstractionActivity abstractionActivity, @NonNull Integer changedBy, @NonNull String status) {
        Long currentDate = System.currentTimeMillis();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_MEDICAL_RECORD_ABSTRACTION_ACTIVITY)) {
                stmt.setInt(1, changedBy);
                stmt.setString(2, abstractionActivity.getActivity());
                stmt.setString(3, status);
                stmt.setString(4, abstractionActivity.getFilesUsed());
                stmt.setLong(5, currentDate);
                stmt.setInt(6, abstractionActivity.medicalRecordAbstractionActivityId);
                int result = stmt.executeUpdate();
                if (result != 1) {
                    throw new RuntimeException("Error updating status of medical record abstraction activity for participant w/ id " + abstractionActivity.getParticipantId() + " it was updating " + result + " rows");
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error adding new medical record abstraction activity for participantId w/ id " + abstractionActivity.getParticipantId(), results.resultException);
        }
        abstractionActivity.setStatus(status);
        return abstractionActivity;
    }

    public static HashMap<String, List<AbstractionActivity>> getAllAbstractionActivityByRealm(@NonNull String realm) {
        logger.info("Collection abstraction activity information");
        HashMap<String, List<AbstractionActivity>> abstractionActivitiesMap = new HashMap<>();

        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL_MEDICAL_RECORD_ABSTRACTION_ACTIVITY)) {
                stmt.setString(1, realm);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String participantId = rs.getString(DBConstants.PARTICIPANT_ID);
                        AbstractionActivity abstractionActivity = new AbstractionActivity(rs.getInt(DBConstants.MEDICAL_RECORD_ABSTRACTION_ACTIVITY_ID),
                                participantId,
                                rs.getString(DBConstants.ACTIVITY),
                                rs.getString(DBConstants.STATUS),
                                rs.getString(DBConstants.NAME),
                                rs.getLong(DBConstants.START_DATE),
                                rs.getString(DBConstants.FILES_USED),
                                rs.getLong(DBConstants.LAST_CHANGED)
                        );
                        if (abstractionActivitiesMap.containsKey(participantId)){
                            List<AbstractionActivity> abstractionActivities = abstractionActivitiesMap.get(participantId);
                            abstractionActivities.add(abstractionActivity);
                        }
                        else {
                            List<AbstractionActivity> abstractionActivities = new ArrayList<>();
                            abstractionActivities.add(abstractionActivity);
                            abstractionActivitiesMap.put(participantId, abstractionActivities);
                        }
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of abstraction activities for " + realm, results.resultException);
        }
        logger.info("Got " + abstractionActivitiesMap.size() + " participants abstraction acticity in DSM DB for " + realm);
        return abstractionActivitiesMap;
    }

    public static List<AbstractionActivity> getAbstractionActivity(@NonNull String realm, @NonNull String ddpParticipantId) {
        List<AbstractionActivity> activities = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MEDICAL_RECORD_ABSTRACTION_ACTIVITY)) {
                stmt.setString(1, realm);
                stmt.setString(2, ddpParticipantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String participantId = rs.getString(DBConstants.PARTICIPANT_ID);
                        activities.add(new AbstractionActivity(rs.getInt(DBConstants.MEDICAL_RECORD_ABSTRACTION_ACTIVITY_ID),
                                participantId,
                                rs.getString(DBConstants.ACTIVITY),
                                rs.getString(DBConstants.STATUS),
                                rs.getString(DBConstants.NAME),
                                rs.getLong(DBConstants.START_DATE),
                                rs.getString(DBConstants.FILES_USED),
                                rs.getLong(DBConstants.LAST_CHANGED)
                        ));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of abstraction activities for " + realm, results.resultException);
        }
        return activities;
    }

    public static AbstractionActivity getAbstractionActivity(@NonNull String realm, @NonNull String ddpParticipantId, @NonNull String activity) {
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_MEDICAL_RECORD_ABSTRACTION_ACTIVITY + SQL_SELECT_AND_WHERE_ACTIVITY)) {
                stmt.setString(1, realm);
                stmt.setString(2, ddpParticipantId);
                stmt.setString(3, activity);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String participantId = rs.getString(DBConstants.PARTICIPANT_ID);
                        dbVals.resultValue = new AbstractionActivity(rs.getInt(DBConstants.MEDICAL_RECORD_ABSTRACTION_ACTIVITY_ID),
                                participantId,
                                rs.getString(DBConstants.ACTIVITY),
                                rs.getString(DBConstants.STATUS),
                                rs.getString(DBConstants.NAME),
                                rs.getLong(DBConstants.START_DATE),
                                rs.getString(DBConstants.FILES_USED),
                                rs.getLong(DBConstants.LAST_CHANGED));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error getting list of abstraction activities for " + realm, results.resultException);
        }
        return (AbstractionActivity) results.resultValue;
    }
}
