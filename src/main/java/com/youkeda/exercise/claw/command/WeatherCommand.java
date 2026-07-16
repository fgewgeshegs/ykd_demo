package com.youkeda.exercise.claw.command;

import com.youkeda.exercise.claw.config.WeatherConfig;
import com.youkeda.exercise.claw.exception.ClawException;
import com.youkeda.exercise.claw.model.WeatherResponse;
import com.youkeda.exercise.claw.tool.WeatherTool;

/**
 * weather 命令
 * 查询指定城市的天气信息
 */
public class WeatherCommand implements CommandHandler {

    private final WeatherTool weatherTool;

    public WeatherCommand(WeatherConfig weatherConfig) {
        this.weatherTool = new WeatherTool(weatherConfig);
    }

    @Override
    public void execute(String[] args) throws ClawException {
        // 校验参数：需要提供城市名称
        if (args == null || args.length < 2 || args[1] == null || args[1].trim().isEmpty()) {
            throw new ClawException("请输入城市名称");
        }

        String city = args[1].trim();

        // 调用 WeatherTool 查询天气
        WeatherResponse response = weatherTool.queryWeather(city);

        // 格式化输出
        System.out.println(response);
    }
}
