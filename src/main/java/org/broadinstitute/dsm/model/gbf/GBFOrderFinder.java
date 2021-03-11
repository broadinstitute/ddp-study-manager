package org.broadinstitute.dsm.model.gbf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.ElasticSearchUtil;
import org.elasticsearch.client.RestHighLevelClient;

public class GBFOrderFinder {

    private final int maxOrdersToProcess;
    private final String esIndex;
    private final RestHighLevelClient esClient;
    private int maxDaysToReturnPreviousKit = Integer.MAX_VALUE;

    private static final String FIND_KITS_TO_ORDER_QUERY =
            "\n" +
                    "select distinct\n" +
                    "subkit.external_name,\n" +
                    "orders.external_order_number,\n" +
                    "orders.ddp_participant_id,\n" +
                    "(select max(req.dsm_kit_request_id) from ddp_kit_request req where req.external_order_number = orders.external_order_number) as max_kit_request_id,\n" +
                    "(select req.order_transmitted_at from ddp_kit_request req where req.dsm_kit_request_id = orders.external_order_number\n" +
                    "for update) as order_transmission_date\n" +
                    "from\n" +
                    "ddp_instance i,\n" +
                    "ddp_kit_request_settings s,\n" +
                    "sub_kits_settings subkit,\n" +
                    "(select req2.external_order_number, req2.ddp_participant_id,  req2.ddp_instance_id,\n" +
                    "      req2.kit_type_id, req2.dsm_kit_request_id\n" +
                    "      from ddp_kit_request req2,\n" +
                    "           ddp_kit k2,\n" +
                    "           ddp_instance i\n" +
                    "      where i.instance_name = ?\n" +
                    "        and req2.upload_reason is not null\n" +
                    "        and req2.ddp_instance_id = i.ddp_instance_id\n" +
                    "        and k2.dsm_kit_request_id = req2.dsm_kit_request_id\n" +
                    "-- any of: any kit for the ptp has been sent back, has a CE order or a result\n" +
                    "        and (k2.CE_order is null\n" +
                    "          or\n" +
                    "             k2.test_result is not null\n" +
                    "          or\n" +
                    "             k2.ups_return_status like 'D%'\n" +
                    "          or\n" +
                    "             k2.ups_return_status like 'I%'\n" +
                    "          )\n" +
                    "        and (\n" +
                    "          -- it's been at most n days since the kit was delivered\n" +
                    "          (k2.ups_tracking_status like 'D%'\n" +
                    "              and\n" +
                    "           DATE_ADD(str_to_date(k2.ups_tracking_date, '%Y%m%d %H%i%s'), INTERVAL ? DAY) > now())\n" +
                    "          )\n" +
                    "        and\n" +
                    "        req2.order_transmitted_at is null\n" +
                    "\n" +
                    "      union\n" +
                    "\n" +
                    "      select distinct req.external_order_number, req.ddp_participant_id, req.ddp_instance_id,\n" +
                    "      req.kit_type_id, req.dsm_kit_request_id\n" +
                    "      from ddp_kit_request req, ddp_instance i\n" +
                    "      where \n" +
                    "      i.instance_name = ?\n" +
                    "      and req.upload_reason is null\n" +
                    "      and req.order_transmitted_at is null\n" +
                    "      and req.ddp_instance_id = i.ddp_instance_id\n" +
                    "        and 1 = (select count(distinct req2.external_order_number)\n" +
                    "                 from ddp_kit_request req2\n" +
                    "                 where req.ddp_participant_id = req2.ddp_participant_id\n" +
                    "                   and req.ddp_instance_id = req2.ddp_instance_id)\n" +
                    // todo arz need to join through to subkit type id?  yes
                    "     ) as orders\n" +
                    "where\n" +
                    "i.instance_name = ?\n" +
                    "and\n" +
                    "i.ddp_instance_id = s.ddp_instance_id\n" +
                    "and\n" +
                    "subkit.ddp_kit_request_settings_id = s.ddp_kit_request_settings_id\n" +
                    "and\n" +
                    "subkit.kit_type_id = orders.kit_type_id\n" +
                    "order by max_kit_request_id asc limit ?\n";

    public GBFOrderFinder(Integer maxDaysToReturnPreviousKit,
                          int maxOrdersToProcess,
                          RestHighLevelClient esClient,
                          String esIndex) {
        if (maxDaysToReturnPreviousKit != null) {
            this.maxDaysToReturnPreviousKit = maxDaysToReturnPreviousKit;
        }
        this.maxOrdersToProcess = maxOrdersToProcess;
        this.esClient = esClient;
        this.esIndex = esIndex;
    }

    public Collection<SimpleKitOrder> findKitsToOrder(String ddpInstanceName, Connection conn) {
        Set<String> participantGuids = new HashSet<>();
        List<SimpleKitOrder> kitsToOrder = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(FIND_KITS_TO_ORDER_QUERY,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            stmt.setString(1, ddpInstanceName);
            stmt.setInt(2, maxDaysToReturnPreviousKit);
            stmt.setString(3, ddpInstanceName);
            stmt.setString(4, ddpInstanceName);
            stmt.setInt(5, maxOrdersToProcess);

            try (ResultSet rs = stmt.executeQuery()) {
                // loop through once to get participants
                while (rs.next()) {
                    participantGuids.add(rs.getString(DBConstants.DDP_PARTICIPANT_ID));
                }

                if (!participantGuids.isEmpty()) {
                    Map<String, Address> addressForParticipants = ElasticSearchUtil.getParticipantAddresses(esClient, esIndex, participantGuids);
                    // now iterate again to get address
                    while (rs.previous()) {
                        String participantGuid = rs.getString(DBConstants.DDP_PARTICIPANT_ID);
                        String externalOrderId = rs.getString(DBConstants.EXTERNAL_ORDER_NUMBER);
                        String kitName = rs.getString(DBConstants.EXTERNAL_KIT_NAME);
                        if (addressForParticipants.containsKey(participantGuid)) {
                            Address recipientAddress = addressForParticipants.get(participantGuid);
                            kitsToOrder.add(new SimpleKitOrder(recipientAddress, externalOrderId, kitName, participantGuid));
                        } else {

                        }
                    }
                } else {

                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error querying kits", e);
        }
        return kitsToOrder;
    }
}
