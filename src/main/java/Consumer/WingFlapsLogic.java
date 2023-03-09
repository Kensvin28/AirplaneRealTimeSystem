package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class WingFlapsLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;
    WingFlaps wingFlaps;
    Altimeter altimeter;
    // wing flap angle rate of change
    final int DELTA = 30;

    public WingFlapsLogic(WingFlaps wingFlaps, Altimeter altimeter) {
        this.wingFlaps = wingFlaps;
        this.altimeter = altimeter;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void moveFlaps(int newAngle) {
        // brake
        if (newAngle == -90) {
            wingFlaps.setAngle(-90);
            System.out.println("[WING FLAPS] Braking...");
        }

        // change angle
        if (wingFlaps.getAngle() < newAngle) {
            wingFlaps.changeAngle(DELTA);
        } else if (wingFlaps.getAngle() > newAngle) {
            wingFlaps.changeAngle(-DELTA);
        }

        transmit(wingFlaps.getAngle());
        changeAltitude(wingFlaps.getAngle());
    }

    private void changeAltitude(int angle) {
        int altitudeChange = 0;
        // No altitude change when touchdown
        if (altimeter.getAltitude() == 0) {
            return;
        }

        switch (angle) {
            case 60 -> altitudeChange = 1000;
            case 30 -> altitudeChange = 500;
            case 0 -> altitudeChange = 0;
            case -30 -> altitudeChange = -500;
            case -60 -> altitudeChange = -1000;
        }
        System.out.println("[WING FLAPS] Change altitude by " + altitudeChange);
        altimeter.setAltitude(altitudeChange);
    }

    public void transmit(int wingFlapsAngle) {
        try (Connection con = cf.newConnection();
             Channel channel = con.createChannel()) {
            channel.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, Key.WING_FLAPS.name, false, null, String.valueOf(wingFlapsAngle).getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.WING_FLAPS.name);
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