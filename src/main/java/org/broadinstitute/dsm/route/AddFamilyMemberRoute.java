package org.broadinstitute.dsm.route;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import org.broadinstitute.ddp.handlers.util.Result;
import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.ParticipantData;
import org.broadinstitute.dsm.db.User;
import org.broadinstitute.dsm.model.ddp.AddFamilyMemberPayload;
import org.broadinstitute.dsm.model.ddp.FamilyMemberDetails;
import org.broadinstitute.dsm.security.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class AddFamilyMemberRoute extends RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(AddFamilyMemberRoute.class);

    static final String FIELD_TYPE = "RGP_PARTICIPANTS";

    @Override
    protected Object processRequest(Request request, Response response, String userId) throws Exception {
        Gson gson = new Gson();
        AddFamilyMemberPayload addFamilyMemberPayload = gson.fromJson(request.body(), AddFamilyMemberPayload.class);

        String participantGuid = addFamilyMemberPayload.getParticipantGuid()
                .orElseThrow(() -> new NoSuchElementException("Participant Guid is not provided"));

        String realm =
                addFamilyMemberPayload.getRealm().orElseThrow(() -> new NoSuchElementException("Realm is not provided"));
        String ddpInstanceId = DDPInstance.getDDPInstance(realm).getDdpInstanceId();

        Optional<FamilyMemberDetails> maybeFamilyMemberData = addFamilyMemberPayload.getData();
        if (maybeFamilyMemberData.isEmpty() || isFamilyMemberFieldsEmpty(maybeFamilyMemberData)) {
            response.status(400);
            return new Result(400, "Family member information is not provided");
        }
        FamilyMemberDetails familyMemberData = addFamilyMemberPayload.getData().get();

        Integer uId =
                addFamilyMemberPayload.getUserId().orElseThrow(() -> new NoSuchElementException("User id is not provided"));
        if (Integer.parseInt(userId) != uId) {
            throw new RuntimeException("User id was not equal. User id in token " + userId + " user id in request " + uId);
        }
        try {
            ParticipantData.createNewParticipantData(participantGuid, ddpInstanceId, FIELD_TYPE, gson.toJson(familyMemberData), User.getUser(uId).getEmail());
            logger.info("Family member for participant " + participantGuid + " successfully created");
        } catch (Exception e) {
            throw new RuntimeException("Could not create family member " + e);
        }
        return new Result(200);
    }

    boolean isFamilyMemberFieldsEmpty(Optional<FamilyMemberDetails> maybeFamilyMemberData) {
        AtomicBoolean allFieldsFilled = new AtomicBoolean(false); //AtomicBoolean used to change its value in lambda expression
        maybeFamilyMemberData.ifPresent(data -> {
            if (StringUtils.isBlank(data.getFirstName()) || StringUtils.isBlank(data.getLastName()) || StringUtils.isBlank(data.getMemberType())
            || StringUtils.isBlank(data.getFamilyId()) || StringUtils.isBlank(data.getCollaboratorParticipantId())) {
                allFieldsFilled.set(true);
            }
        });
        return allFieldsFilled.get();
    }
}
