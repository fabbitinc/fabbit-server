package com.fabbitinc.server.presentation.project.converter;

import com.fabbitinc.server.application.activity.dto.response.ActivityScope;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ActivityScopeConverter implements Converter<String, ActivityScope> {

    @Override
    public ActivityScope convert(String source) {
        return ActivityScope.from(source);
    }
}
