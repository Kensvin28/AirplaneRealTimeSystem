package Producer;

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
    public static void main(String[] args) {
        ScheduledExecutorService weatherSystem = Executors.newScheduledThreadPool(1);
        weatherSystem.scheduleAtFixedRate(new WeatherSystemLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class WeatherSystemLogic implements Runnable {
    Random rand = new Random();
    String EXCHANGE_NAME = "sensorControllerExchange";
    ConnectionFactory cf = new ConnectionFactory();

    public WeatherSystemLogic() {
    }

    @Override
    public void run() {
        String currentWeather = getWeather();
        transmit(currentWeather);
    }

    public String getWeather() {
        String weather = "";
        int currentWeather = rand.nextInt(1, 3);
        System.out.println("Weather: " + currentWeather);
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
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.basicPublish(EXCHANGE_NAME, "", false, null, weather.getBytes());
            System.out.println("Weather: " + weather);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
