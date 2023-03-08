package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class SpeedometerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Speedometer speedometer;

    public SpeedometerLogic(Speedometer speedometer){
        this.speedometer = speedometer;
    }

    @Override
    public void run() {
        transmit(speedometer.getSpeed());
    }

    public void transmit(int speed) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.SPEED.name, false, null, String.valueOf(speed).getBytes());
            System.out.println("[SPEEDOMETER] Speed: " + speed);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
