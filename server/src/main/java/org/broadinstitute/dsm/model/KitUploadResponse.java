package org.broadinstitute.dsm.model;

import java.util.Collection;

public class KitUploadResponse {

    private Collection<KitRequest> invalidKitAddressList;
    private Collection<KitRequest> duplicateKitList;

    public KitUploadResponse(Collection<KitRequest> invalidKitAddressList, Collection<KitRequest> duplicateKitList) {
        this.invalidKitAddressList = invalidKitAddressList;
        this.duplicateKitList = duplicateKitList;
    }
}
