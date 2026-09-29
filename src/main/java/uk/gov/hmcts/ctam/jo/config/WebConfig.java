package uk.gov.hmcts.ctam.jo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.ctam.jo.filters.NoQueryParametersInterceptor;

import java.time.Clock;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final NoQueryParametersInterceptor noQueryParametersInterceptor;

    /** UTC clock for error timestamps; tests replace it with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(noQueryParametersInterceptor).addPathPatterns("/api/v1/reference_data/**");
    }
}
