package org.broadinstitute.dsm.model.auth;

import lombok.Data;

import java.util.List;

@Data
public class AccessRole {

    private String roleName;
    private List<String> permissions;

    public AccessRole(String roleName, List<String> permissions) {
        this.roleName = roleName;
        this.permissions = permissions;
    }
}
