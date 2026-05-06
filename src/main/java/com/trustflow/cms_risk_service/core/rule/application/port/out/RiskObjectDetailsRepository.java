package com.trustflow.cms_risk_service.core.rule.application.port.out;

import com.trustflow.cms_risk_service.core.rule.application.RiskObjectResult;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface RiskObjectDetailsRepository {
    Map<UUID, RiskObjectResult> findByIds(Set<UUID> ids);
}
