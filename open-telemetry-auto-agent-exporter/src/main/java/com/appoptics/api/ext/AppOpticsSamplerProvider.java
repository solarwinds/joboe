package com.appoptics.api.ext;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(ConfigurableSamplerProvider.class)
public class AppOpticsSamplerProvider implements ConfigurableSamplerProvider {
    @Override
    public Sampler createSampler(ConfigProperties config) {
        return new AppOpticsSampler();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
