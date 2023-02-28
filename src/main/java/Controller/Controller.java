package Controller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Controller {
    public Controller() {
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(10);
        timer.scheduleAtFixedRate(new ControllerLogic(), 0, 1, TimeUnit.SECONDS);
    }
}

class ControllerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();

    String ENGINE = "engine";
    String PRESSURIZER = "pressurizer";
    String WING_FLAPS = "wingFlaps";

    @Override
    public void run() {
        Delivery message = receive();
        String sender = message.getEnvelope().getRoutingKey();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        handleMessage(body, sender);
    }

    private void handleMessage(String message, String sender) {
        switch (sender) {
            case "altitude" -> {
//                CompletableFuture.runAsync(() -> handleEngine(message));
                CompletableFuture.runAsync(() -> handleFlaps(message));
//                CompletableFuture.runAsync(() -> handleLandingGear(message));
            }
            case "pressure" -> {
                CompletableFuture.runAsync(() -> handlePressurizer(message));
//                CompletableFuture.runAsync(() -> handleOxygenMasks(message));
            }
            case "speed" -> {
                CompletableFuture.runAsync(() -> handleEngine(message));
//                CompletableFuture.runAsync(() -> handleFlaps(message));
            }
            case "weather" -> {
                CompletableFuture.runAsync(() -> handleRudder(message));
//                CompletableFuture.runAsync(() -> handleFlaps(message));
            }
//            case "temperature":
//                CompletableFuture.runAsync(() -> handleEngine(message));
//                CompletableFuture.runAsync(() -> handleFlaps(message));
//                break;
        }
    }

    private void handleRudder(String message) {
    }

    private void handleOxygenMasks(String message) {
    }

    private void handlePressurizer(String message) {
    }

    private void handleLandingGear(String message) {
    }

    private void handleEngine(String message) {
        // TODO: Formulate function that can model various factors
        int speed = Integer.parseInt(message);
        String engineInstruction = "";
        if (speed > 550) engineInstruction = "25";
        else if (speed > 525) engineInstruction = "50";
        else if (speed > 500) engineInstruction = "75";
        else if (speed >= 100) engineInstruction = "100";
        System.out.println("[CONTROLLER] Telling engine to change throttle to " + engineInstruction + "%");
        transmit(engineInstruction, Key.ENGINE.name);
    }

    public void handleFlaps(String message) {
        int altitude = Integer.parseInt(message);
        String flapInstruction = "";
        //TODO: Landing special case
        if (altitude > 49_000) flapInstruction = "-60";
        else if (altitude > 45_000) flapInstruction = "-30";
        else if (altitude > 40_000) return;
        else if (altitude > 35_000) flapInstruction = "30";
        else if (altitude >= 30_000) flapInstruction = "60";
        System.out.println("[CONTROLLER] Telling flap to change its angle to " + flapInstruction + "°");
        transmit(flapInstruction, Key.WING_FLAPS.name);
    }

    private Delivery receive() {
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "#");
            final CompletableFuture<Delivery> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                messageResponse.complete(msg);
            }, x -> {

            });
            return messageResponse.get();
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void transmit(String instruction, String key){
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, key, false, null, instruction.getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}