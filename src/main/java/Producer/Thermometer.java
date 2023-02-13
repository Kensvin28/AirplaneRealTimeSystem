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

public class Thermometer {
    public static void main(String[] args) {
        ScheduledExecutorService thermometer = Executors.newScheduledThreadPool(1);
        thermometer.scheduleAtFixedRate(new ThermometerLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class ThermometerLogic implements Runnable {
    Random rand = new Random();
    String EXCHANGE_NAME = "sensorControllerExchange";
    ConnectionFactory cf = new ConnectionFactory();

    public ThermometerLogic() {
    }

    @Override
    public void run() {
        String currentTemperature = getTemperature();
        transmit(currentTemperature);
    }

    public String getTemperature() {
        String temperature = "";
        int currentTemperature = rand.nextInt(15, 40);
        System.out.println("Temperature: " + currentTemperature);
        if (currentTemperature > 40) temperature = "tooHigh";
        else if (currentTemperature > 30) temperature = "slightlyHigh";
        else if (currentTemperature > 25) temperature = "ok";
        else if (currentTemperature > 20) temperature = "slightlyLow";
        else if (currentTemperature >= 15) temperature = "tooLow";
        return temperature;
    }

    public void transmit(String temperature) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.basicPublish(EXCHANGE_NAME, "", false, null, temperature.getBytes());
            System.out.println("Temperature: " + temperature);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
