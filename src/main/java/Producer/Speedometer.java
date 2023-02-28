package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

public class Speedometer {
    Random rand = new Random();
    int speed;
    final int MAX_SPEED = 600;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speedChange) {
        if(speedChange != 0) {
            // speed limit
            if (speed - speedChange < 0) {
                speed = 0;
            }
            if (speed + speedChange > MAX_SPEED) {
                speed = MAX_SPEED;
            }
            speed += speedChange;
            System.out.println("[SPEEDOMETER] New Speed: " + speed);
        }
    }

    public Speedometer() {
        speed = rand.nextInt(100, 600);
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
        timer.scheduleAtFixedRate(new SpeedometerLogic(), 0, 1, TimeUnit.SECONDS);
    }

    class SpeedometerLogic implements Runnable {
        ConnectionFactory cf = new ConnectionFactory();

        @Override
        public void run() {
            transmit(getSpeed());
        }

        public void transmit(int speed) {
            try (Connection connection = cf.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
                channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.SPEED.name, false, null, String.valueOf(speed).getBytes());
                System.out.println("[SPEEDOMETER] Speed: " + speed);
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
