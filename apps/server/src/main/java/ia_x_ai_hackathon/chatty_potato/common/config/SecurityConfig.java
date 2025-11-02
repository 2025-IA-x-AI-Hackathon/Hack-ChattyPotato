package ia_x_ai_hackathon.chatty_potato.common.config;

import ia_x_ai_hackathon.chatty_potato.common.filter.JwtAuthenticationFilter;
import ia_x_ai_hackathon.chatty_potato.common.resolver.UserArgumentResolver;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final UserArgumentResolver userArgumentResolver;

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(userArgumentResolver);
	}


	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cfg = new CorsConfiguration();

		// 모든 오리진/헤더/메서드 허용 (쿠키 미사용 기준)
		cfg.setAllowedOriginPatterns(List.of("*")); // 패턴 사용(와일드카드)
		cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cfg.setAllowedHeaders(List.of("*"));
		cfg.setExposedHeaders(List.of("Location", "Content-Disposition"));
		cfg.setAllowCredentials(false); // 쿠키 안 쓸 때는 false 권장
		cfg.setMaxAge(3600L); // preflight 캐시(초)

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.httpBasic(HttpBasicConfigurer::disable)
				.csrf(CsrfConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/auth/guest").permitAll()
						.anyRequest().authenticated()
				)

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
