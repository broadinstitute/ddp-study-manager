package org.broadinstitute.dsm.cf;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.gson.Gson;
import com.google.pubsub.v1.PubsubMessage;
import com.typesafe.config.Config;
import lombok.NonNull;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.DdpKit;
import org.broadinstitute.dsm.model.ups.UPSStatus;
import org.broadinstitute.dsm.pubsub.KitTrackerPubSubPublisher;
import org.broadinstitute.dsm.statics.DBConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Identifies kits whose shipping history should be
 * queried (by another job) in order to produce
 * reports on turnaround time and study adherence.
 */
public class TestBostonKitTrackerDispatcher implements BackgroundFunction<PubsubMessage> {

    private static final Logger logger = Logger.getLogger(TestBostonKitTrackerDispatcher.class.getName());

    private String STUDY_MANAGER_SCHEMA = System.getenv("STUDY_MANAGER_SCHEMA") + ".";
    private int LOOKUP_CHUNK_SIZE;


    KitTrackerPubSubPublisher kitTrackerPubSubPublisher = new KitTrackerPubSubPublisher();


    @Override
    public void accept(PubsubMessage pubsubMessage, Context context) throws Exception {
        Config cfg = CFUtil.loadConfig();
        String dbUrl = cfg.getString("dsmDBUrl");

        PoolingDataSource<PoolableConnection> dataSource = CFUtil.createDataSource(20, dbUrl);
        final String SQL_SELECT_KITS = "SELECT kit.dsm_kit_request_id,req.ddp_instance_id, kit.kit_label, kit.tracking_to_id, kit.tracking_return_id, kit.error, kit.message, kit.receive_date, " +
                "kth.kit_shipping_history,  kth.kit_return_history,  req.bsp_collaborator_participant_id, " +
                " req.external_order_number, kit.CE_order FROM " + STUDY_MANAGER_SCHEMA + "ddp_kit kit LEFT JOIN " + STUDY_MANAGER_SCHEMA + "ddp_kit_request req " +
                " ON (kit.dsm_kit_request_id = req.dsm_kit_request_id) " +
                "left join " + STUDY_MANAGER_SCHEMA + "kit_tracking_history kth on ( kth.dsm_kit_request_id = kit.dsm_kit_request_id) " +
                " WHERE req.ddp_instance_id = ? and kit_label not like \"%\\\\_1\"  ";
        String SQL_AVOID_DELIVERED = " and (tracking_to_id is not null or tracking_return_id is not null ) and (kit_shipping_history is null or kit_return_history is null or kit_shipping_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\" or kit_return_history not like \"" + UPSStatus.DELIVERED_TYPE + " %\")" +
                " order by kit.dsm_kit_request_id ASC";

        logger.info("Starting the UPS lookup job");
        LOOKUP_CHUNK_SIZE = cfg.getInt("kitTracking.trackingPerCFSize"); // todo pegah put in constants
        ArrayList<DdpKit> subsetOfKits = new ArrayList<>();
        // static vars are dangerous in CF https://cloud.google.com/functions/docs/bestpractices/tips#functions-tips-scopes-java

        try (Connection conn = dataSource.getConnection()) {
            List<DDPInstance> ddpInstanceList = getDDPInstanceListWithRole(conn, "ups_tracking");
            for (DDPInstance ddpInstance : ddpInstanceList) {
                if (ddpInstance != null && ddpInstance.isHasRole()) {
                    logger.info("tracking ups ids for " + ddpInstance.getName());


                    try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KITS + SQL_AVOID_DELIVERED)) {
                        stmt.setString(1, ddpInstance.getDdpInstanceId());
                        stmt.setFetchSize(LOOKUP_CHUNK_SIZE);
                        try (ResultSet rs = stmt.executeQuery()) {
                            int count = 0;

                            while (rs.next() && count < LOOKUP_CHUNK_SIZE) {
                                count++;
                                String kitLabel = rs.getString("kit.kit_label");
                                logger.info(kitLabel);

                                DdpKit kit = new DdpKit(
                                        rs.getString(DBConstants.DSM_KIT_REQUEST_ID),
                                        rs.getString("kit." + DBConstants.KIT_LABEL),
                                        rs.getString("kit." + DBConstants.DSM_TRACKING_TO),
                                        rs.getString("kit." + DBConstants.TRACKING_RETURN_ID),
                                        rs.getString("kit." + DBConstants.ERROR),
                                        rs.getString("kit." + DBConstants.MESSAGE),
                                        rs.getString("kit." + DBConstants.DSM_RECEIVE_DATE),
                                        rs.getString("req." + DBConstants.COLLABORATOR_PARTICIPANT_ID),
                                        rs.getString("req." + DBConstants.EXTERNAL_ORDER_NUMBER),
                                        rs.getBoolean("kit." + DBConstants.CE_ORDER),
                                        rs.getString("kth." + DBConstants.KIT_SHIPPING_HISTORY),
                                        rs.getString("kth." + DBConstants.KIT_RETURN_HISTORY),
                                        rs.getString("req." + DBConstants.DDP_INSTANCE_ID),
                                        rs.getString("kit." + DBConstants.UPS_TRACKING_STATUS),
                                        rs.getString("kit." + DBConstants.UPS_TRACKING_DATE),
                                        rs.getString("kit." + DBConstants.UPS_RETURN_STATUS),
                                        rs.getString("kit." + DBConstants.UPS_RETURN_DATE)
                                );
                                subsetOfKits.add(kit);
                                logger.info("added " + kitLabel);

                            }
                        }
                    }
                    conn.close();
                }



            }
            String json = new Gson().toJson(subsetOfKits.toArray(), DdpKit[].class);
            logger.info(json);
            kitTrackerPubSubPublisher.publishWithErrorHandlerExample("broad-ddp-dev", "cf-kit-tracking", json);
        }
        catch(Exception e){
            logger.log(Level.SEVERE, "Trouble querying data", e);
        }

    }

    public List<DDPInstance> getDDPInstanceListWithRole(Connection conn, @NonNull String role) {
        final String SQL_SELECT_INSTANCE_WITH_ROLE = "SELECT ddp_instance_id, instance_name, base_url, collaborator_id_prefix, migrated_ddp, billing_reference, " +
                "es_participant_index, es_activity_definition_index,  es_users_index, carrier_tracking_url, (SELECT count(role.name) " +
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
