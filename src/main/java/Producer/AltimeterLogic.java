package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class AltimeterLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Altimeter altimeter;

    public AltimeterLogic(Altimeter altimeter) {
        this.altimeter = altimeter;
    }

    @Override
    public void run() {
        transmit(altimeter.getAltitude());
    }

    public void transmit(int altitude) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.ALTITUDE.name, false, null, String.valueOf(altitude).getBytes());
            System.out.println("[ALTIMETER] Current Altitude: " + altimeter.getAltitude());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
