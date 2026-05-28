package com.trustflow.cms_risk_service.infrastructure.monitoring;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptEngineEvaluatorTest {
    private final ScriptEngineEvaluator evaluator = new ScriptEngineEvaluator();

    @Test
    void evaluate_executesGroovyExpressionWithContext() {
        Object result = evaluator.evaluate(
                "amount > threshold",
                Map.of("amount", 1500, "threshold", 1000)
        );

        assertThat(result).isEqualTo(true);
    }

    @Test
    void evaluate_returnsComputedValue() {
        Object result = evaluator.evaluate(
                "amount * factor",
                Map.of("amount", 4, "factor", 5)
        );

        assertThat(result).isEqualTo(20);
    }
}
