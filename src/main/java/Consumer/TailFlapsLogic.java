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
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;

public class TailFlapsLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Phaser connection;
    Connection con;
    Channel chan;
    Channel chan2;
    TailFlaps tailFlaps;
    Altimeter altimeter;
    final int DELTA = 30;

    public TailFlapsLogic(TailFlaps tailFlaps, Altimeter altimeter, Phaser connection) {
        this.tailFlaps = tailFlaps;
        this.altimeter = altimeter;
        this.connection = connection;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
            chan2 = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void moveFlaps(int newAngle) {
        if (tailFlaps.getAngle() < newAngle) {
            tailFlaps.setAngle(DELTA);
        } else if (tailFlaps.getAngle() > newAngle) {
            tailFlaps.setAngle(-DELTA);
        }
        // TODO: change speed and altitude
        System.out.println("[TAIL FLAPS] Flap angle at " + tailFlaps.getAngle() + "°");
        changeAltitude(tailFlaps.getAngle());
    }

    private void changeAltitude(int angle) {
        int altitudeChange = 0;
        switch (angle) {
            case 60 -> altitudeChange = 2000;
            case 30 -> altitudeChange = 500;
            case 0 -> altitudeChange = 0;
            case -30 -> altitudeChange = -500;
            case -60 -> altitudeChange = -2000;
        }
        System.out.println("[TAIL FLAPS] Change altitude by " + altitudeChange);
        altimeter.setAltitude(altitudeChange);
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.TAIL_FLAPS.name);
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
