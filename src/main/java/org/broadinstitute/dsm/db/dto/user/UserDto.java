package org.broadinstitute.dsm.db.dto.user;

import java.util.Optional;

import lombok.Setter;

@Setter
public class UserDto {

    private int id;
    private String name;
    private String email;

    public UserDto(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public UserDto() {

    }

    public int getId() {
        return this.id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }
}
