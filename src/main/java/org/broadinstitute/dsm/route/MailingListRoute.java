package org.broadinstitute.dsm.route;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.ddp.handlers.util.Contact;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.security.RequestHandler;
import org.broadinstitute.dsm.statics.RoutePath;
import org.broadinstitute.dsm.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Collection;

public class MailingListRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(MailingListRoute.class);

    private static final String PERMISSION = "mailingList:view";

    public MailingListRoute(@NonNull Auth0Util auth0Util) {
        super(auth0Util, PERMISSION);
    }

    @Override
    public Object processRequest(Request request, Response response, String userId, String userMail) throws Exception {
        String realm = null;
        if (StringUtils.isNotBlank(getRealm())) {
            realm = getRealm();
        }
        else {
            throw new RuntimeException("No realm was sent!");
        }
        return getMailingListContacts(realm);
    }

    public Collection<Contact> getMailingListContacts(String realm) {
        DDPInstance instance = DDPInstance.getDDPInstance(realm);

        if (instance == null) {
            throw new RuntimeException("Instance name was not found " + realm);
        }

        Contact[] ddpMailingListContacts = null;
        String sendRequest = instance.getBaseUrl() + RoutePath.DDP_MAILINGLIST_PATH;
        try {
            ddpMailingListContacts = DDPRequestUtil.getResponseObject(Contact[].class, sendRequest, instance.getName(), instance.isHasAuth0Token());
            logger.info("Got " + ddpMailingListContacts.length + " mailing list contacts ");
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't get mailing list contacts from " + sendRequest, ex);
        }
        return Arrays.asList(ddpMailingListContacts);
    }
}
