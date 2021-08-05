package org.broadinstitute.dsm.model.filter.participant;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.db.dao.ddp.instance.DDPInstanceDao;
import org.broadinstitute.dsm.db.dto.ddp.instance.DDPInstanceDto;
import org.broadinstitute.dsm.model.elasticsearch.ElasticSearch;
import org.broadinstitute.dsm.model.participant.ParticipantWrapper;
import org.broadinstitute.dsm.model.participant.ParticipantWrapperDto;
import org.broadinstitute.dsm.model.participant.ParticipantWrapperPayload;
import org.broadinstitute.dsm.statics.RoutePath;
import spark.QueryParamsMap;

public class EmptyFilterParticipantList extends BaseFilterParticipantList {


    @Override
    public List<ParticipantWrapperDto> filter(QueryParamsMap queryParamsMap) {
        if(!Objects.requireNonNull(queryParamsMap).hasKey(RoutePath.REALM)) throw new RuntimeException("realm is necessary");
        String realm = queryParamsMap.get(RoutePath.REALM).value();
        DDPInstance ddpInstance = DDPInstance.getDDPInstance(realm);
        DDPInstanceDto ddpInstanceByGuid = new DDPInstanceDao().getDDPInstanceByGuid(realm).orElseThrow();
        ParticipantWrapperPayload participantWrapperPayload = new ParticipantWrapperPayload.Builder()
                .withDdpInstanceDto(ddpInstanceByGuid)
                .withFrom(0)
                .withTo(50)
                .build();
        return new ParticipantWrapper(participantWrapperPayload, new ElasticSearch.Builder().build()).getFilteredList();
    }
}
