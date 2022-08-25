package com.amaxilatis.metis.server.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;

import java.util.Collection;

@Data
@Builder
public class UserDTO {
    private String name;
    
    private String username;
    
    private String role;
    
    private Collection<GrantedAuthority> authorities;
    
    private Boolean enabled;
    
    private Boolean local;
    
    public static UserDTO fromUser(LdapUserDetailsImpl u) {
        if (u == null) {
            return null;
        }
        return UserDTO.builder().name(u.getUsername()).username(u.getUsername()).role("USER").enabled(u.isEnabled()).local(false).build();
    }
}
