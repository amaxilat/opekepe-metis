package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    
    public UserDTO getByUsername(final String username) {
        return UserDTO.builder().username(username).enabled(true).name(username).local(true).role("ADMIN").build();
    }
    
    public UserDTO getByUsername(final String username, final Collection<GrantedAuthority> authorities) {
        final UserDTO u = getByUsername(username);
        u.setAuthorities(authorities);
        return u;
    }
    
    public UserDTO getBySpringUser(final org.springframework.security.core.userdetails.User u) {
        return getByUsername(u.getUsername(), u.getAuthorities());
    }
    
    public UserDTO getByLdapUser(final LdapUserDetailsImpl ldapUser) {
        final UserDTO u = UserDTO.fromUser(ldapUser);
        u.setAuthorities(ldapUser.getAuthorities());
        return u;
    }
}
