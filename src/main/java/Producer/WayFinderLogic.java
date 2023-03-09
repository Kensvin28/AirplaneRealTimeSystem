package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class WayFinderLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    WayFinder wayFinder;

    public WayFinderLogic(WayFinder wayFinder) {
        this.wayFinder = wayFinder;
    }

    @Override
    public void run() {
        transmit(wayFinder.getDirection());
    }

    public void transmit(int direction) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.DIRECTION.name, false, null, String.valueOf(direction).getBytes());
            System.out.println("[WAYFINDER] Current Direction: " + wayFinder.getDirection() + "°");
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
