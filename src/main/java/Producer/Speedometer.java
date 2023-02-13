package Producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Speedometer {
    public static void main(String[] argv) {
        ScheduledExecutorService speedometer = Executors.newScheduledThreadPool(1);
        speedometer.scheduleAtFixedRate(new SpeedometerLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class SpeedometerLogic implements Runnable {
    private static final String EXCHANGE_NAME = "flight";

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            String routingKey = "speed";


            String message = "Increase/Decrease";

            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
