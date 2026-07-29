package com.youkeda.exercise.claw.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class TriggerPolicyFactory {

    private static final Logger log = LoggerFactory.getLogger(TriggerPolicyFactory.class);

    private final ApplicationContext applicationContext;
    private final SkillTriggerPolicy defaultPolicy;

    public TriggerPolicyFactory(ApplicationContext applicationContext,
                                 SkillTriggerPolicy defaultPolicy) {
        this.applicationContext = applicationContext;
        this.defaultPolicy = defaultPolicy;
    }

    public SkillTriggerPolicy getPolicy(String policyName) {
        if (policyName == null || policyName.isBlank()) return defaultPolicy;
        try {
            return applicationContext.getBean(policyName, SkillTriggerPolicy.class);
        } catch (Exception e) {
            log.warn("Trigger policy [{}] not found, falling back to default", policyName);
            return defaultPolicy;
        }
    }
}
