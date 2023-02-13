package Consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Engine {
    public static void main(String[] args) {
        ScheduledExecutorService engine = Executors.newScheduledThreadPool(1);
        engine.scheduleAtFixedRate(new EngineLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class EngineLogic implements Runnable {
    ConnectionFactory factory = new ConnectionFactory();
    private static final String EXCHANGE_NAME = "flight";

    @Override
    public void run() {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, "topic");
            String queueName = channel.queueDeclare().getQueue();

            channel.basicQos(1);

            channel.queueBind(queueName, EXCHANGE_NAME, "speed");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                System.out.println(" [x] Received '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            };

            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
            });
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
