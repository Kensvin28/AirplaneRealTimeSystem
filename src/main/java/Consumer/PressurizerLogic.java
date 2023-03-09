package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Barometer;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class PressurizerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;
    Barometer barometer;
    Pressurizer pressurizer;
    final int DELTA = 1;

    public PressurizerLogic(Pressurizer pressurizer, Barometer barometer) {
        this.pressurizer = pressurizer;
        this.barometer = barometer;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void pressurize(String instruction) {
        switch (instruction) {
            case "suck" -> {
                pressurizer.setState("suck");
                System.out.println("[PRESSURIZER] Pressurizing cabin...");
            }
            case "release" -> {
                pressurizer.setState("release");
                System.out.println("[PRESSURIZER] Depressurizing cabin...");
            }
            case "suck maximum" -> {
                pressurizer.setState("suck maximum");
                System.out.println("[PRESSURIZER] Emergency pressurizing cabin...");
            }
        }
        transmit(pressurizer.getState());
        changePressure(pressurizer.getState());
    }

    private void changePressure(String valve) {
        double pressureChange;
        if (valve.equals("suck")) {
            pressureChange = DELTA;
        } else if (valve.equals("suck maximum")) {
            pressureChange = 2*DELTA;
        } else {
            pressureChange = -DELTA;
        }
        System.out.println("[PRESSURIZER] Change pressure by " + pressureChange);
        barometer.setPressure(pressureChange);
    }

    public void transmit(String pressurizerState) {
        try (Connection con = cf.newConnection();
             Channel channel = con.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.PRESSURIZER.name, false, null, pressurizerState.getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.PRESSURIZER.name);
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
//                        System.out.println("pressurizer closed");

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
        pressurize(message);
    }
}
