package com.trustflow.cms_risk_service.core.rule.application;

import com.trustflow.cms_risk_service.core.common.exception.NotFoundException;
import com.trustflow.cms_risk_service.core.rule.application.port.in.GetRuleChangeHistoryUseCase;
import com.trustflow.cms_risk_service.core.rule.application.port.out.RuleQueryRepository;
import com.trustflow.cms_risk_service.core.rule.application.port.out.UserInfoRepository;
import com.trustflow.cms_risk_service.core.security.UserContext;
import com.trustflow.cms_risk_service.core.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRuleChangeHistoryService implements GetRuleChangeHistoryUseCase {
    private static final String HISTORY_NOT_FOUND_MESSAGE = "Запись истории изменений не найдена";
    private static final String UNKNOWN_AUTHOR_NAME = "Unknown user";

    private final RuleQueryRepository ruleQueryRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    public RuleChangeHistoryResult getRuleChangeHistory(int page, int pageSize, String query) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than or equal to 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than or equal to 1");
        }

        UserContext userContext = UserContextHolder.getRequired();
        String normalizedQuery = normalizeQuery(query);
        RuleChangeHistoryPageResult pageResult = ruleQueryRepository.findChangeHistoryByCompanyId(
                userContext.companyId(),
                normalizedQuery,
                page,
                pageSize
        );

        Map<UUID, String> authorNames = new HashMap<>();
        return new RuleChangeHistoryResult(
                pageResult.items().stream()
                        .map(item -> new RuleChangeHistoryItemResult(
                                item.id(),
                                item.ruleId(),
                                item.changedAt(),
                                item.ruleName(),
                                item.description(),
                                resolveAuthorName(item.authorId(), userContext.accessToken(), authorNames)
                        ))
                        .toList(),
                pageResult.hasMore()
        );
    }

    @Override
    public RuleChangeHistoryDetailsResult getRuleChangeHistoryDetails(UUID historyId) {
        UserContext userContext = UserContextHolder.getRequired();
        RuleChangeHistoryDetailsResult details = ruleQueryRepository.findChangeHistoryDetailsByIdAndCompanyId(
                        historyId,
                        userContext.companyId()
                )
                .orElseThrow(() -> new NotFoundException(HISTORY_NOT_FOUND_MESSAGE));

        return new RuleChangeHistoryDetailsResult(
                details.id(),
                details.companyId(),
                details.ruleId(),
                details.ruleName(),
                details.description(),
                details.authorId(),
                resolveAuthorName(details.authorId(), userContext.accessToken(), new HashMap<>()),
                details.condition(),
                details.categoryId(),
                details.riskObjectId(),
                details.priority(),
                details.responsibleUserId(),
                details.actions(),
                details.enabled(),
                details.mechanismScriptName(),
                details.mechanismScriptContent(),
                details.createdByUserId(),
                details.savedAt(),
                details.changedAt()
        );
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveAuthorName(UUID authorId, String accessToken, Map<UUID, String> cache) {
        if (authorId == null) {
            return UNKNOWN_AUTHOR_NAME;
        }
        return cache.computeIfAbsent(authorId, key -> userInfoRepository.findAuthorName(key, accessToken));
    }
}
