package com.amaxilatis.metis.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;

@Configuration
@Order(-1)
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SpringSecurityWebAppConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private SimpleAuthenticationSuccessHandler successHandler;
    
    @Autowired
    public void configAuthentication(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                //encoder
                .passwordEncoder(new BCryptPasswordEncoder())
                //datasource
                .dataSource(dataSource)
                //select user
                .usersByUsernameQuery("select username, password, enabled from user where username=?")
                //authorities
                .authoritiesByUsernameQuery("select username, role from user where username=?");
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
