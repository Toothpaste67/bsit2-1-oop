package com.liceo.campustrack.config;

import jakarta.servlet.DispatcherType;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomSuccessHandler successHandler) throws Exception {
        http
                // CSRF protection stays ON: forms get the token from th:action, fetch() reads it from the <meta> tags
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/login", "/register", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/faculty/**").hasRole("FACULTY")
                        // The scanner page and every scanning API are for students only
                        .requestMatchers("/student/**", "/attendance/**").hasRole("STUDENT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The main port is HTTPS (phones need it for the camera). This adds a plain HTTP port
     * that only accepts connections from this computer, for http://localhost and the desktop window.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> localHttpConnector(
            @Value("${campustrack.http-port:0}") int httpPort,
            @Value("${server.ssl.enabled:false}") boolean sslEnabled) {
        return factory -> {
            if (!sslEnabled || httpPort <= 0) return;
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setPort(httpPort);
            connector.setProperty("address", "127.0.0.1");
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
