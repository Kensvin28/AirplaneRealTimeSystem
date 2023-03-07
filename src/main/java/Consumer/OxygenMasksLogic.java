package Consumer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class OxygenMasksLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    OxygenMasks oxygenMasks;

    public OxygenMasksLogic(OxygenMasks oxygenMasks) {
        this.oxygenMasks = oxygenMasks;
    }

    @Override
    public void run() {
        String message = receive();
        drop(message);
    }

    public void drop(String instruction) {
        if (instruction.equals("drop")) {
            oxygenMasks.setActive(true);
            System.err.println("[OXYGEN MASKS] MASKS DROPPED");
        }
    }

    public String receive(){
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.OXYGEN_MASKS.name);
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
