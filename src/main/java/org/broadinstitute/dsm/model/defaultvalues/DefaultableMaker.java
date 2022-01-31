package org.broadinstitute.dsm.model.defaultvalues;

import org.broadinstitute.dsm.model.Study;

public class DefaultableMaker {

    public static Defaultable makeDefaultable(Study study) {
        Defaultable defaultable = (studyGuid, participantId) -> true;
        switch (study) {
            case ATCP:
                break;
            case RGP:
                defaultable = new RgpAutomaticProbandDataCreator();
                break;
            default:
                break;
        }
        return defaultable;
    }

}
