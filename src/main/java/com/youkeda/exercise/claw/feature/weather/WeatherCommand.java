package com.youkeda.exercise.claw.feature.weather;

import com.youkeda.exercise.claw.infrastructure.common.ClawException;
import com.youkeda.exercise.claw.application.command.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * weather 命令
 * 查询指定城市的天气信息
 */
@Component
public class WeatherCommand implements CommandHandler {

    private final WeatherService weatherTool;

    public WeatherCommand(WeatherService weatherTool) {
        this.weatherTool = weatherTool;
    }

    @Override
    public void execute(String[] args) throws ClawException {
        // 校验参数：需要提供城市名称
        if (args == null || args.length < 2 || args[1] == null || args[1].trim().isEmpty()) {
            throw new ClawException("请输入城市名称");
        }

        String city = args[1].trim();

        // 调用 WeatherService 查询天气
        WeatherResponse response = weatherTool.queryWeather(city);

        // 格式化输出
        System.out.println(response);
    }
}
