package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Barometer;
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
    Phaser connection;
    Connection con;
    Channel chan;
    Channel chan2;
    Barometer barometer;
    Pressurizer pressurizer;
    final int DELTA = 1;

    public PressurizerLogic(Pressurizer pressurizer, Barometer barometer, Phaser connection) {
        this.pressurizer = pressurizer;
        this.barometer = barometer;
        this.connection = connection;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
            chan2 = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void pressurize(String instruction) {
        if (instruction.equals("suck")) {
            pressurizer.setActive(true);
            System.out.println("[PRESSURIZER] Pressurising cabin...");
        } else if (instruction.equals("release")) {
            pressurizer.setActive(false);
            System.out.println("[PRESSURIZER] Depressurising cabin...");
        }
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

    public String receive(){
        try {
//            chan2.exchangeDeclare(Exchange.SWITCH_OFF_EXCHANGE.name, "fanout");
//            String qName = chan2.queueDeclare().getQueue();
//            chan2.queueBind(qName, Exchange.SWITCH_OFF_EXCHANGE.name, "");
//            chan2.basicConsume(qName, (x, msg) -> {
//                try {
//                    chan.close();
//                    chan2.close();
//                    con.close();
//                }
//                catch (TimeoutException e) {
//
//                }
//            }, x -> {
//
//            });

            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
//            chan.basicQos(1);
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
