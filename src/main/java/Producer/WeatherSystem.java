package Producer;

import Controller.Exchange;
import Controller.Key;
import Controller.Weather;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherSystem {
    String weather;

    public String getWeather() {
        return weather;
    }

    public void setWeather(Weather newWeather){
//        weather = newWeather;
//        System.out.println("[WEATHER] New Weather: " + weather);
    }

    public WeatherSystem() {
        weather = String.valueOf(Weather.values()[(int)(Math.random()*(Weather.values().length))]);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new WeatherSystemLogic(), 0, 1, TimeUnit.SECONDS);
    }
}