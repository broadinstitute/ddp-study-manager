package org.broadinstitute.dsm.model.patch;

import org.broadinstitute.dsm.model.Patch;

public class PatchFactory {

    public static Patchable makePatch(Patch patch) {
        Patchable patcher = new NullPatch();
        // switch cases here
        return patcher;
    }



}
