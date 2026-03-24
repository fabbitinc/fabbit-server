package com.fabbitinc.server.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fabbitinc.server.presentation.engineeringchange.dto.request.CreateEngineeringChangeRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncIssuesRequest;
import com.fabbitinc.server.presentation.workitem.dto.response.CommentResponse;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
        schemas.putAll(converters.readAll(CreateEngineeringChangeRequest.class));
        schemas.putAll(converters.readAll(CommentResponse.class));

        OpenAPI openApi = new OpenAPI().components(new Components().schemas(schemas));
        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        assertThat(openApi.getComponents().getSchemas().get("SyncIssuesRequest").getProperties())
                .containsKey("issue_ids")
                .doesNotContainKey("issueIds");
        assertThat(openApi.getComponents().getSchemas().get("CreateEngineeringChangeRequest").getProperties())
                .containsKey("source_issue_id")
                .doesNotContainKey("sourceIssueId");
        assertThat(openApi.getComponents().getSchemas().get("CommentResponse").getProperties())
                .containsKey("target_id")
                .containsKey("is_modified")
                .doesNotContainKey("targetId");
    }

    @Test
    void openApiCustomizerNormalizesWildcardJsonResponsesToApplicationJson() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        OpenAPI openApi = new OpenAPI().path(
                "/api/v1/test",
                new PathItem().get(new Operation().responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().content(new Content().addMediaType(
                                "*/*",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/TestResponse"))
                        )))))
        );

        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        Content content = openApi.getPaths().get("/api/v1/test").getGet().getResponses().get("200").getContent();
        assertThat(content).containsOnlyKeys(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        assertThat(content.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE).getSchema().get$ref())
                .isEqualTo("#/components/schemas/TestResponse");
    }

    @Test
    void openApiCustomizerKeepsWildcardBinaryResponsesUntouched() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        OpenAPI openApi = new OpenAPI().path(
                "/api/v1/export",
                new PathItem().get(new Operation().responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().content(new Content().addMediaType(
                                "*/*",
                                new MediaType().schema(new StringSchema().format("byte"))
                        )))))
        );

        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        Content content = openApi.getPaths().get("/api/v1/export").getGet().getResponses().get("200").getContent();
        assertThat(content).containsOnlyKeys("*/*");
        assertThat(content.get("*/*").getSchema().getFormat()).isEqualTo("byte");
    }

    @Test
    void openApiCustomizerDoesNotRewriteNoContentResponses() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        OpenAPI openApi = new OpenAPI().path(
                "/api/v1/delete",
                new PathItem().delete(new Operation().responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().content(new Content().addMediaType(
                                "*/*",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/VoidLikeResponse"))
                        )))))
        );

        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        Content content = openApi.getPaths().get("/api/v1/delete").getDelete().getResponses().get("204").getContent();
        assertThat(content).isNull();
    }

    @Test
    void openApiCustomizerNormalizesErrorResponsesToApiErrorResponse() {
        JacksonProperties jacksonProperties = new JacksonProperties();
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
        SpringDocConfigProperties springDocConfigProperties = new SpringDocConfigProperties();

        OpenAPI openApi = new OpenAPI().path(
                "/api/v1/test",
                new PathItem().get(new Operation().responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().content(new Content().addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/TestResponse"))
                        )))
                        .addApiResponse("400", new ApiResponse().content(new Content().addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/TestResponse"))
                        )))
                        .addApiResponse("401", new ApiResponse().content(new Content().addMediaType(
                                org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE,
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/TestResponse"))
                        )))))
        );

        config.openApiPropertyNamingCustomizer(jacksonProperties, springDocConfigProperties).customise(openApi);

        Schema<?> successSchema = openApi.getPaths().get("/api/v1/test").getGet().getResponses()
                .get("200")
                .getContent()
                .get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .getSchema();
        Schema<?> badRequestSchema = openApi.getPaths().get("/api/v1/test").getGet().getResponses()
                .get("400")
                .getContent()
                .get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .getSchema();
        Schema<?> unauthorizedSchema = openApi.getPaths().get("/api/v1/test").getGet().getResponses()
                .get("401")
                .getContent()
                .get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .getSchema();

        assertThat(successSchema.get$ref()).isEqualTo("#/components/schemas/TestResponse");
        assertThat(badRequestSchema.get$ref()).isEqualTo("#/components/schemas/ApiErrorResponse");
        assertThat(unauthorizedSchema.get$ref()).isEqualTo("#/components/schemas/ApiErrorResponse");
        assertThat(openApi.getComponents()).isNotNull();
        assertThat(openApi.getComponents().getSchemas()).containsKey("ApiErrorResponse");
        assertThat(openApi.getComponents().getSchemas().get("ApiErrorResponse").getProperties())
                .containsKeys("code", "message");
    }

    private static final class SampleResponse {

        private String sampleValue;

        public String getSampleValue() {
            return sampleValue;
        }
    }
}
