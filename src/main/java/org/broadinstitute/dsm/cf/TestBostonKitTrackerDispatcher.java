package org.broadinstitute.dsm.cf;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.JsonParser;
import com.typesafe.config.Config;
import lombok.NonNull;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.jobs.PubsubMessage;
import org.broadinstitute.dsm.model.UPSKit;
import org.broadinstitute.dsm.model.ups.UPSActivity;
import org.broadinstitute.dsm.model.ups.UPSPackage;
import org.broadinstitute.dsm.model.ups.UPSStatus;
import org.broadinstitute.dsm.pubsub.KitTrackerPubSubPublisher;
import org.broadinstitute.dsm.statics.DBConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.utils.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
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
        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(2, dbUrl);
        String data = new String(Base64.getDecoder().decode(pubsubMessage.getData()));
        final String SQL_SELECT_KITS_WITH_LATEST_ACTIVITY = "SELECT  * " +
                " FROM  " + STUDY_MANAGER_SCHEMA + "ddp_kit kit  " +
                " LEFT JOIN     " + STUDY_MANAGER_SCHEMA + "ddp_kit_request req  ON (kit.dsm_kit_request_id = req.dsm_kit_request_id)   " +
                " left join    " + STUDY_MANAGER_SCHEMA + "ups_shipment shipment on (shipment.dsm_kit_request_id = kit.dsm_kit_request_id) " +
                " left join  " + STUDY_MANAGER_SCHEMA + "ups_package pack on (pack .dsm_kit_request_id = kit.dsm_kit_request_id and pack.ups_shipment_id = shipment.ups_shipment_id) " +
                " left join  " + STUDY_MANAGER_SCHEMA + "ups_activity activity on (activity.dsm_kit_request_id = kit.dsm_kit_request_id and pack.ups_package_id = activity.ups_package_id) " +
                " WHERE req.ddp_instance_id = ? and ( kit_label not like \"%\\\\_1\") and kit.dsm_kit_request_id > ? " +
                " and (shipment.ups_shipment_id is null or  activity.ups_activity_id in  " +
                " ( SELECT ac.ups_activity_id  " +
                " FROM ups_package pac INNER JOIN  " +
                "    ( SELECT  ups_package_id, MAX(ups_activity_id) maxId  " +
                "    FROM ups_activity  " +
                "    GROUP BY ups_package_id  ) lastActivity ON pac.ups_package_id = lastActivity.ups_package_id INNER JOIN  " +
                "    ups_activity ac ON   lastActivity.ups_package_id = ac.ups_package_id  " +
                "    AND lastActivity.maxId = ac.ups_activity_id  " +
                " ))";
        String SQL_AVOID_DELIVERED = " and (tracking_to_id is not null or tracking_return_id is not null ) and  pack.delivery_date is null" +
                " order by kit.dsm_kit_request_id ASC LIMIT ?";
        logger.info("Starting the UPS lookup job");
        logger.info(data);
        LOOKUP_CHUNK_SIZE = new JsonParser().parse(data).getAsJsonObject().get("size").getAsInt();
        logger.info("The chunk size for each cloud function is " + LOOKUP_CHUNK_SIZE);
        JSONArray subsetOfKits = new JSONArray();
        String project = cfg.getString("pubsub.projectId");
        String topicId = cfg.getString("pubsub.topicId");
        try (Connection conn = dataSource.getConnection()) {
            List<DDPInstance> ddpInstanceList = getDDPInstanceListWithRole(conn, DBConstants.UPS_TRACKING_ROLE);
            for (DDPInstance ddpInstance : ddpInstanceList) {
                if (ddpInstance != null && ddpInstance.isHasRole()) {
                    int lastKitId = 0;
                    UPSKit kit = null;
                    int count = 0;
                    logger.info("tracking ups ids for " + ddpInstance.getName());
                    while (!(kit == null && count != 0)) {
                        count++;
                        try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS_WITH_LATEST_ACTIVITY + SQL_AVOID_DELIVERED)) {
                            stmt.setString(1, ddpInstance.getDdpInstanceId());
                            stmt.setInt(2, lastKitId);
                            stmt.setInt(3, LOOKUP_CHUNK_SIZE);
                            subsetOfKits = new JSONArray();
                            stmt.setFetchSize(LOOKUP_CHUNK_SIZE);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    logger.info(kit == null ? "null" : kit.getDsmKitRequestId());
                                    if (kit == null) {
                                        String shipmentId = rs.getString("ups_shipment_id");
                                        UPSPackage upsPackage;
                                        if (StringUtils.isNotBlank(shipmentId)) {
                                            UPSStatus latestStatus = new UPSStatus(rs.getString("activity.ups_status_type"),
                                                    rs.getString("activity.ups_status_description"),
                                                    rs.getString("activity.ups_status_code"));
                                            UPSActivity packageLastActivity = new UPSActivity(
                                                    rs.getString("activity.ups_location"),
                                                    latestStatus,
                                                    rs.getString("activity.ups_activity_date"),
                                                    rs.getString("activity.ups_activity_time"),
                                                    rs.getString("activity.ups_activity_id"),
                                                    rs.getString("activity.ups_package_id"),
                                                    rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_KIT_REQUEST_ID)
                                            );
                                            upsPackage = new UPSPackage(
                                                    rs.getString("pack.tracking_number"),
                                                    new UPSActivity[] { packageLastActivity },
                                                    rs.getString("pack.ups_shipment_id"),
                                                    rs.getString("pack.ups_package_id"),
                                                    rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_KIT_REQUEST_ID),
                                                    null, null);
                                        }
                                        else {
                                            upsPackage = new UPSPackage(
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_KIT_REQUEST_ID),
                                                    null, null);
                                        }
                                        kit = new UPSKit(upsPackage,
                                                rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.KIT_LABEL),
                                                rs.getBoolean(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.CE_ORDER),
                                                rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_KIT_REQUEST_ID),
                                                rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.EXTERNAL_ORDER_NUMBER),
                                                rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_TRACKING_TO),
                                                rs.getString(DBConstants.DDP_KIT_TABLE_ABBR + DBConstants.DSM_TRACKING_RETURN),
                                                rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.DDP_INSTANCE_ID),
                                                rs.getString(DBConstants.DDP_KIT_REQUEST_TABLE_ABBR + DBConstants.BSP_COLLABORATOR_PARTICIPANT_ID)
                                        );
                                    }
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
