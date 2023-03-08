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
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;

public class EngineLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Speedometer speedometer;
    Connection con;
    Channel chan;
    Phaser connection;

//    Altimeter altimeter;
    Engine engine;
    final int DELTA = 25;

    public EngineLogic(Engine engine, Speedometer speedometer, Altimeter altimeter, Phaser connection) {
        this.engine = engine;
        this.speedometer = speedometer;
        this.connection = connection;
//        this.altimeter = altimeter;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
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
//            chan2.exchangeDeclare(Exchange.SWITCH_OFF_EXCHANGE.name, "fanout");
//            String qName = chan2.queueDeclare().getQueue();
//            chan2.queueBind(qName, Exchange.SWITCH_OFF_EXCHANGE.name, "");
//            chan2.basicConsume(qName, (x, msg) -> {
//                try {
//                    chan.close();
////                    chan2.close();
//                    con.close();
//                }
//                catch (TimeoutException e) {
//
//                }
//            }, x -> {
//
//            });

            chan.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
//            chan.basicQos(1);
            chan.queueBind(qName, Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.ENGINE.name);
            final CompletableFuture<String> messageResponse = new CompletableFuture<>();
            chan.basicConsume(qName, (x, msg) -> {
                if (msg.getEnvelope().getRoutingKey().contains("off")) {
                    try {
                        if(chan.isOpen()) {
                            chan.close();
                        }
                        if(con.isOpen()) {
                            con.close();
                        }
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
        int newThrottle = Integer.parseInt(message);
        changeThrottle(newThrottle);
    }
}
