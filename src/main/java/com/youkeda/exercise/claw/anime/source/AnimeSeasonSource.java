package com.youkeda.exercise.claw.anime.source;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.source.NotificationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeSeasonSource implements NotificationSource {
    private static final Logger log = LoggerFactory.getLogger(AnimeSeasonSource.class);

    @Override
    public String getName() { return "ANIME_SEASON"; }

    @Override
    public boolean supports(CampusConfig config) { return true; }

    @Override
    public void check() {
        log.info("AnimeSeasonSource stub — will be implemented in a later task");
    }
}
