package Producer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Altimeter {
    public static void main(String[] args) {
        ScheduledExecutorService altimeter = Executors.newScheduledThreadPool(1);
        altimeter.scheduleAtFixedRate(new AltimeterLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class AltimeterLogic implements Runnable {
    Random rand = new Random();
    String myExchange = "sensorControllerExchange";
    ConnectionFactory cf = new ConnectionFactory();

    public AltimeterLogic() {
    }

    @Override
    public void run() {
        String currentAltitude = getData();
        transmit(currentAltitude);
    }

    public String getData() {
        String altitude = "";
        int currentAltitude = rand.nextInt(30_000, 50_000);
        System.out.println("Altitude: " + currentAltitude);
        if (currentAltitude > 49_000) altitude = "tooHigh";
        else if (currentAltitude > 45_000) altitude = "slightlyHigh";
        else if (currentAltitude > 40_000) altitude = "ok";
        else if (currentAltitude > 35_000) altitude = "slightlyLow";
        else if (currentAltitude >= 30_000) altitude = "tooLow";
        return altitude;
    }

    public void transmit(String altitude) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(myExchange, "fanout");
            channel.basicPublish(myExchange, "", false, null, altitude.getBytes());
            System.out.println("Altitude: " + altitude);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
