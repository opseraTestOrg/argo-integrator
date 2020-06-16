package com.opsera.integrator.argo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * All swagger config reside here
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    /**
     * Triggers the scan of controllers ro show in swagger
     * @return
     */
    @Bean
    public Docket postsApi() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.opsera.integrator.argo.controller"))
                .paths(PathSelectors.any()).build().apiInfo(apiInfo());
    }

    /**
     * API documentation
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Opsera Argo Integrator API")
                .description("Opsera API for integrating with Argo tool")
                .termsOfServiceUrl("https://opsera.io/legal.html").version("1.0").build();
    }
}
