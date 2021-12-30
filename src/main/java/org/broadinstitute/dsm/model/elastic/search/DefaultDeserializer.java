package org.broadinstitute.dsm.model.elastic.search;

import java.util.Map;
import java.util.Optional;

import org.broadinstitute.dsm.util.ObjectMapperSingleton;

public class DefaultDeserializer implements Deserializer {

    @Override
    public Optional<ElasticSearchParticipantDto> deserialize(Map<String, Object> sourceMap) {
        return Optional.ofNullable(ObjectMapperSingleton.instance().convertValue(sourceMap, ElasticSearchParticipantDto.class));
    }
}
