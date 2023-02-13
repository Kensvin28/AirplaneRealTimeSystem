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

public class Speedometer {
    public static void main(String[] args) {
        ScheduledExecutorService speedometer = Executors.newScheduledThreadPool(1);
        speedometer.scheduleAtFixedRate(new SpeedometerLogic(), 0, 3, TimeUnit.SECONDS);
    }
}

class SpeedometerLogic implements Runnable {
    Random rand = new Random();
    String EXCHANGE_NAME = "sensorControllerExchange";
    ConnectionFactory cf = new ConnectionFactory();

    public SpeedometerLogic() {
    }

    @Override
    public void run() {
        String currentSpeed = getSpeed();
        transmit(currentSpeed);
    }

    public String getSpeed() {
        String speed = "";
        int currentSpeed = rand.nextInt(100, 600);
        System.out.println("Speed: " + currentSpeed);
        if (currentSpeed > 550) speed = "tooFast";
        else if (currentSpeed > 525) speed = "slightlyFast";
        else if (currentSpeed > 500) speed = "ok";
        else if (currentSpeed > 400) speed = "slightlySlow";
        else if (currentSpeed >= 100) speed = "tooSlow";
        return speed;
    }

    public void transmit(String speed) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            channel.basicPublish(EXCHANGE_NAME, "", false, null, speed.getBytes());
            System.out.println("Speed: " + speed);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
