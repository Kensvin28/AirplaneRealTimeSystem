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
import java.util.concurrent.TimeoutException;

public class PressurizerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Barometer barometer;
    Pressurizer pressurizer;
    final int DELTA = 1;

    public PressurizerLogic(Pressurizer pressurizer, Barometer barometer) {
        this.pressurizer = pressurizer;
        this.barometer = barometer;
    }

    @Override
    public void run() {
        String message = receive();
        pressurize(message);
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
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.PRESSURIZER.name);
            final CompletableFuture<String> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                messageResponse.complete(new String(msg.getBody(), StandardCharsets.UTF_8));
            }, x -> {

            });
            return messageResponse.get();
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
