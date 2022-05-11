package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class UserListDTO {
    private Collection<UserDTO> users;
}
