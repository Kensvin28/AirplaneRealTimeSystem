package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
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
    WingFlaps wingFlaps;
    Altimeter altimeter;
    final int DELTA = 30;

    public WingFlapsLogic(WingFlaps wingFlaps, Altimeter altimeter){
        this.wingFlaps = wingFlaps;
        this.altimeter = altimeter;
    }

    @Override
    public void run() {
        String message = receive();
        try {
            int newAngle = Integer.parseInt(message);
            moveFlaps(newAngle);
        } catch(Exception e){
            System.out.println("[WING FLAPS] Braking...");
        }

    }

    public void moveFlaps(int newAngle) {
        if (wingFlaps.getAngle() < newAngle) {
            wingFlaps.setAngle(DELTA);
        } else if (wingFlaps.getAngle() > newAngle) {
            wingFlaps.setAngle(-DELTA);
        }
        // TODO: change speed and altitude
        System.out.println("[WING FLAPS] Flap angle at " + wingFlaps.getAngle() + "°");
        changeAltitude(wingFlaps.getAngle());
    }

    private void changeAltitude(int angle) {
        int altitudeChange = 0;
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

    public String receive(){
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.WING_FLAPS.name);
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