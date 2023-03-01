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
    Random rand = new Random();
    Weather weather;

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather newWeather){
        weather = newWeather;
        System.out.println("[WEATHER] New Weather: " + weather);
    }

    public WeatherSystem() {
        // TODO: change weather at one point
        weather = Weather.values()[(int)(Math.random()*(Weather.values().length))];
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new WeatherSystemLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class WeatherSystemLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run () {
            transmit(String.valueOf(weather));
        }

        public void transmit (String weather){
            try (Connection connection = cf.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
                channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.WEATHER.name, false, null, weather.getBytes());
                System.out.println("[WEATHER SYSTEM] Weather: " + weather);
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
