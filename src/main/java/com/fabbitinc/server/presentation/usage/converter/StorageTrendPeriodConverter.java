package com.fabbitinc.server.presentation.usage.converter;

import com.fabbitinc.server.application.usage.model.StorageTrendPeriod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StorageTrendPeriodConverter implements Converter<String, StorageTrendPeriod> {

    @Override
    public StorageTrendPeriod convert(String source) {
        return StorageTrendPeriod.from(source);
    }
}
