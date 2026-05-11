package com.unionsg.xaccounting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;



import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.unionsg.xaccounting.security.JwtAuthenticationFilter;
import com.unionsg.xaccounting.security.JwtAuthenticationFilter;
import com.unionsg.xaccounting.security.JwtService;
//
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
////       http
////               .csrf(csrf -> csrf.disable())
////               .formLogin(form->form.disable())
////               .httpBasic(basic->basic.disable())
////               .authorizeHttpRequests(auth -> auth
////                       .requestMatchers("/api/auth/**").permitAll()
////                       .anyRequest().authenticated());
////       return http.build();
////
//
////       http
////               .csrf(csrf -> csrf.disable())
////               .cors(cors -> cors.disable())
////               .formLogin(form -> form.disable())
////               .httpBasic(basic -> basic.disable())
////               .authorizeHttpRequests(auth -> auth
////                       .anyRequest().permitAll()
////               );
////       return http.build();
//        http
//                .csrf(csrf-> csrf.disable())
//                .cors(cors->cors.configurationSource(corsConfigurationSource()))
//                .formLogin(form ->form.disable())
//                .httpBasic(basic -> basic.disable())
//                .authorizeHttpRequests(auth->auth
//                        .requestMatchers("/api/auth/**").permitAll()
//                        .anyRequest().permitAll()
//                );
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource(){
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // Allow frontend origins
//        configuration.setAllowedOrigins(Arrays.asList(
//                "http://localhost:8081"
//        ));
//
//        // allow all http methods
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//
//        // allow all headers
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//
//        // allow credentials (cookies, authorization headers)
//        configuration.setAllowCredentials(true);
//
//        // Expose headers to frontend
//        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//
//        // Cache preflight response
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source =  new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//
//        return source;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder(){
//        return new BCryptPasswordEncoder();
//    }
//}



@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors->cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/users/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // allow frontend origins
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();

        // allow frontend origins
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8081", "http://localhost:8080"));

        // allow http methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // cache preflight response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    };

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}