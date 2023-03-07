package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
import Producer.Speedometer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class EngineLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Speedometer speedometer;
//    Altimeter altimeter;
    Engine engine;
    final int DELTA = 25;

    public EngineLogic(Engine engine, Speedometer speedometer, Altimeter altimeter) {
        this.engine = engine;
        this.speedometer = speedometer;
//        this.altimeter = altimeter;
    }

    public void changeThrottle(int newThrottle) {
        if(engine.getThrottle()+newThrottle<-100){
            engine.setThrottle(-100);
        }
        else if (engine.getThrottle() < newThrottle) {
            engine.setThrottle(DELTA);
        } else if (engine.getThrottle() > newThrottle) {
            engine.setThrottle(-DELTA);
        }

        System.out.println("[ENGINE] Engine throttle at " + engine.getThrottle() + "%");
        changeSpeed(engine.getThrottle());
//        changeAltitude(engine.getThrottle());
    }

//    private void changeAltitude(int throttle) {
//        int altitudeChange = 0;
//        if (throttle >= 90) altitudeChange = 1000;
//        else if (throttle >= 75) altitudeChange = 500;
//        else if (throttle >= 50) altitudeChange = 0;
//        else if (throttle >= 25) altitudeChange = -500;
//        else if (throttle > 0) altitudeChange = -1000;
//        System.out.println("[ENGINE] Change altitude by " + altitudeChange);
//        altimeter.setAltitude(altitudeChange);
//    }

    private void changeSpeed(int throttle) {
        int acceleration = 0;
        if (throttle >= 90) acceleration = 50;
        else if (throttle >= 75) acceleration = 25;
        else if (throttle >= 50) acceleration = 0;
        else if (throttle >= 25) acceleration = -25;
        else if (throttle > 0) acceleration = -50;
        else if (throttle <= -50) acceleration = -100;

        System.out.println("[ENGINE] Change speed by " + acceleration);
        speedometer.setSpeed(acceleration);
    }

    public String receive() {
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.ENGINE.name);
            final CompletableFuture<String> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                String message = new String(msg.getBody(), StandardCharsets.UTF_8);
                messageResponse.complete(message);
            }, x -> {

            });
            return messageResponse.get();
        } catch (IOException | TimeoutException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        String message = receive();
        int newThrottle = Integer.parseInt(message);
        changeThrottle(newThrottle);
    }
}
