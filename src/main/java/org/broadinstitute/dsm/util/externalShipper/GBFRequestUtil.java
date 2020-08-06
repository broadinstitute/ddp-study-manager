package org.broadinstitute.dsm.util.externalShipper;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Executor;
import org.broadinstitute.ddp.db.SimpleResult;
import org.broadinstitute.ddp.util.Utility;
import org.broadinstitute.dsm.DSMServer;
import org.broadinstitute.dsm.exception.ExternalShipperException;
import org.broadinstitute.dsm.model.*;
import org.broadinstitute.dsm.model.gbf.*;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.util.EasyPostUtil;
import org.broadinstitute.dsm.util.SecurityUtil;
import org.broadinstitute.dsm.util.SystemUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.ddp.db.TransactionWrapper.inTransaction;

public class GBFRequestUtil implements ExternalShipper {

    private static final Logger logger = LoggerFactory.getLogger(GBFRequestUtil.class);

    public static final String SQL_SELECT_EXTERNAL_KIT_NOT_DONE = "SELECT * FROM (SELECT kt.kit_type_name, ddp_site.instance_name, ddp_site.ddp_instance_id, ddp_site.base_url, ddp_site.auth0_token, ddp_site.migrated_ddp, " +
            "ddp_site.collaborator_id_prefix, req.bsp_collaborator_sample_id, req.ddp_participant_id, req.ddp_label, req.dsm_kit_request_id, req.bsp_collaborator_participant_id, " +
            "req.kit_type_id, req.external_order_status, req.external_order_number, req.external_response, kt.no_return, req.created_by, kit.kit_label FROM kit_type kt, ddp_kit_request req, ddp_kit kit, ddp_instance ddp_site " +
            "WHERE req.ddp_instance_id = ddp_site.ddp_instance_id AND req.kit_type_id = kt.kit_type_id AND kit.dsm_kit_request_id = req.dsm_kit_request_id) AS request " +
            "LEFT JOIN ddp_participant_exit ex ON (ex.ddp_instance_id = request.ddp_instance_id AND ex.ddp_participant_id = request.ddp_participant_id) " +
            "LEFT JOIN (SELECT subK.kit_type_id, subK.external_name from ddp_kit_request_settings dkc " +
            "LEFT JOIN sub_kits_settings subK ON (subK.ddp_kit_request_settings_id = dkc.ddp_kit_request_settings_id)) as subkits ON (subkits.kit_type_id = request.kit_type_id) " +
            "WHERE ex.ddp_participant_exit_id is null AND request.ddp_instance_id = ? AND NOT external_order_status <=> 'CANCELLED' AND (external_response IS NULL OR kit_label IS NULL)";
    private static final String SQL_SELECT_KIT_REQUEST_BY_EXTERNAL_ORDER_NUMBER = "SELECT dsm_kit_request_id FROM ddp_kit_request req WHERE external_order_number = ?";

    public static final String ORDER_ENDPOINT = "order";
    public static final String CONFIRM_ENDPOINT = "confirm";
    public static final String STATUS_ENDPOINT = "status";
    public final String CANCEL_ORDER_ENDPOINT = "cancelorder";

//    private final String ORDERED = "ORDERED";
    private final String NOT_FOUND = "NOT FOUND"; //INDICATES: we have no record of the order number
    private final String RECEIVED = "RECEIVED"; //INDICATES: the order number is at GBF but has not begun processing
    private final String CANCELLED = "CANCELLED"; //INDICATES: the order was cancelled either via API or customer service
    private final String PROCESSING = "PROCESSING"; //INDICATES: the order is in our back-end system and being fulfilled
    private final String FORMS_PRINTED = "FORMS PRINTED"; //N/A to Promise project
    private final String DISTRIBUTED = "DISTRIBUTION"; //INDICATES: the order is in the final stages of shipping / Can no longer be ‘Cancelled’
    private final String SHIPPED = "SHIPPED"; //INDICATES: the order has been shipped and is with the carrier

    private final String XML_NODE_EXPRESSION = "/ShippingConfirmations/ShippingConfirmation[@OrderNumber=\'%1\']";

    public final String EXTERNAL_SHIPPER_NAME = "gbf";

    private static int additionalAttempts = 1;
    private static int sleepInMs = 500;

    private static Executor blindTrustEverythingExecutor;

    public GBFRequestUtil() {
        try {
            if (blindTrustEverythingExecutor == null) {
                blindTrustEverythingExecutor = Executor.newInstance(SecurityUtil.buildHttpClient());
            }
        }
        catch (Exception e) {
            logger.error("Starting up the blindTrustEverythingExecutor ", e);
            System.exit(-3);
        }
    }

    public String getExternalShipperName() {
        return EXTERNAL_SHIPPER_NAME;
    }

    public void orderKitRequests(ArrayList<KitRequest> kitRequests, EasyPostUtil easyPostUtil, KitRequestSettings kitRequestSettings) throws Exception {
        if (kitRequests != null && !kitRequests.isEmpty()) {
            Orders orders = new Orders();
            orders.setOrders(new ArrayList<>());
            for (KitRequest kit : kitRequests) {
                Address address = null;
                // kit was uploaded
                if (kit instanceof KitUploadObject) {
                    com.easypost.model.Address participantAddress = easyPostUtil.getAddress(((KitUploadObject) kit).getEasyPostAddressId());
                    address = new Address(participantAddress.getName(),
                            participantAddress.getStreet1(), participantAddress.getStreet2(), participantAddress.getCity(),
                            participantAddress.getState(), participantAddress.getZip(), participantAddress.getCountry(),
                            participantAddress.getPhone());
                }
                // kit per ddp request
                else if (kit.getParticipant() != null) {
                    address = new Address(kit.getParticipant().getFirstName() + " " + kit.getParticipant().getLastName(),
                            kit.getParticipant().getStreet1(), kit.getParticipant().getStreet2(), kit.getParticipant().getCity(),
                            kit.getParticipant().getState(), kit.getParticipant().getPostalCode(), kit.getParticipant().getCountry(),
                            kitRequestSettings.getPhone());
                    //todo PEGAH TESTBOSTON verify address
                }
                if (address != null) {
                    ShippingInfo shippingInfo = new ShippingInfo(kitRequestSettings.getCarrierToAccountNumber(), kitRequestSettings.getServiceTo(), address);
                    List<LineItem> lineItems = new ArrayList<>();
                    lineItems.add(new LineItem(kitRequestSettings.getExternalShipperKitName(), "1"));
                    Order order = new Order(kit.getExternalOrderNumber(),kitRequestSettings.getExternalClientId(), kit.getParticipantId(), shippingInfo, lineItems);
                    orders.getOrders().add(order);
                }
                else {
                    logger.error("No address for participant w/ " + kit.getParticipantId());
                }
            }
            if (!orders.getOrders().isEmpty()) {
                String orderXml = GBFRequestUtil.orderXmlToString(Orders.class, orders);//todo pegah check this
                boolean test = DSMServer.isTest(getExternalShipperName());//true for dev in `not-secret.conf`
                JSONObject payload = new JSONObject().put("orderXml", orderXml).put("test", test);
                String sendRequest = DSMServer.getBaseUrl(getExternalShipperName()) + ORDER_ENDPOINT;
                String apiKey = DSMServer.getApiKey(getExternalShipperName());// todo pegah put that in the vault for dev deployment
                Response gbfResponse = null;
                int totalAttempts = 2 + additionalAttempts;
                Exception ex = null;
                try {
                    for (int i = 1; i <= totalAttempts; i++) {
                        //if this isn't the first attempt let's wait a little before retrying...
                        if (i > 1) {
                            logger.info("Sleeping before request retry for " + sleepInMs + " ms.");
                            Thread.sleep(sleepInMs);
                            logger.info("Sleeping done.");
                        }
                        try {
                            gbfResponse = executePost(Response.class, sendRequest, payload.toString(), apiKey);
                            break;
                        }
                        catch (Exception newEx) {
                            logger.warn( "Send request failed (attempt #" + i + " of " + totalAttempts + "): ", newEx);
                            ex = newEx;
                        }
                    }
                }
                catch (Exception outerEx) {
                    throw new RuntimeException("Unable to send requests.", ex);
                }
                if (gbfResponse != null && gbfResponse.isSuccess()) {
                    logger.info("Ordered kits");
                }
                else {
                    throw new ExternalShipperException("Unable to order kits after retry.", ex);
                }
            }
        }
    }

    // Order Status is as it sounds, a real-time status of the order progression through the GBF internal process
    // 'RECEIVED', 'PROCESSING', 'SHIPPED', 'DISTRIBUTION', 'FORMS PRINTED', 'CANCELLED', 'NOT FOUND', 'SHIPPED (SIMULATED)'
    // return the actual tube barcode for all tubes but only after the kit is shipped (around 7:00pm)
    public void orderStatus(ArrayList<KitRequest> kitRequests) throws Exception {
        if (kitRequests != null && !kitRequests.isEmpty()) {
            List<String> orderNumbers = new ArrayList<>();
            for (KitRequest kit : kitRequests) {
                if (!orderNumbers.contains(kit.getExternalOrderNumber())) {
                    orderNumbers.add(kit.getExternalOrderNumber());
                }
            }
            JSONObject payload = new JSONObject().put("orderNumbers", orderNumbers);
            String sendRequest = DSMServer.getBaseUrl(getExternalShipperName()) + STATUS_ENDPOINT;
            logger.info("payload: " + payload.toString());
            Response gbfResponse = executePost(Response.class, sendRequest, payload.toString(), DSMServer.getApiKey(getExternalShipperName()));
            if (gbfResponse != null && gbfResponse.isSuccess()) {
                List<Status> statuses = gbfResponse.getStatuses();
                if (statuses != null && !statuses.isEmpty()) {
                    for (Status status : statuses) {
                        List<String> dsmKitRequestIds = getDSMKitRequestId(status.getOrderNumber());
                        if (dsmKitRequestIds != null && !dsmKitRequestIds.isEmpty()) {
                            for (String dsmKitRequestId : dsmKitRequestIds) {
                                KitRequestExternal.updateKitRequest(status.getOrderStatus(), System.currentTimeMillis(), dsmKitRequestId);
                                //todo pegah tell Pepper about SHIPPED and DELIVERED kits here
                            }
                        }
                    }
                }
            }
            else {
                new RuntimeException("Failed to check status of kits from " + EXTERNAL_SHIPPER_NAME + ": " + gbfResponse.getErrorMessage());
            }
        }
    }

    // The confirmation, dependent upon level of detail required, is a shipping receipt to prove completion.
    // Confirmation may include order number, client(participant) ID, outbound tracking number, return tracking number(s), line item(s), kit serial number(s), etc.
    public void orderConfirmation(ArrayList<KitRequest> kitRequests, long startDate, long endDate) throws Exception {
        JSONObject payload = new JSONObject().put("startDate", SystemUtil.getDateFormatted(startDate)).put("endDate", SystemUtil.getDateFormatted(endDate));
        String sendRequest = DSMServer.getBaseUrl(getExternalShipperName()) + CONFIRM_ENDPOINT;
        logger.info("payload: " + payload.toString());
        Response gbfResponse = executePost(Response.class, sendRequest, payload.toString(), DSMServer.getApiKey(getExternalShipperName()));
        System.out.println(gbfResponse.getXML());
        if (gbfResponse != null && StringUtils.isNotBlank(gbfResponse.getXML())) {
            ShippingConfirmations shippingConfirmations = objectFromXMLString(ShippingConfirmations.class, gbfResponse.getXML());
            List<ShippingConfirmation> confirmationList = shippingConfirmations.getShippingConfirmations();
            if (confirmationList != null && !confirmationList.isEmpty()) {
                for (ShippingConfirmation confirmation : confirmationList) {
                    //update external shipper response at request
                    Node node = GBFRequestUtil.getXMLNode(gbfResponse.getXML(), XML_NODE_EXPRESSION.replace("%1", confirmation.getOrderNumber()));
                    List<String> dsmKitRequestIds = getDSMKitRequestId(confirmation.getOrderNumber());
                    if (dsmKitRequestIds != null && !dsmKitRequestIds.isEmpty()) {
                        for (String dsmKitRequestId : dsmKitRequestIds) {
                            KitRequestExternal.updateKitRequestResponse(getStringFromNode(node), dsmKitRequestId);
                        }
                    }
                    //update kit shipping information
                    if (confirmation.getItem() != null) {
                        List<SubItem> subItems = confirmation.getItem().getSubItem();
                        if (subItems != null && !subItems.isEmpty()) {
                            for (SubItem subItem : subItems) {
                                List<Tube> tubes = subItem.getTube();

                                for (int i = 0; i < tubes.size(); i++) {
                                    KitRequest kitRequest = getKitRequest(kitRequests, confirmation.getOrderNumber(), subItem.getItemNumber(), i);
                                    if (kitRequest != null) {//todo pegah check here
                                        KitRequestExternal.updateKitRequestResponse(confirmation.getTracking(), subItem.getReturnTracking(),
                                                tubes.get(i).getSerial(), SystemUtil.getLongFromDateString(confirmation.getShipDate()), EXTERNAL_SHIPPER_NAME,
                                                kitRequest.getDsmKitRequestId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                logger.info("No shipping confirmation returned");
            }
        }
    }

    public void orderCancellation(ArrayList<KitRequest> kitRequests) throws Exception {}

    public ArrayList<KitRequest> getKitRequestsNotDone(int instanceId) {
        ArrayList<KitRequest> kitRequests = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_EXTERNAL_KIT_NOT_DONE)) {
                stmt.setInt(1, instanceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        kitRequests.add(new KitRequest(rs.getString(DBConstants.DSM_KIT_REQUEST_ID), rs.getString(DBConstants.DDP_PARTICIPANT_ID),
                                null, null, rs.getString(DBConstants.EXTERNAL_ORDER_NUMBER), null,
                                rs.getString(DBConstants.EXTERNAL_ORDER_STATUS),
                                rs.getString("subkits." + DBConstants.EXTERNAL_KIT_NAME)));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error looking up kit requests  ", results.resultException);
        }
        logger.info("Found " + kitRequests.size() + " kit requests which do not have an end status yet");
        return kitRequests;
    }

    public List<String> getDSMKitRequestId(String externalOrderNumber) {
        List<String> dsmKitRequestIds = new ArrayList<>();
        SimpleResult results = inTransaction((conn) -> {
            SimpleResult dbVals = new SimpleResult();
            try (PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_KIT_REQUEST_BY_EXTERNAL_ORDER_NUMBER)) {
                stmt.setString(1, externalOrderNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        dsmKitRequestIds.add(rs.getString(DBConstants.DSM_KIT_REQUEST_ID));
                    }
                }
            }
            catch (SQLException ex) {
                dbVals.resultException = ex;
            }
            return dbVals;
        });

        if (results.resultException != null) {
            throw new RuntimeException("Error looking up kit requests  ", results.resultException);
        }
        logger.info("Found " + dsmKitRequestIds.size() + " kit requests with given external_order_number");
        return dsmKitRequestIds;
    }

    private KitRequest getKitRequest(@NonNull ArrayList<KitRequest> kitRequests, @NonNull String externalOrderNumber, String subKitName, int tubeNumber) {
        int counter = -1;
        for (KitRequest kitRequest : kitRequests) {
            if (externalOrderNumber.equals(kitRequest.getExternalOrderNumber())) {
                if (StringUtils.isNotBlank(subKitName)) {
                    if (subKitName.equals(kitRequest.getExternalKitName())) {
                        counter++;
                        if (counter == tubeNumber) {
                            return kitRequest;
                        }
                    }
                }
                else {
                    return kitRequest;
                }
            }
        }
        return null;
    }

    public static <T> T executePost(Class<T> responseClass, String sendRequest, Object objectToPost, String apiKey) throws Exception {
        logger.info("Requesting data from GBF w/ " + sendRequest);
        org.apache.http.client.fluent.Request request = SecurityUtil.createPostRequestWithHeader(sendRequest, apiKey, objectToPost);

//        T object = request.execute().handleResponse(res -> { //TODO remove blind trust!!!
        T object = blindTrustEverythingExecutor.execute(request).handleResponse(res -> {
            int responseCodeInt = res.getStatusLine().getStatusCode();
            try {
                if (responseCodeInt != HttpStatusCodes.STATUS_CODE_OK) {
                    logger.error("Got " + responseCodeInt + " from " + sendRequest);
                    if (responseCodeInt == HttpStatusCodes.STATUS_CODE_SERVER_ERROR) {
                        throw new RuntimeException("Got " + responseCodeInt + " from " + sendRequest);
                    }
                    else {
                        logger.error(Utility.getHttpResponseAsString(res));
                    }
                    return null;
                }
                else {
                    String response = Utility.getHttpResponseAsString(res);
                    return new Gson().fromJson(response, responseClass);
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to read HttpResponse ", e);
            }
        });
        return object;
    }

    public static <T> String orderXmlToString(Class<T> clazz, T object) throws JAXBException{
        StringWriter sw = new StringWriter();
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        jaxbMarshaller.marshal(object, sw);
        return sw.toString();
    }

    public static <T> T objectFromXMLString(Class<T> clazz, String xml) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        StringReader reader = new StringReader(xml);

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

        T object = (T) unmarshaller.unmarshal(xmlStreamReader);;

        return object;
    }

    public static Node getXMLNode(String xml, String expression) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        ByteArrayInputStream stream = new ByteArrayInputStream(xml.getBytes());
        Document document = builder.parse(stream);

        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Node) xPath.compile(expression).evaluate(document, XPathConstants.NODE);
    }

    public static String getStringFromNode(Node node) throws Exception {
        StreamResult xmlOutput = new StreamResult(new StringWriter());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(node), xmlOutput);
        return xmlOutput.getWriter().toString();
    }
}
