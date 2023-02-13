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

public class Barometer {
    public static void main(String[] args) {
        ScheduledExecutorService barometer = Executors.newScheduledThreadPool(1);
        barometer.scheduleAtFixedRate(new BarometerLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class BarometerLogic implements Runnable {
    Random rand = new Random();
    String myExchange = "sensorControllerExchange";
    ConnectionFactory cf = new ConnectionFactory();

    public BarometerLogic() {
    }

    @Override
    public void run() {
        String currentPressure = getData();
        transmit(currentPressure);
    }

    public String getData() {
        String pressure = "";
        int currentPressure = rand.nextInt(1, 14);
        System.out.println("Pressure: " + currentPressure);
        if (currentPressure > 14) pressure = "tooHigh";
        else if (currentPressure > 12) pressure = "slightlyHigh";
        else if (currentPressure > 11) pressure = "ok";
        else if (currentPressure > 5) pressure = "slightlyLow";
        else if (currentPressure >= 1) pressure = "tooLow";
        return pressure;
    }

    public void transmit(String pressure) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(myExchange, "fanout");
            channel.basicPublish(myExchange, "", false, null, pressure.getBytes());
            System.out.println("Pressure: " + pressure);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

