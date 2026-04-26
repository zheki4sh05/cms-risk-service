package com.trustflow.cms_risk_service.infrastructure.monitoring;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScriptEngineEvaluator {
    public Object evaluate(String script, Map<String, Object> contextVariables) {
        Binding binding = new Binding();
        contextVariables.forEach(binding::setVariable);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(script);
    }
}
