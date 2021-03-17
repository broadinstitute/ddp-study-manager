package org.broadinstitute.dsm.cf;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import lombok.NonNull;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.DdpKit;
import org.broadinstitute.dsm.jobs.PubsubMessage;
import org.broadinstitute.dsm.model.ups.UPSStatus;
import org.broadinstitute.dsm.pubsub.KitTrackerPubSubPublisher;
import org.broadinstitute.dsm.statics.DBConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Identifies kits whose shipping history should be
 * queried (by another job) in order to produce
 * reports on turnaround time and study adherence.
 */
public class TestBostonKitTrackerDispatcher implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = LoggerFactory.getLogger(TestBostonKitTrackerDispatcher.class.getName());

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private int LOOKUP_CHUNK_SIZE;
    KitTrackerPubSubPublisher kitTrackerPubSubPublisher = new KitTrackerPubSubPublisher();

    @Override
    public void accept(PubsubMessage pubsubMessage, Context context) throws Exception {
        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString("dsmDBUrl");
        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(20, dbUrl);
        final String SQL_SELECT_KITS = "SELECT kit.dsm_kit_request_id,req.ddp_instance_id, kit.kit_label, kit.tracking_to_id, kit.tracking_return_id, kit.error, kit.message, kit.receive_date, " +
                "kth.kit_shipping_history,  kth.kit_return_history,  req.bsp_collaborator_participant_id, kit.ups_tracking_status, kit.ups_return_status, kit.ups_tracking_date, kit.ups_return_date, " +
                " req.external_order_number, kit.CE_order FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit kit LEFT JOIN " + STUDY_MANAGER_SCHEMA + "ddp_kit_request req " +
                " ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) " +
                "left join " + STUDY_MANAGER_SCHEMA + "kit_tracking_history kth on ( kth.dsm_kit_request_id = kit.dsm_kit_request_id) " +
                " WHERE req.ddp_instance_id = ? and kit_label not like \"%\\\\_1\" and kit.dsm_kit_request_id > ? ";
        String SQL_AVOID_DELIVERED = " and (tracking_to_id is not null or tracking_return_id is not null ) and (kth.kit_shipping_history is null or kth.kit_return_history is null or kth.kit_shipping_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\" or kth.kit_return_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\")" +
                " order by kit.dsm_kit_request_id ASC LIMIT ?";
        logger.info("Starting the UPS lookup job");
        LOOKUP_CHUNK_SIZE = new JsonParser().parse(pubsubMessage.getData()).getAsJsonObject().get("size").getAsInt();
        logger.info("The chunk size for each cloud function is " + LOOKUP_CHUNK_SIZE);
        JSONArray subsetOfKits = new JSONArray();
        String project = cfg.getString("pubsub.projectId");
        String topicId = cfg.getString("pubsub.topicId");
        try (Connection conn = dataSource.getConnection()) {
            List<DDPInstance> ddpInstanceList = getDDPInstanceListWithRole(conn, DBConstants.UPS_TRACKING_ROLE);
            for (DDPInstance ddpInstance : ddpInstanceList) {
                if (ddpInstance != null && ddpInstance.isHasRole()) {
                    int lastKitId = 0;
                    DdpKit kit = null;
                    int count = 0;
                    logger.info("tracking ups ids for " + ddpInstance.getName());
                    while (!(kit == null && count != 0)) {
                        count++;
                        try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS + SQL_AVOID_DELIVERED)) {
                            stmt.setString(1, ddpInstance.getDdpInstanceId());
                            stmt.setInt(2, lastKitId);
                            stmt.setInt(3, LOOKUP_CHUNK_SIZE);
                            subsetOfKits = new JSONArray();
                            stmt.setFetchSize(LOOKUP_CHUNK_SIZE);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    kit = new DdpKit(
                                            rs.getString(DBConstants.DSM_KIT_REQUEST_ID),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.KIT_LABEL),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_TRACKING_TO),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.TRACKING_RETURN_ID),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.ERROR),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.MESSAGE),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_RECEIVE_DATE),
                                            rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.COLLABORATOR_PARTICIPANT_ID),
                                            rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.EXTERNAL_ORDER_NUMBER),
                                            rs.getBoolean(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.CE_ORDER),
                                            rs.getString(DBConstants.KIT_TRACKING_HISTORY_TABLE_ABBR + DBConstants.KIT_SHIPPING_HISTORY),
                                            rs.getString(DBConstants.KIT_TRACKING_HISTORY_TABLE_ABBR + DBConstants.KIT_RETURN_HISTORY),
                                            rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.DDP_INSTANCE_ID),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.UPS_TRACKING_STATUS),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.UPS_TRACKING_DATE),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.UPS_RETURN_STATUS),
                                            rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.UPS_RETURN_DATE)
                                    );
                                    JSONObject jsonKit = new JSONObject(kit);
                                    subsetOfKits.put(jsonKit);
                                    logger.info("added " + kit.getKitLabel());
                                }
                            }
                            catch (Exception e) {
                                logger.error("Trouble executing select query ", e);
                            }
                        }
                        catch (Exception e) {
                            logger.error("Trouble creating the statement ", e);
                        }
                        lastKitId = Integer.parseInt(kit.getDsmKitRequestId());
                        kit = null;
                    }
                }
            }
            kitTrackerPubSubPublisher.publishMessage(project, topicId, subsetOfKits.toString());
        }
        catch (Exception e) {
            logger.error("Trouble creating a connection to DB ", e);
        }

    }

    public List<DDPInstance> getDDPInstanceListWithRole(Connection conn, @NonNull String role) {
        final String SQL_SELECT_INSTANCE_WITH_ROLE = "SELECT ddp_instance_id, instance_name, base_url, collaborator_id_prefix, migrated_ddp, billing_reference, " +
                "es_participant_index, es_activity_definition_index,  es_users_index, (SELECT count(role.name) " +
                "FROM " + STUDY_MANAGER_SCHEMA + "ddp_instance realm, " + STUDY_MANAGER_SCHEMA + "ddp_instance_role inRol, " + STUDY_MANAGER_SCHEMA + "instance_role role WHERE realm.ddp_instance_id = inRol.ddp_instance_id AND inRol.instance_role_id = role.instance_role_id AND role.name = ? " +
                "AND realm.ddp_instance_id = main.ddp_instance_id) AS 'has_role', mr_attention_flag_d, tissue_attention_flag_d, auth0_token, notification_recipients FROM  " + STUDY_MANAGER_SCHEMA + "ddp_instance main " +
                "WHERE is_active = 1";

        List<DDPInstance> ddpInstances = new ArrayList<>();
        try (PreparedStatement bspStatement = conn.prepareStatement(SQL_SELECT_INSTANCE_WITH_ROLE)) {
            bspStatement.setString(1, role);
            try (ResultSet rs = bspStatement.executeQuery()) {
                while (rs.next()) {
                    DDPInstance ddpInstance = DDPInstance.getDDPInstanceWithRoleFormResultSet(rs);
                    ddpInstances.add(ddpInstance);
                }
            }
        }
        catch (SQLException ex) {
            throw new RuntimeException("Error looking ddpInstances ", ex);
        }

        return ddpInstances;
    }

}
