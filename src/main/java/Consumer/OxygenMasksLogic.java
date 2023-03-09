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

public class OxygenMasksLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Connection con;
    Channel chan;
    OxygenMasks oxygenMasks;

    public OxygenMasksLogic(OxygenMasks oxygenMasks) {
        this.oxygenMasks = oxygenMasks;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void drop(String instruction) {
        if (instruction.equals("drop")) {
            oxygenMasks.setActive(true);
            System.err.println("[OXYGEN MASKS] MASKS DROPPED");
        }
    }

    public String receive() {
        try {
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(2);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.OXYGEN_MASKS.name);
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
//                        System.out.println("oxygen masks closed");
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
        drop(message);
    }
}
