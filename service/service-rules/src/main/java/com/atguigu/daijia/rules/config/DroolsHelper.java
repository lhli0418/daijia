package com.atguigu.daijia.rules.config;

import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DroolsHelper {
    // 制定规则文件的路径
    private static final String RULES_CUSTOMER_RULES_DRL = "rules/RewardRule.drl";

    public static KieSession loadForRule(String drlStr) {
         KieServices kieServices = KieServices.Factory.get();
        
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
    kieFileSystem.write(
        ResourceFactory.newClassPathResource(drlStr));
        
        KieBuilder kb = kieServices.newKieBuilder(kieFileSystem);
        kb.buildAll();

        KieModule kieModule = kb.getKieModule();
        KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
        return kieContainer.newKieSession();
    }
}