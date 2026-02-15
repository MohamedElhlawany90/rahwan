package com.blueWhale.Rahwan;


import com.blueWhale.Rahwan.mapconfig.GoogleMapsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class RahwanApplication {

	public static void main(String[] args) {
		SpringApplication.run(RahwanApplication.class, args);
	}

//	@Bean
//	public CorsWebFilter corsWebFilter() {
//		CorsConfiguration config = new CorsConfiguration();
//		config.setAllowedOrigins(List.of("*"));
//		// config.setAllowedOriginPatterns(List.of("*"));  // or use patterns
//		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//		config.setAllowedHeaders(List.of("*"));
//		config.setAllowCredentials(false);    // true only if origins are specific
//		config.setMaxAge(3600L);
//
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", config);
//
//		return new CorsWebFilter(source);
//	}
}
