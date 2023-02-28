package Producer;
import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Barometer implements Runnable {
    Random rand = new Random();
    ConnectionFactory cf = new ConnectionFactory();
    int pressure = Integer.MIN_VALUE;

    public Barometer() {
    }

    @Override
    public void run() {
        transmit(getPressure());
        setPressure();
    }

    private void setPressure() {
    }

    public int getPressure() {
        if (pressure == Integer.MIN_VALUE) {
            pressure = rand.nextInt(10, 13);
        }
        return pressure;
//        System.out.println("Pressure: " + currentPressure);
//        if (currentPressure > 14) pressure = "tooHigh";
//        else if (currentPressure > 12) pressure = "slightlyHigh";
//        else if (currentPressure > 11) pressure = "ok";
//        else if (currentPressure > 5) pressure = "slightlyLow";
//        else if (currentPressure >= 1) pressure = "tooLow";
    }

    public void transmit(int pressure) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.PRESSURE.name, false, null, String.valueOf(pressure).getBytes());
            System.out.println("[BAROMETER] Pressure: " + pressure);
            Thread.sleep(100);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

