package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
import Producer.Speedometer;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class EngineLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Speedometer speedometer;
    Connection con;
    Channel chan;

    Engine engine;
    // throttle rate of change
    final int DELTA = 25;

    public EngineLogic(Engine engine, Speedometer speedometer, Altimeter altimeter) {
        this.engine = engine;
        this.speedometer = speedometer;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeThrottle(int newThrottle) {
        // set max braking throttle to -100%
        if (engine.getThrottle() + newThrottle < -100) {
            engine.setThrottle(-100);
        }
        // increase throttle
        else if (engine.getThrottle() < newThrottle) {
            engine.setThrottle(DELTA);
            // decrease throttle
        } else if (engine.getThrottle() > newThrottle) {
            engine.setThrottle(-DELTA);
        }

        transmit(engine.getThrottle());
        changeSpeed(engine.getThrottle());
    }

    private void changeSpeed(int throttle) {
        int acceleration = 0;
        if (throttle >= 90) acceleration = 50;
        else if (throttle >= 75) acceleration = 25;
        else if (throttle >= 50) acceleration = 0;
        else if (throttle >= 25) acceleration = -25;
        else if (throttle > 0) acceleration = -50;
        else if (throttle <= -50) acceleration = -100;

        System.out.println("[ENGINE] Change speed by " + acceleration);
        speedometer.setSpeed(acceleration);
    }

    public void transmit(int throttle) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.ENGINE.name, false, null, String.valueOf(throttle).getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.ENGINE.name);
            final CompletableFuture<String> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                // stop consuming
                if (msg.getEnvelope().getRoutingKey().contains("off")) {
                    try {
                        if (chan.isOpen()) {
                            chan.close();
                        }
                        if (con.isOpen()) {
                            con.close();
                        }
                    } catch (TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }

                messageResponse.complete(new String(msg.getBody(), StandardCharsets.UTF_8));
            }, x -> {

            });
            return messageResponse.get();
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        String message = receive();
        int newThrottle = Integer.parseInt(message);
        changeThrottle(newThrottle);
    }
}
