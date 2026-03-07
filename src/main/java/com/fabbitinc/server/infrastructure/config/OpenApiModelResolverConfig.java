package com.fabbitinc.server.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiModelResolverConfig {

    @Bean
    public ModelResolver modelResolver(
            JacksonProperties jacksonProperties,
            SpringDocConfigProperties springDocConfigProperties) {
        ObjectMapper objectMapper = createOpenApiObjectMapper(jacksonProperties, springDocConfigProperties);
        return new ModelResolver(objectMapper).openapi31(springDocConfigProperties.isOpenapi31());
    }

    @Bean
    public GlobalOpenApiCustomizer openApiPropertyNamingCustomizer(
            JacksonProperties jacksonProperties,
            SpringDocConfigProperties springDocConfigProperties) {
        ObjectMapper objectMapper = createOpenApiObjectMapper(jacksonProperties, springDocConfigProperties);
        return openApi -> {
            normalizeSwaggerAccessorPrefixBug(openApi, objectMapper);
            normalizeWildcardJsonResponseContentType(openApi);
        };
    }

    private ObjectMapper createOpenApiObjectMapper(
            JacksonProperties jacksonProperties,
            SpringDocConfigProperties springDocConfigProperties) {
        ObjectMapper objectMapper = new ObjectMapperProvider(springDocConfigProperties).jsonMapper().copy();
        applyPropertyNamingStrategy(objectMapper, jacksonProperties.getPropertyNamingStrategy());
        return objectMapper;
    }

    private void applyPropertyNamingStrategy(ObjectMapper objectMapper, String propertyNamingStrategy) {
        if (!StringUtils.hasText(propertyNamingStrategy)) {
            return;
        }

        try {
            Class<?> propertyNamingStrategyClass = ClassUtils.forName(propertyNamingStrategy, null);
            objectMapper.setPropertyNamingStrategy(
                    (PropertyNamingStrategy) BeanUtils.instantiateClass(propertyNamingStrategyClass));
        }
        catch (ClassNotFoundException ex) {
            objectMapper.setPropertyNamingStrategy(resolvePropertyNamingStrategy(propertyNamingStrategy));
        }
    }

    private PropertyNamingStrategy resolvePropertyNamingStrategy(String fieldName) {
        Field field = ReflectionUtils.findField(PropertyNamingStrategies.class, fieldName, PropertyNamingStrategy.class);
        Assert.state(field != null, () -> "Constant named '" + fieldName + "' not found");
        ReflectionUtils.makeAccessible(field);
        try {
            return (PropertyNamingStrategy) field.get(null);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void normalizeSwaggerAccessorPrefixBug(OpenAPI openApi, ObjectMapper objectMapper) {
        if (openApi == null || objectMapper.getPropertyNamingStrategy() == null) {
            return;
        }

        // swagger-core가 issueId/issueIds처럼 "is"로 시작하는 일반 이름을 getter 예외로 오인해
        // 원래의 camelCase member 이름으로 되돌리므로, 최종 OpenAPI 스키마에서 다시 정규화한다.
        Set<Schema<?>> visitedSchemas = Collections.newSetFromMap(new IdentityHashMap<>());
        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            openApi.getComponents().getSchemas().values()
                    .forEach(schema -> normalizeSwaggerAccessorPrefixBug(schema, objectMapper, visitedSchemas));
        }
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values()
                .forEach(pathItem -> normalizeSwaggerAccessorPrefixBug(pathItem, objectMapper, visitedSchemas));
    }

    private void normalizeSwaggerAccessorPrefixBug(
            PathItem pathItem,
            ObjectMapper objectMapper,
            Set<Schema<?>> visitedSchemas) {
        if (pathItem == null) {
            return;
        }

        pathItem.readOperations().forEach(operation -> {
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(parameter -> {
                    normalizeSwaggerAccessorPrefixBug(parameter.getSchema(), objectMapper, visitedSchemas);
                    normalizeSwaggerAccessorPrefixBug(parameter.getContent(), objectMapper, visitedSchemas);
                });
            }
            if (operation.getRequestBody() != null) {
                normalizeSwaggerAccessorPrefixBug(operation.getRequestBody().getContent(), objectMapper, visitedSchemas);
            }
            if (operation.getResponses() != null) {
                operation.getResponses().values()
                        .forEach(response -> normalizeSwaggerAccessorPrefixBug(response.getContent(), objectMapper, visitedSchemas));
            }
        });
    }

    private void normalizeSwaggerAccessorPrefixBug(
            Content content,
            ObjectMapper objectMapper,
            Set<Schema<?>> visitedSchemas) {
        if (content == null) {
            return;
        }

        for (MediaType mediaType : content.values()) {
            normalizeSwaggerAccessorPrefixBug(mediaType.getSchema(), objectMapper, visitedSchemas);
        }
    }

    @SuppressWarnings("unchecked")
    private void normalizeSwaggerAccessorPrefixBug(
            Schema<?> schema,
            ObjectMapper objectMapper,
            Set<Schema<?>> visitedSchemas) {
        if (schema == null || !visitedSchemas.add(schema)) {
            return;
        }

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            Map<String, String> renamedProperties = new LinkedHashMap<>();
            Map<String, Schema> normalizedProperties = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String normalizedPropertyName = normalizeSwaggerAccessorPrefixBug(entry.getKey(), objectMapper);
                normalizedProperties.putIfAbsent(normalizedPropertyName, entry.getValue());
                if (!entry.getKey().equals(normalizedPropertyName)) {
                    renamedProperties.put(entry.getKey(), normalizedPropertyName);
                }
            }
            if (!renamedProperties.isEmpty()) {
                schema.setProperties(normalizedProperties);
                normalizeRequiredPropertyNames(schema, renamedProperties);
            }
            normalizedProperties.values()
                    .forEach(propertySchema -> normalizeSwaggerAccessorPrefixBug(propertySchema, objectMapper, visitedSchemas));
        }

        normalizeSwaggerAccessorPrefixBug(schema.getItems(), objectMapper, visitedSchemas);
        if (schema.getAdditionalProperties() instanceof Schema<?> additionalPropertiesSchema) {
            normalizeSwaggerAccessorPrefixBug(additionalPropertiesSchema, objectMapper, visitedSchemas);
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(item -> normalizeSwaggerAccessorPrefixBug(item, objectMapper, visitedSchemas));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(item -> normalizeSwaggerAccessorPrefixBug(item, objectMapper, visitedSchemas));
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(item -> normalizeSwaggerAccessorPrefixBug(item, objectMapper, visitedSchemas));
        }
        normalizeSwaggerAccessorPrefixBug(schema.getNot(), objectMapper, visitedSchemas);
    }

    private void normalizeRequiredPropertyNames(Schema<?> schema, Map<String, String> renamedProperties) {
        List<String> required = schema.getRequired();
        if (required == null || required.isEmpty()) {
            return;
        }

        List<String> normalizedRequired = new ArrayList<>(required.size());
        for (String propertyName : required) {
            String normalizedPropertyName = renamedProperties.getOrDefault(propertyName, propertyName);
            if (!normalizedRequired.contains(normalizedPropertyName)) {
                normalizedRequired.add(normalizedPropertyName);
            }
        }
        schema.setRequired(normalizedRequired);
    }

    private String normalizeSwaggerAccessorPrefixBug(String propertyName, ObjectMapper objectMapper) {
        if (!isSwaggerAccessorPrefixBugTarget(propertyName)) {
            return propertyName;
        }

        PropertyNamingStrategy propertyNamingStrategy = objectMapper.getPropertyNamingStrategy();
        if (propertyNamingStrategy instanceof PropertyNamingStrategies.NamingBase namingBase) {
            return namingBase.translate(propertyName);
        }

        String normalizedPropertyName =
                propertyNamingStrategy.nameForField(objectMapper.getSerializationConfig(), null, propertyName);
        return StringUtils.hasText(normalizedPropertyName) ? normalizedPropertyName : propertyName;
    }

    private boolean isSwaggerAccessorPrefixBugTarget(String value) {
        return hasUppercaseCharacter(value)
                && (startsWithLowercaseBeanPrefix(value, "is") || startsWithLowercaseBeanPrefix(value, "get"));
    }

    private boolean hasUppercaseCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isUpperCase(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithLowercaseBeanPrefix(String value, String prefix) {
        return value.startsWith(prefix)
                && value.length() > prefix.length()
                && Character.isLowerCase(value.charAt(prefix.length()));
    }

    private void normalizeWildcardJsonResponseContentType(OpenAPI openApi) {
        if (openApi == null || openApi.getPaths() == null) {
            return;
        }

        openApi.getPaths().values().forEach(this::normalizeWildcardJsonResponseContentType);
    }

    private void normalizeWildcardJsonResponseContentType(PathItem pathItem) {
        if (pathItem == null) {
            return;
        }

        pathItem.readOperations().forEach(this::normalizeWildcardJsonResponseContentType);
    }

    private void normalizeWildcardJsonResponseContentType(Operation operation) {
        if (operation == null || operation.getResponses() == null) {
            return;
        }

        operation.getResponses().forEach(this::normalizeWildcardJsonResponseContentType);
    }

    private void normalizeWildcardJsonResponseContentType(String responseCode, ApiResponse response) {
        if (isNoContentResponse(responseCode) || response == null) {
            return;
        }

        Content content = response.getContent();
        if (content == null || content.size() != 1 || !content.containsKey("*/*")) {
            return;
        }

        MediaType wildcardMediaType = content.get("*/*");
        if (wildcardMediaType == null || !hasJsonCompatibleSchema(wildcardMediaType.getSchema())) {
            return;
        }

        Content normalizedContent = new Content();
        normalizedContent.addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, wildcardMediaType);
        response.setContent(normalizedContent);
    }

    private boolean isNoContentResponse(String responseCode) {
        return "204".equals(responseCode) || "205".equals(responseCode) || "304".equals(responseCode);
    }

    private boolean hasJsonCompatibleSchema(Schema<?> schema) {
        return schema != null && !isBinarySchema(schema);
    }

    private boolean isBinarySchema(Schema<?> schema) {
        return "string".equals(schema.getType())
                && ("binary".equals(schema.getFormat()) || "byte".equals(schema.getFormat()));
    }
}
