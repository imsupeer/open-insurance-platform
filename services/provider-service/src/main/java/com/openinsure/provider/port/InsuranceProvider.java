package com.openinsure.provider.port;

import com.openinsure.provider.domain.ProviderData;
import com.openinsure.provider.domain.ProviderId;
import java.util.List;

public interface InsuranceProvider {
    ProviderId id();
    ProviderData.Summary summary();
    List<ProviderData.Policy> policies();
    List<ProviderData.Claim> claims();
}

