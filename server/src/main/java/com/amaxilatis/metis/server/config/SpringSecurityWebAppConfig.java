package com.amaxilatis.metis.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//        auth //inmemmory details
//                .inMemoryAuthentication()
//                .passwordEncoder(encoder)
//                .withUser("test")
//                .password(encoder.encode("user"))
//                .roles("ADMIN");
        auth //ldap details
                .ldapAuthentication()
                .rolePrefix("LDAP_")
                .userDnPatterns("uid={0},ou=people")
                .groupSearchBase("ou=groups")
                .contextSource()
                .url("ldap://localhost:8389/dc=springframework,dc=org")
                .and()
                .passwordCompare()
                .passwordEncoder(encoder)
                .passwordAttribute("userPassword");
        auth //jdbc details
                .jdbcAuthentication()
                //encoder
                .passwordEncoder(encoder)
                //datasource
                .dataSource(dataSource)
                //select user
                .usersByUsernameQuery("select username, password, enabled from user where username=?")
                //authorities
                .authoritiesByUsernameQuery("select username, role from user where username=?")
                .rolePrefix("JDBC_");
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
