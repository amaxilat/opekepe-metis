package com.amaxilatis.metis.server.model;

import com.amaxilatis.metis.server.db.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private String name;
    
    private String username;
    
    private String role;
    
    private Boolean enabled;
    
    public static UserDTO fromUser(final User u) {
        if (u == null) {
            return null;
        }
        return UserDTO.builder().name(u.getName()).username(u.getUsername()).role(u.getRole()).enabled(u.getEnabled()).build();
    }
}
