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

public class Barometer {
    Random rand = new Random();
    int pressure;

    public int getPressure() {
        return pressure;
    }

    public void setPressure(int pressureChange) {
        if (pressureChange != 0) {
            pressure += pressureChange;
            System.out.println("[BAROMETER] New Pressure: " + pressure);
        }
    }

    public Barometer() {
        pressure = rand.nextInt(10, 13);
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new BarometerLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class BarometerLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run() {
            transmit(pressure);
        }

        public void transmit(int pressure) {
            try (Connection connection = cf.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
                channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.PRESSURE.name, false, null, String.valueOf(pressure).getBytes());
                System.out.println("[BAROMETER] Cabin Pressure: " + pressure);
                Thread.sleep(100);
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
