package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class WeatherSystemLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    WeatherSystem weatherSystem;

    public WeatherSystemLogic(WeatherSystem weatherSystem){
        this.weatherSystem = weatherSystem;
    }

    @Override
    public void run() {
        transmit(weatherSystem.getWeather());
    }

    public void transmit(String weather) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.WEATHER.name(), false, null, weather.getBytes());
            System.out.println("[WEATHER SYSTEM] Weather: " + weather);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
