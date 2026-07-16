package org.example.wechatilink;

import lombok.extern.slf4j.Slf4j;
import org.example.wechatilink.config.ILinkConfigProperties;
import org.example.wechatilink.service.ILinkBotService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class IlinkApplication {

    private final ILinkBotService botService;

    public IlinkApplication(ILinkBotService botService) {
        this.botService = botService;
    }

    public static void main(String[] args) {
        SpringApplication.run(IlinkApplication.class, args);
    }

    /**
     * 应用启动后自动启动 iLink Bot（可通过 ilink.bot.auto-start=false 禁用）。
     */
    @Bean
    CommandLineRunner startBot(ILinkConfigProperties config) {
        return args -> {
            if (config.isAutoStart()) {
                log.info("🚀 应用已启动，正在初始化 iLink Bot...");
                botService.start();
            } else {
                log.info("iLink Bot 自动启动已禁用（ilink.bot.auto-start=false）");
            }
        };
    }

}