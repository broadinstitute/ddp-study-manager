package org.broadinstitute.dsm.careevolve;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.exception.CareEvolveException;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.statics.DBConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places orders with CareEvolve for COVID-19 virology
 */
public class Covid19OrderRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(Covid19OrderRegistrar.class);

    private final String endpoint;

    private final String careEvolveAccount;

    private final Provider provider;

    private static final List<String> KIT_ACTIVITY_CODES = List.of("BASELINE_SYMPTOM","LONGITUDINAL_COVID");

    /**
     * Create a new one that uses the given endpoint
     * for placing orders
     */
    public Covid19OrderRegistrar(String endpoint,
                                 String careEvolveAccount,
                                 Provider provider) {
        this.endpoint = endpoint;
        this.careEvolveAccount  = careEvolveAccount;
        this.provider = provider;
    }

    /**
     * Places an order in CareEvolve
     * @param auth the API credentials
     * @param participantHruid hruid for the participant
     * @param kitLabel The label on the swab.  Corresponds to ORC-2 and GP sample_id
     * @param kitId an identifier that will show up in Birch to help
     *              associate the result back to the proper kit
     */
    public OrderResponse orderTest(Authentication auth, String participantHruid, String kitLabel,
                                   String kitId) throws CareEvolveException {

        DDPInstance instance = DDPInstance.getDDPInstanceWithRole("testboston", DBConstants.HAS_KIT_REQUEST_ENDPOINTS);

        Map<String, String> queryConditions = new HashMap<>();
        queryConditions.put("ES", " AND profile.hruid = '" + participantHruid +"'");
        List<ParticipantWrapper> participants = ParticipantWrapper.getFilteredList(instance, queryConditions);

        // "activityCode" -> "BASELINE_COVID"
        // data.get("activities") DOB SEX RACE

        if (participants.size() == 1) {
            ParticipantWrapper participant = participants.iterator().next();
            JsonObject data = participant.getDataAsJson();
            if (data != null) {
                JsonObject address = data.get("address").getAsJsonObject();
                if (address != null) {
                    Address careEvolveAddress = toCareEvolveAddress(address);

                    JsonObject profile = data.get("profile").getAsJsonObject();
                    String patientId = profile.get("guid").getAsString();

                    String firstName = profile.get("firstName").getAsString();
                    String lastName = profile.get("lastName").getAsString();

                    List<AOE> aoes = AOE.forTestBoston(null, kitId);

                    JsonArray activities = data.get("activities").getAsJsonArray();

                    JsonObject latestKitActivity = getLatestKitActivity(activities);
                    JsonArray latestKitAnswers = latestKitActivity.get("questionsAnswers").getAsJsonArray();

                    StringBuilder collectionDateTimeStr = new StringBuilder();
                    Instant collectionDateTime = null;
                    for (int i = 0; i < latestKitAnswers.size(); i++) {
                        JsonObject latestKitAnswer = latestKitAnswers.get(i).getAsJsonObject();
                        String questionStableId = latestKitAnswer.get("stableId").getAsString();

                        if ("SAMPLE_COLLECT_DATE".equals(questionStableId)) {
                            JsonObject dateFields = latestKitAnswer.get("dateFields").getAsJsonObject();
                            collectionDateTimeStr.append(dateFields.get("month")).append("/")
                                    .append(dateFields.get("day")).append("/")
                                    .append(dateFields.get("year"));
                        } else if ("SAMPLE_COLLECT_TIME".equals(questionStableId)) {
                            JsonArray timeFields = latestKitAnswer.getAsJsonArray("answer").get(0).getAsJsonArray();
                            for (int timeFieldIdx = 0; timeFieldIdx < timeFields.size(); timeFieldIdx++) {
                                collectionDateTimeStr.append(" ").append(timeFields.get(timeFieldIdx).getAsString());
                            }
                        }
                    }

                    try {
                        collectionDateTime = new SimpleDateFormat("MM/dd/yyyy h m a").parse(collectionDateTimeStr.toString()).toInstant();
                    } catch (ParseException e) {
                        throw new CareEvolveException("Could not parse collection date time " + collectionDateTimeStr.toString()
                                + " for participant " + patientId + " in activity instance " + latestKitActivity.get("guid"));
                    }

                    // todo arz add dob/race/ethnicity when available
                    Patient testPatient = new Patient(patientId, firstName, lastName, "1901-01-01", "Other", "Other",
                            "other", careEvolveAddress);

                    Message message = new Message(new Order(careEvolveAccount, testPatient, kitLabel, collectionDateTime, provider, aoes), kitId);

                    OrderResponse orderResponse = null;
                    try {
                        orderResponse = orderTest(auth, message);
                    } catch (IOException e) {
                        throw new CareEvolveException("Could not order test for " + patientId, e);
                    }

                    if (StringUtils.isNotBlank(orderResponse.getError())) {
                        throw new CareEvolveException("Order for participant " + participantHruid + " with handle  "+ orderResponse.getHandle() + " placed with error " + orderResponse.getError());
                    }
                    return orderResponse;
                } else {
                    throw new CareEvolveException("No address for " + participantHruid + ".  Cannot register order.");
                }
            } else {
                throw new CareEvolveException("No participant data for " + participantHruid + ".  Cannot register order.");
            }
        } else {
            throw new CareEvolveException("No particiant data found for " + participantHruid + ".  Cannot register order.");
        }
    }

    /**
     * Returns the latest kit-related activity
     */
    private JsonObject getLatestKitActivity(JsonArray activities) {
        JsonObject latestKitActivity = null;
        for (int i = 0; i < activities.size(); i++) {
            JsonObject activity = activities.get(i).getAsJsonObject();
            if (KIT_ACTIVITY_CODES.contains(activity.get("activityCode").getAsString())) {
                if (latestKitActivity == null) {
                    latestKitActivity = activity;
                }
                if (latestKitActivity.get("createdAt").getAsNumber().longValue() < activity.get("createdAt").getAsNumber().longValue()) {
                    latestKitActivity = activity;
                }
            }
        }
        return latestKitActivity;
    }

    private Address toCareEvolveAddress(JsonObject esAddress) {
        return new Address(esAddress.get("street1").getAsString(),
                esAddress.get("street2").getAsString(),
                esAddress.get("city").getAsString(),
                esAddress.get("state").getAsString(),
                esAddress.get("zip").getAsString());
    }

    /**
     * Order a test using the given auth and message details.
     * Returns an {@link OrderResponse} when the order has been
     * placed successfully.  Otherwise, an exception is thrown.
     */
    private OrderResponse orderTest(Authentication auth, Message message) throws IOException {
        logger.info("About to send {} as {} to {}", message.getName(), endpoint);
        AuthenticatedMessage careEvolveMessage = new AuthenticatedMessage(auth, message);

        String json = new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(careEvolveMessage);

        Response response = Request.Post(endpoint).bodyString(json, ContentType.APPLICATION_JSON).execute();
        HttpResponse httpResponse = response.returnResponse();

        String responseString = EntityUtils.toString(httpResponse.getEntity());

        if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            OrderResponse orderResponse = new Gson().fromJson(IOUtils.toString(httpResponse.getEntity().getContent()), OrderResponse.class);
            return orderResponse;
        } else {
            logger.error("Order {} returned {} with {}", message.getName(), httpResponse.getStatusLine().getStatusCode(), responseString);
            throw new CareEvolveException("CareEvolve returned " + httpResponse.getStatusLine().getStatusCode() + " with " + responseString);
        }
    }
}
