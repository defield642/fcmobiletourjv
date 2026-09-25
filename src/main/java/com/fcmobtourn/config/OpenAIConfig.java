package com.fcmobtourn.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class OpenAIConfig implements WebMvcConfigurer {
 public void addCorsMappings(CorsRegistry r){ r.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("GET","POST","PUT","DELETE","OPTIONS"); }
}
