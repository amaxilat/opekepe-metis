package com.amaxilatis.metis.server.model;

import com.amaxilatis.metis.server.db.model.User;
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
    
    public static UserDTO fromUser(final User u) {
        if (u == null) {
            return null;
        }
        return UserDTO.builder().name(u.getName()).username(u.getUsername()).role(u.getRole()).enabled(u.getEnabled()).local(true).build();
    }
    
    public static UserDTO fromUser(LdapUserDetailsImpl u) {
        if (u == null) {
            return null;
        }
        return UserDTO.builder().name(u.getUsername()).username(u.getUsername()).role("USER").enabled(u.isEnabled()).local(false).build();
    }
}
