package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.WayFinder;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TailFlapsLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;
    TailFlaps tailFlaps;
    WayFinder wayFinder;

    public TailFlapsLogic(TailFlaps tailFlaps, WayFinder wayFinder) {
        this.tailFlaps = tailFlaps;
        this.wayFinder = wayFinder;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void moveFlaps(int newAngle) {
        tailFlaps.setAngle(newAngle);
        System.out.println("[TAIL FLAPS] Flap angle at " + tailFlaps.getAngle() + "°");
        transmit(tailFlaps.getAngle());
        changeDirection(newAngle);
    }

    private void changeDirection(int directionChange) {
        System.out.println("[TAIL FLAPS] Change direction by " + directionChange + "°");
        wayFinder.setDirection(directionChange);
    }

    public void transmit(int tailFlapsAngle) {
        try (Connection con = cf.newConnection();
             Channel channel = con.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.TAIL_FLAPS.name, false, null, String.valueOf(tailFlapsAngle).getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.TAIL_FLAPS.name);
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
        int newAngle = Integer.parseInt(message);
        moveFlaps(newAngle);
    }
}
