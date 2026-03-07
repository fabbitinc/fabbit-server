package com.fabbitinc.server.infrastructure.config;

import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.jackson.ModelResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiModelResolverConfigTest {

    private final OpenApiModelResolverConfig config = new OpenApiModelResolverConfig();

    @Test
    void modelResolverAppliesConfiguredPropertyNamingStrategy() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        ModelResolver modelResolver = config.modelResolver(jacksonProperties, springDocConfigProperties);
        List<String> propertyNames = modelResolver.objectMapper()
                .getSerializationConfig()
                .introspect(modelResolver.objectMapper().constructType(SampleResponse.class))
                .findProperties()
                .stream()
                .map(BeanPropertyDefinition::getName)
                .toList();

        assertThat(propertyNames).contains("sample_value");
    }

    private static final class SampleResponse {

        private String sampleValue;

        public String getSampleValue() {
            return sampleValue;
        }
    }
}
