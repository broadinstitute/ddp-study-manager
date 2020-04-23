package org.broadinstitute.dsm.model.auth;

import lombok.Data;

import java.util.List;

@Data
public class Access {

    private long exp;
    private List<AccessRole> accessRoles;

    public Access(long exp, List<AccessRole> accessRoles) {
        this.exp = exp;
        this.accessRoles = accessRoles;
    }
}
