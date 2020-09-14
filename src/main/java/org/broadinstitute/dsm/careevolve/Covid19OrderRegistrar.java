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

    public static final String BASELINE_SYMPTOM_ACTIVITY = "BASELINE_SYMPTOM";

    public static final String BASELINE_COVID_ACTIVITY = "BASELINE_COVID";

    private static final List<String> KIT_ACTIVITY_CODES = List.of(BASELINE_SYMPTOM_ACTIVITY,"LONGITUDINAL_COVID");

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
     * @param kitPickupTime the time at which the kit was picked
     *                      up from the participant
     */
    public OrderResponse orderTest(Authentication auth, String participantHruid, String kitLabel,
                                   String kitId, Instant kitPickupTime) throws CareEvolveException {

        DDPInstance instance = DDPInstance.getDDPInstanceWithRole("testboston", DBConstants.HAS_KIT_REQUEST_ENDPOINTS);

        Map<String, String> queryConditions = new HashMap<>();
        queryConditions.put("ES", " AND profile.hruid = '" + participantHruid +"'");
        List<ParticipantWrapper> participants = ParticipantWrapper.getFilteredList(instance, queryConditions);

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

                    List<AOE> aoes = AOE.forTestBoston(patientId, kitId);

                    JsonArray activities = data.get("activities").getAsJsonArray();

                    JsonObject latestKitActivity = getLatestKitActivity(activities);
                    JsonArray latestKitAnswers = latestKitActivity.get("questionsAnswers").getAsJsonArray();

                    String collectionDateTimeStr = null;
                    Instant collectionDateTime = null;
                    StringBuilder collectionDate = new StringBuilder();
                    StringBuilder collectionTime = new StringBuilder();
                    for (int i = 0; i < latestKitAnswers.size(); i++) {
                        JsonObject latestKitAnswer = latestKitAnswers.get(i).getAsJsonObject();
                        String questionStableId = latestKitAnswer.get("stableId").getAsString();

                        if ("SAMPLE_COLLECT_DATE".equals(questionStableId)) {
                            collectionDate.append(latestKitAnswer.get("date").getAsString());
                        } else if ("SAMPLE_COLLECT_TIME".equals(questionStableId)) {
                            JsonArray timeFields = latestKitAnswer.getAsJsonArray("answer").get(0).getAsJsonArray();
                            for (int timeFieldIdx = 0; timeFieldIdx < timeFields.size(); timeFieldIdx++) {
                                collectionTime.append(" ").append(timeFields.get(timeFieldIdx).getAsString());
                            }
                        }
                    }

                    if (collectionDate.length() > 0 && collectionTime.length() > 0) {
                        collectionDateTimeStr = collectionDate + " " + collectionTime;
                        try {
                            collectionDateTime = new SimpleDateFormat("yyyy-MM-dd h m a").parse(
                                    collectionDateTimeStr
                            ).toInstant();
                        } catch (ParseException e) {
                            throw new CareEvolveException("Could not parse collection date time " + collectionDateTimeStr.toString()
                                    + " for participant " + patientId + " in activity instance " + latestKitActivity.get("guid"), e);
                        }
                    } else {
                        collectionDateTime = kitPickupTime;
                    }

                    if (collectionDateTime == null) {
                        logger.error("No kit collection time for "+ patientId);
                    }

                    JsonObject baselineCovidActivity = getBaselineCovidActivity(activities);
                    JsonArray baselineCovidAnswers = baselineCovidActivity.get("questionsAnswers").getAsJsonArray();
                    String dob = null;

                    // mappings  for  race, ethnicity, and  sex are custom  for the  Ellkay API
                    // and are controlled  by the Ellkay integration
                    String race = "2131-1"; // code for "other"
                    String ethnicity = "U";
                    String sex = "U";
                    for (int i = 0; i < baselineCovidAnswers.size(); i++) {
                        JsonObject answer = baselineCovidAnswers.get(i).getAsJsonObject();
                        String questionStableId = answer.get("stableId").getAsString();

                        if ("DOB".equals(questionStableId)) {
                            JsonObject dateFields = answer.get("dateFields").getAsJsonObject();
                            StringBuilder dobStr  = new StringBuilder();
                            dobStr.append(dateFields.get("year")).append("-")
                                    .append(dateFields.get("month")).append("-")
                                    .append(dateFields.get("day"));
                            SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");
                            try {
                                dob = dobFormat.format(dobFormat.parse(dobStr.toString()));
                            } catch (ParseException e) {
                                throw new CareEvolveException("Could not parse dob " + dobStr.toString()
                                        + " for participant " + patientId + " in activity instance " + baselineCovidActivity.get("guid"));
                            }

                        } else if ("SEX".equals(questionStableId)) {
                            JsonArray sexAnswers = answer.getAsJsonArray("answer");
                            if (sexAnswers.size() > 0) {
                                sex = sexAnswers.get(0).getAsString();
                            }

                            if ("SEX_FEMALE".equals(sex)) {
                                sex = "F";
                            } else if ("SEX_MALE".equals(sex)) {
                                sex = "M";
                            } else if ("OTHER".equals(sex)) {
                                sex = "U";
                            }  else {
                                logger.error("Could not map sex " + sex + " to Ellkay code; will default to unknown for " + baselineCovidActivity.get("guid"));
                            }

                        }  else if ("RACE".equals(questionStableId)) {
                            JsonArray races = answer.getAsJsonArray("answer");

                            if (races.size() == 1) {
                                // CareEvolve only supports a  single race per order
                                race = races.get(0).getAsString();

                                if ("ASIAN".equals(race)) {
                                    race = "2028-9";
                                } else if ("BLACK".equals(race)) {
                                    race = "2054-5";
                                } else if ("AMERICAN_INDIAN".equals(race)) {
                                    race = "2054-5";
                                } else if ("NATIVE_HAWAIIAN".equals(race)) {
                                    race = "2076-8";
                                } else if ("WHITE".equals(race)) {
                                    race = "2106-3";
                                } else if ("OTHER".equals(race)) {
                                    race = "2131-1";
                                } else {
                                    logger.error("Could not map race " + race + " to Ellkay code; will default to other for " + baselineCovidActivity.get("guid"));
                                }
                            } else {
                                // if there's no value or if multiple values are selected,
                                // use the code for "other"
                                race = "2131-1";
                            }
                        }
                        else if ("ETHNICITY".equals(questionStableId)) {
                            JsonArray answers = answer.getAsJsonArray("answer");
                            if  (answers.size() > 0) {
                                ethnicity = answer.getAsJsonArray("answer").get(0).getAsString();
                                if ("HISPANIC_LATINO".equals(ethnicity)) {
                                    ethnicity = "H";
                                } else if ("NOT_HISPANIC_LATINO".equals(ethnicity)) {
                                    ethnicity = "N";
                                } else {
                                    logger.error("Could not map ethnicity " + ethnicity + " to Ellkay code; will default to unknown for " + baselineCovidActivity.get("guid"));
                                }
                            }
                        }

                    }

                    Patient testPatient = new Patient(patientId, firstName, lastName, dob, race, ethnicity,
                            sex, careEvolveAddress);

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

    private JsonObject getBaselineCovidActivity(JsonArray activities) {
        JsonObject baselineCovidActivity = null;
        for (int i = 0; i < activities.size(); i++) {
            JsonObject activity = activities.get(i).getAsJsonObject();
            if (BASELINE_COVID_ACTIVITY.equals(activity.get("activityCode").getAsString())) {
                baselineCovidActivity = activity;
            }
        }
        return baselineCovidActivity;
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
