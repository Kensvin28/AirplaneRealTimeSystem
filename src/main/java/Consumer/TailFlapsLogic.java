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

public class TailFlapsLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    TailFlaps tailFlaps;
    Altimeter altimeter;
    final int DELTA = 30;

    public TailFlapsLogic(TailFlaps tailFlaps, Altimeter altimeter){
        this.tailFlaps = tailFlaps;
        this.altimeter = altimeter;
    }

    @Override
    public void run() {
        String message = receive();
        int newAngle = Integer.parseInt(message);
        moveFlaps(newAngle);
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

    public String receive(){
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.TAIL_FLAPS.name);
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
