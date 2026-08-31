package com.agustin.backend_dialysis_record.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("classpath:supabase-jwk.json")
    private Resource jwkResource;

    @Bean
    public JwtDecoder jwtDecoder() throws Exception {
        // Leemos la llave pública desde el archivo estático en resources/
        String jwkJson = new String(jwkResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        
        // Supabase JWKS root tiene un array "keys"
        JWKSet jwkSet = JWKSet.parse(jwkJson);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        
        // Configuramos el procesador nativo de Nimbus para aceptar firmas ES256 (P-256)
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        
        return new NimbusJwtDecoder(jwtProcessor);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://10.0.2.2:8080",
                "https://*.codemagic.app",
                "https://frontend-dialysis-record-system-flutter.onrender.com"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(false);

        // Cache del preflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .cors(withDefaults())
                // 1) CSRF: en APIs REST stateless normalmente se desactiva
                .csrf(csrf -> csrf.disable())

                // 2) SessionManagement: STATELESS = no se guarda sesión de usuario en servidor
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3) Reglas de autorización por rutas
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints públicos
                        .requestMatchers("/ping", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Rutas protegidas
                        .requestMatchers("/api/doctors/**").authenticated()
                        .requestMatchers("/api/patients/**").authenticated()
                        .requestMatchers("/api/sessions/**").authenticated()

                        .anyRequest().authenticated()
                )

                // 4) Habilitar Resource Server para Supabase (OAuth2)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))

                // 5) Construir la cadena final
                .build();
    }
}
