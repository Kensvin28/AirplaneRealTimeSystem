package Consumer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class LandingGearLogic implements Runnable {
    Connection con;
    Channel chan;
    ConnectionFactory cf = new ConnectionFactory();
    LandingGear landingGear;

    public LandingGearLogic(LandingGear landingGear) {
        this.landingGear = landingGear;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void lower(String instruction) {
        if (instruction.equals("lower") && !landingGear.isActive()) {
            landingGear.setActive(true);
            System.err.println("[LANDING GEAR] Lowering landing gear...");
        }
    }

    public void transmit(boolean landingGearDown) {
        try (Connection con = cf.newConnection();
             Channel channel = con.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.LANDING_GEAR.name, false, null, String.valueOf(landingGearDown).getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.LANDING_GEAR.name);
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
//                        System.out.println("landing gear closed");
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
        lower(message);
        transmit(landingGear.isActive());
    }
}
