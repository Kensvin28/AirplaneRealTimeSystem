package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherSystemLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    WeatherSystem weatherSystem;
    ScheduledExecutorService timer;

    public WeatherSystemLogic(WeatherSystem weatherSystem){
        this.weatherSystem = weatherSystem;
        timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(weatherSystem::setWeather, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        transmit(weatherSystem.getWeather());
    }

    // stop changing weather for landing
    public void stopWeatherChange() {
        timer.shutdown();
    }

    public void transmit(String weather) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.WEATHER.name, false, null, weather.getBytes());
            System.out.println("[WEATHER SYSTEM] Weather: " + weather);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
