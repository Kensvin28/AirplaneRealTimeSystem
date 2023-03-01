package Consumer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class LandingGear {
    boolean active = false;

    private void setActive(boolean state){
        active = state;
    }

    public LandingGear() {
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new landingGearLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class landingGearLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run() {
            String message = receive();
            lower(message);
        }

        public void lower(String instruction) {
            if (instruction.equals("lower")) {
                setActive(true);
                System.out.println("[LANDING GEAR] Lowering landing gear...");
            }
        }

        public String receive(){
            try {
                Connection con = cf.newConnection();
                Channel chan = con.createChannel();
                chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
                String qName = chan.queueDeclare().getQueue();
                chan.basicQos(1);
                chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.LANDING_GEAR.name);
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
}
