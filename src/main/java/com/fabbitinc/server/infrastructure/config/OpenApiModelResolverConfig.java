package com.fabbitinc.server.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.swagger.v3.core.jackson.ModelResolver;
import java.lang.reflect.Field;
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
        ObjectMapper objectMapper = new ObjectMapperProvider(springDocConfigProperties).jsonMapper().copy();
        applyPropertyNamingStrategy(objectMapper, jacksonProperties.getPropertyNamingStrategy());
        return new ModelResolver(objectMapper).openapi31(springDocConfigProperties.isOpenapi31());
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
}
