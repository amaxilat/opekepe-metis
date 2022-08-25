package com.amaxilatis.metis.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SpringSecurityWebAppConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private SimpleAuthenticationSuccessHandler successHandler;
    @Autowired
    PasswordEncoder encoder;
    
    @Value("${metis.ldap.domain:none}")
    private String ldapDomain;
    
    @Value("${metis.ldap.url:none}")
    private String ldapUrl;
    
    @Value("${metis.ldap.use:false}")
    private boolean useLdap;
    
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
                    .inMemoryAuthentication()
                    .passwordEncoder(encoder)
                    .withUser("metis")
                    .password(encoder.encode("password"))
                    .roles("ADMIN");
        }
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel.anyRequest().requiresSecure()).csrf().disable().cors().and().authorizeRequests()
                //public
                .antMatchers("/login", "/do_login", "/webjars/**", "/js/**", "/img/**", "/css/**", "/files/**", "/icons/**").permitAll()
                //authorized
                .antMatchers("/**").fullyAuthenticated()
                //login
                .and().formLogin()
                //login info
                .loginPage("/login").usernameParameter("username").passwordParameter("password").loginProcessingUrl("/login").defaultSuccessUrl("/").successHandler(successHandler)
                //failure
                .failureUrl("/login")
                //logout
                .and().logout().logoutUrl("/do_logout").logoutSuccessUrl("/login").deleteCookies("JSESSIONID");
    }
}
