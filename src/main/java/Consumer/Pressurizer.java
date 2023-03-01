package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Barometer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Pressurizer {
    Barometer barometer;
    boolean active = false;

    private void setActive(boolean state){
        active = state;
    }

    public Pressurizer(Barometer barometer) {
        this.barometer = barometer;
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new PressurizerLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class PressurizerLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run() {
            String message = receive();
            pressurize(message);
        }

        public void pressurize(String instruction) {
            if (instruction.equals("suck")) {
                setActive(true);
                System.out.println("[PRESSURIZER] Pressurising cabin...");
            } else if (instruction.equals("release")) {
                setActive(false);
                System.out.println("[PRESSURIZER] Depressurising cabin...");
            }
            changePressure(active);
        }

        private void changePressure(boolean valve) {
            int pressureChange;
            if (valve) {
                pressureChange = 1;
            } else {
                pressureChange = -1;
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
}