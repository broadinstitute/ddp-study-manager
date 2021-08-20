package org.broadinstitute.dsm.model.filter.participant;

import java.util.List;
import java.util.Objects;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.model.ParticipantWrapper;
import org.broadinstitute.dsm.statics.DBConstants;
import org.broadinstitute.dsm.statics.RoutePath;
import spark.QueryParamsMap;

public class EmptyFilterParticipantList extends BaseFilterParticipantList {


    @Override
    public List<ParticipantWrapper> filter(QueryParamsMap queryParamsMap) {
        if(!Objects.requireNonNull(queryParamsMap).hasKey(RoutePath.REALM)) throw new RuntimeException("realm is necessary");
        String realm = queryParamsMap.get(RoutePath.REALM).value();
        DDPInstance ddpInstance = DDPInstance.getDDPInstanceWithRole(realm, DBConstants.HAS_MEDICAL_RECORD_ENDPOINTS);
        return ParticipantWrapper.getFilteredList(ddpInstance, null);
    }
}
