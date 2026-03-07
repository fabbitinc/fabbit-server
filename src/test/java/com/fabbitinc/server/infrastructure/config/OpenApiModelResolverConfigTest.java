package com.fabbitinc.server.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fabbitinc.server.application.issue.dto.request.CreateChangeRequestRequest;
import com.fabbitinc.server.application.issue.dto.request.SyncIssuesRequest;
import com.fabbitinc.server.application.issue.dto.response.CommentResponse;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;

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

    @Test
    void openApiCustomizerNormalizesIssuePrefixedPropertiesWithoutTouchingValidIsProperties() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        ModelResolver modelResolver = config.modelResolver(jacksonProperties, springDocConfigProperties);
        ModelConverters converters = new ModelConverters();
        converters.addConverter(modelResolver);

        Map<String, Schema> schemas = new LinkedHashMap<>(converters.readAll(SyncIssuesRequest.class));
        schemas.putAll(converters.readAll(CreateChangeRequestRequest.class));
        schemas.putAll(converters.readAll(CommentResponse.class));

        OpenAPI openApi = new OpenAPI().components(new Components().schemas(schemas));
        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        assertThat(openApi.getComponents().getSchemas().get("SyncIssuesRequest").getProperties())
                .containsKey("issue_ids")
                .doesNotContainKey("issueIds");
        assertThat(openApi.getComponents().getSchemas().get("CreateChangeRequestRequest").getProperties())
                .containsKey("issue_number")
                .doesNotContainKey("issueNumber");
        assertThat(openApi.getComponents().getSchemas().get("CommentResponse").getProperties())
                .containsKey("issue_id")
                .containsKey("is_modified")
                .doesNotContainKey("issueId");
    }

    private static final class SampleResponse {

        private String sampleValue;

        public String getSampleValue() {
            return sampleValue;
        }
    }
}
