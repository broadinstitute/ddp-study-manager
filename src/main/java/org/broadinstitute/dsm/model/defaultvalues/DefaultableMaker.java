package org.broadinstitute.dsm.model.defaultvalues;

import org.broadinstitute.dsm.model.rgp.AutomaticProbandDataCreator;

public class DefaultableMaker {

    public enum Study {
        ATCP,
        RGP
    }

    public static Defaultable makeDefaultable(Study study) {
        Defaultable defaultable = (studyGuid, participantId) -> true;
        switch (study) {
            case ATCP:
                break;
            case RGP:
                defaultable = new AutomaticProbandDataCreator();
                break;
            default:
                break;
        }
        return defaultable;
    }

}
