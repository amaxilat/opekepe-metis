package com.amaxilatis.metis.server.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Order(-1)
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SpringSecurityWebAppConfig extends WebSecurityConfigurerAdapter {
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
    
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
        return new InMemoryUserDetailsManager(user);
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .cors()
                .and().authorizeRequests()
                //public
                .antMatchers("/login", "/do_login", "/webjars/**", "/js/**", "/img/**", "/css/**", "/files/**", "/icons/**").permitAll()
                //authorized
                .antMatchers("/**").fullyAuthenticated()
                //login
                .and()
                .formLogin()
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/login")
                .and()
                //logout
                .logout()
                .logoutUrl("/do_logout")
                .logoutSuccessUrl("/login")
                .deleteCookies("JSESSIONID");
        
        //        http
        //            .requestMatcher(EndpointRequest.toAnyEndpoint())
        //            .authorizeRequests()
        //            .antMatchers("/**").authenticated()
        //            .and()
        //            .logout().logoutUrl("/logout").logoutSuccessUrl("/logout_success")
        //            .invalidateHttpSession(true).clearAuthentication(true)
        //            .and().httpBasic().disable();
    }
}
