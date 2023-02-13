import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Controller {
    public static void main(String[] args) {
        ScheduledExecutorService controller = Executors.newScheduledThreadPool(1);
        controller.scheduleAtFixedRate(new ControllerLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class ControllerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    String sensorControlExchange = "sensorControllerExchange";
    String controlActuatorExchange = "controllerActuatorExchange";

    @Override
    public void run() {
        String message = receive();
        String instruction = handleMessage(message);
        transmit(instruction);
    }

    public String handleFlaps(String message) {
        String flapInstruction;
        switch (message) {
            case "tooHigh" -> flapInstruction = "-90";
            case "slightlyHigh" -> flapInstruction = "-60";
            case "slightlyLow" -> flapInstruction = "60";
            case "tooLow" -> flapInstruction = "90";
            default -> flapInstruction = "do nothing";
        }
        return flapInstruction;
    }

    private String handleMessage(String message) {
        String instruction = handleFlaps(message);
        return instruction;
    }

    private String receive() {
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(sensorControlExchange, "fanout");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, sensorControlExchange, "");
            final CompletableFuture<String> response = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                String message = new String(msg.getBody(), StandardCharsets.UTF_8);
                response.complete(message);
            }, x -> {

            });
            return response.get();
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void transmit(String instruction){
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(controlActuatorExchange, "direct");
            channel.basicPublish(controlActuatorExchange, "", false, null, instruction.getBytes());
            System.out.println("Telling to change flap's angle to " + instruction);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}