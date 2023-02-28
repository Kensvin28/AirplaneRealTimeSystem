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
import java.util.concurrent.*;

public class Engine {
    Speedometer speedometer;
    Altimeter altimeter;
    int throttle = 75;
    final int DELTA = 25;

    public Engine(Speedometer speedometer, Altimeter altimeter) {
        this.speedometer = speedometer;
        this.altimeter = altimeter;
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new EngineLogic(), 0, 1, TimeUnit.SECONDS);
    }

    private void setThrottle(int newThrottle) {
        throttle += newThrottle;
    }

    class EngineLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        public void changeThrottle(int newThrottle) {
            if (throttle < newThrottle) {
                setThrottle(DELTA);
            } else if (throttle > newThrottle) {
                setThrottle(-DELTA);
            }

            //TODO: Speed, Altitude, Pressure, Thermometer
            System.out.println("[ENGINE] Engine throttle at " + throttle + "%");
            changeSpeed(throttle);
            changeAltitude(throttle);
        }

        private void changeAltitude(int throttle) {
            int altitudeChange = 0;
            if (throttle >= 90) altitudeChange = 1000;
            else if (throttle >= 75) altitudeChange = 500;
            else if (throttle >= 50) altitudeChange = 0;
            else if (throttle >= 25) altitudeChange = -500;
            else if (throttle > 0) altitudeChange = -1000;
            System.out.println("[ENGINE] Change altitude by " + altitudeChange);
            altimeter.setAltitude(altitudeChange);
        }

        private void changePressure(int throttle) {
        }

        private void changeTemperature(int throttle) {
        }

        private void changeSpeed(int throttle) {
            int acceleration;
            if (throttle >= 90) acceleration = 50;
            else if (throttle >= 75) acceleration = 25;
            else if (throttle >= 50) acceleration = 0;
            else if (throttle >= 25) acceleration = -25;
            else if (throttle > 0) acceleration = -50;
            else acceleration = -100;

            System.out.println("[ENGINE] Change speed by " + acceleration);
            speedometer.setSpeed(acceleration);
        }

        public void transmit(String message, String change, String key) {
            try (Connection connection = cf.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(Exchange.ACTUATOR_SENSOR_EXCHANGE.name, "direct");
                channel.basicPublish(Exchange.ACTUATOR_SENSOR_EXCHANGE.name, key, false, null, change.getBytes());
                System.out.println(message + change);
                Thread.sleep(100);
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException(e);
            }
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
}
