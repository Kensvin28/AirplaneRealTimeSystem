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
import java.util.concurrent.Phaser;
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
        if (instruction.equals("suck")) {
            pressurizer.setActive(true);
            System.out.println("[PRESSURIZER] Pressurizing cabin...");
        } else if (instruction.equals("release")) {
            pressurizer.setActive(false);
            System.out.println("[PRESSURIZER] Depressurizing cabin...");
        }
        transmit(pressurizer.isActive());
        changePressure(pressurizer.isActive());
    }

    private void changePressure(boolean valve) {
        double pressureChange;
        if (valve) {
            pressureChange = DELTA;
        } else {
            pressureChange = -DELTA;
        }
        System.out.println("[PRESSURIZER] Change pressure by " + pressureChange);
        barometer.setPressure(pressureChange);
    }

    public void transmit(boolean pressurizerState) {
        try (Connection con = cf.newConnection();
             Channel channel = con.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.PRESSURIZER.name, false, null, String.valueOf(pressurizerState).getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive(){
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.PRESSURIZER.name);
            final CompletableFuture<String> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                if (msg.getEnvelope().getRoutingKey().contains("off")) {
                    try {
                        if(chan.isOpen()) {
                            chan.close();
                        }
                        if(con.isOpen()) {
                            con.close();
                        }
                        System.out.println("Pressurizer");
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
