package org.broadinstitute.dsm.careevolve;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.broadinstitute.dsm.exception.CareEvolveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places orders with CareEvolve for COVID-19 virology
 */
public class Covid19OrderRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(Covid19OrderRegistrar.class);

    private final String endpoint;

    /**
     * Create a new one that uses the given endpoint
     * for placing orders
     */
    public Covid19OrderRegistrar(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Order a test using the given auth and message details.
     * Returns an {@link OrderResponse} when the order has been
     * placed successfully.  Otherwise, an exception is thrown.
     */
    public OrderResponse orderTest(Authentication auth, Message message) throws IOException {
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
