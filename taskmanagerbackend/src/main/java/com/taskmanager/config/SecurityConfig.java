package com.taskmanager.config;

import com.taskmanager.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // register + login + error are open to everyone
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/", "/index.html", "/script.js", "/style.css", "/favicon.ico", "/static/**").permitAll()

                // only MANAGER can create/update/delete projects
                .requestMatchers(HttpMethod.POST, "/api/projects/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/projects/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasRole("MANAGER")

                // only MANAGER can create tasks, assign, set priority/deadline
                .requestMatchers(HttpMethod.POST, "/api/tasks/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/tasks/*/assign").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/tasks/*/priority").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/tasks/*/deadline").hasRole("MANAGER")

                // any logged-in user (including DEVELOPER) can update task status and view data
                .requestMatchers(HttpMethod.PUT, "/api/tasks/*/status").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()

                .anyRequest().authenticated()
            )
            // run our JWT filter before Spring's default login filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
