package org.broadinstitute.dsm.model;

import java.util.Collection;

public class KitUploadResponse {

    private Collection<KitRequest> invalidKitAddressList;
    private Collection<KitRequest> duplicateKitList;
    private Collection<KitRequest> specialKitList;

    public KitUploadResponse(Collection<KitRequest> invalidKitAddressList, Collection<KitRequest> duplicateKitList, Collection<KitRequest> specialKitList) {
        this.invalidKitAddressList = invalidKitAddressList;
        this.duplicateKitList = duplicateKitList;
        this.specialKitList = specialKitList;
    }
}
