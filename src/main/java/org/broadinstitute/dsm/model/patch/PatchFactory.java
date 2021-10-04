package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.model.Patch;

public class PatchFactory {

    public static BasePatch makePatch(Patch patch) {
        BasePatch patcher = new NullPatch();



        // switch cases here
        return patcher;
    }





}
