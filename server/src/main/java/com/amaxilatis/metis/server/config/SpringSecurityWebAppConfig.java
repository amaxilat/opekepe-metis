package com.amaxilatis.metis.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityWebAppConfig extends WebSecurityConfigurerAdapter {
    
    private final SimpleAuthenticationSuccessHandler successHandler;
    final PasswordEncoder encoder;
    
    @Value("${metis.ldap.domain:none}")
    private String ldapDomain;
    
    @Value("${metis.ldap.url:none}")
    private String ldapUrl;
    
    @Value("${metis.ldap.use:false}")
    private boolean useLdap;
    
    @Value("${metis.user.authority:METIS}")
    private String userAuthority;
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (useLdap) {
            final ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider =
                    // connect to the active directory
                    new ActiveDirectoryLdapAuthenticationProvider(ldapDomain, ldapUrl);
            
            // to parse AD failed credentials' error message due to account - expiry,lock, credentials - expiry,lock
            activeDirectoryLdapAuthenticationProvider.setConvertSubErrorCodesToExceptions(true);
            
            auth //active directory details
                    .authenticationProvider(activeDirectoryLdapAuthenticationProvider);
        } else {
            auth //inmemmory details
                    .inMemoryAuthentication().passwordEncoder(encoder)
                    //user details
                    .withUser("metis").password(encoder.encode("password")).authorities(userAuthority);
        }
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel.anyRequest().requiresSecure()).csrf().disable().cors().and().authorizeRequests()
                //public
                .antMatchers("/login", "/do_login", "/webjars/**", "/js/**", "/img/**", "/css/**", "/files/**", "/icons/**").permitAll()
                //authorized
                .antMatchers("/**").hasAnyAuthority(userAuthority)
                //login
                .and().formLogin()
                //login info
                .loginPage("/login").usernameParameter("username").passwordParameter("password").loginProcessingUrl("/login").defaultSuccessUrl("/").successHandler(successHandler)
                //failure
                .failureUrl("/login")
                .and()
                .exceptionHandling().accessDeniedPage("/403")
                //logout
                .and().logout().logoutUrl("/do_logout").logoutSuccessUrl("/login").deleteCookies("JSESSIONID");
    }
}
