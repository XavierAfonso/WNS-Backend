package ch.heigvd.wns.security;


import ch.heigvd.wns.security.jwt.JWTAuthenticationFilter;
import ch.heigvd.wns.security.jwt.JWTLoginFilter;
import ch.heigvd.wns.security.jwt.UserDetailsServiceImp;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The JWT configuration are inspired by an excellent article: https://dzone.com/articles/securing-spring-boot-with-jwts
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImp();
    };

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // disable caching
        http.headers().cacheControl();

        http.csrf().disable() // disable csrf for our requests.
        .authorizeRequests()
        .antMatchers("/").permitAll()
        .antMatchers(HttpMethod.POST,"/users/signin").permitAll()
        .antMatchers(HttpMethod.POST,"/users/signup").permitAll()
        .anyRequest().authenticated()
        .and()
        // We filter the api/login requests
        .addFilterBefore(new JWTLoginFilter("/users/signin", authenticationManager()), UsernamePasswordAuthenticationFilter.class)
        // And filter other requests to check the presence of JWT in header
        .addFilterBefore(new JWTAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());

        // Create a default account
        //auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
       /* auth.inMemoryAuthentication()
                .withUser("admin")
                .password("password")
                .roles("ADMIN");*/
    }

}
