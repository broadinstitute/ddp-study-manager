package org.broadinstitute.dsm.model.defaultvalues;

import org.broadinstitute.dsm.model.elastic.ESActivities;
import org.broadinstitute.dsm.model.elastic.search.ElasticSearchParticipantDto;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class DefaultValuesTest {

    @Test
    public void isSelfOrDependentParticipant() {
        DefaultValues defaultValues = new DefaultValues();

        ESActivities esActivities = new ESActivities();
        esActivities.setActivityCode("PREQUAL");
        esActivities.setQuestionsAnswers(Collections.singletonList(Map.of("answer", List.of("CHILD_DIAGNOSED"))));

        ESActivities esActivities2 = new ESActivities();
        esActivities2.setActivityCode("PREQUAL");
        esActivities2.setQuestionsAnswers(Collections.singletonList(Map.of("answer", List.of("DIAGNOSED"))));

        ElasticSearchParticipantDto participantDto = new ElasticSearchParticipantDto.Builder()
                .withActivities(List.of(esActivities))
                .build();

        defaultValues.elasticSearchParticipantDto = participantDto;

        Assert.assertTrue(defaultValues.isSelfOrDependentParticipant());

        participantDto.setActivities(List.of(esActivities2));

        Assert.assertTrue(defaultValues.isSelfOrDependentParticipant());
    }
}