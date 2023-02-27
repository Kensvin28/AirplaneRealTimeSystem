package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Altimeter implements Runnable {
    Random rand = new Random();
    ConnectionFactory cf = new ConnectionFactory();
    volatile int altitude = Integer.MIN_VALUE;

    public Altimeter() {
    }

    @Override
    public void run() {
        transmit(getAltitude());
        setAltitude();
    }

    public int getAltitude() {
        if (altitude == Integer.MIN_VALUE) {
            altitude = rand.nextInt(30_000, 50_000);
        }
        return altitude;
    }

    public void setAltitude() {
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(Exchange.ACTUATOR_SENSOR_EXCHANGE.name, "direct");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, Exchange.ACTUATOR_SENSOR_EXCHANGE.name, Key.ALTITUDE.name);
            String ctag = chan.basicConsume(qName, true, (x, msg) -> {
                String message = new String(msg.getBody(), StandardCharsets.UTF_8);
                int altitudeChange = Integer.parseInt(message);
                if(altitudeChange != 0) {
                    // TODO: Still not updating the altitude
                    altitude = altitude + altitudeChange;
                    System.out.println("[ALTIMETER] New Altitude: " + altitude);
                }
            }, x -> {

            });
            chan.basicCancel(ctag);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
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
