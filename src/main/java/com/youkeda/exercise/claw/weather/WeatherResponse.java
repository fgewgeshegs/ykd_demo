package com.youkeda.exercise.claw.weather;

import lombok.Data;

/**
 * 天气响应数据模型
 * 封装从天气 API 获取的天气信息
 */
@Data
public class WeatherResponse {

    private String city;
    private String weather;
    private double temperature;
    private int humidity;

    public WeatherResponse() {
    }

    public WeatherResponse(String city, String weather, double temperature, int humidity) {
        this.city = city;
        this.weather = weather;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    @Override
    public String toString() {
        return "城市：" + city + "\n" +
               "天气：" + weather + "\n" +
               "温度：" + Math.round(temperature) + "℃\n" +
               "湿度：" + humidity + "%";
    }
}
