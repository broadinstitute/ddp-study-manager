package org.broadinstitute.dsm.model.patch;

import java.util.List;
import java.util.Optional;

import org.broadinstitute.dsm.model.NameValue;

public abstract class BasePatch implements Patchable {


    abstract Optional<Object> processMultipleNameValues(List<NameValue> nameValues);

}
