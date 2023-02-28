package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Altimeter {
    Random rand = new Random();
    int altitude;

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitudeChange){
        if(altitudeChange != 0) {
            altitude += altitudeChange;
            System.out.println("[ALTIMETER] New Altitude: " + altitude);
        }
    }

    public Altimeter() {
        altitude = rand.nextInt(30_000, 50_000);
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new AltimeterLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class AltimeterLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run() {
            transmit(altitude);
        }

        public void transmit(int altitude) {
            try (Connection connection = cf.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
                channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.ALTITUDE.name, false, null, String.valueOf(altitude).getBytes());
                System.out.println("[ALTIMETER] Current Altitude: " + altitude);
                Thread.sleep(100);
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}


