package com.amaxilatis.metis.server.service;

import com.amaxilatis.metis.server.db.model.User;
import com.amaxilatis.metis.server.db.repository.UserRepository;
import com.amaxilatis.metis.server.model.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    public UserDTO getByUsername(final String username) {
        final User u = userRepository.findByUsername(username);
        return UserDTO.fromUser(u);
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
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(UserDTO::fromUser).collect(Collectors.toList());
    }
    
    public User addUser(final String username, final String password, final String name, final String role, final Boolean enabled) {
        return userRepository.save(User.builder().username(username).password(new BCryptPasswordEncoder().encode(password)).name(name).role(role).enabled(enabled).build());
    }
    
    public void deleteUser(final String username) {
        userRepository.delete(userRepository.findByUsername(username));
    }
    
    public boolean updateUserPassword(final String username, final String oldPassword, final String newPassword) {
        User u = userRepository.findByUsername(username);
        if (u != null && encoder.matches(oldPassword, u.getPassword())) {
            u.setPassword(encoder.encode(newPassword));
            userRepository.save(u);
            return true;
        } else {
            return false;
        }
    }
}
