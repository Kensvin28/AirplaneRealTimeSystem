package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherSystem implements Runnable {
    Random rand = new Random();
    String EXCHANGE_TYPE = "topic";
    ConnectionFactory cf = new ConnectionFactory();
    String weather = "";

    public WeatherSystem() {
    }

    @Override
    public void run() {
        transmit(getWeather());
    }

    public String getWeather() {
        int currentWeather = rand.nextInt(1, 3);
        weather = switch (currentWeather) {
            case 1 -> "rainy";
            case 2 -> "cloudy";
            default -> "sunny";
        };
        return weather;
    }

    public void transmit(String weather) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, EXCHANGE_TYPE);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.WEATHER.name, false, null, weather.getBytes());
            System.out.println("Weather: " + weather);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
