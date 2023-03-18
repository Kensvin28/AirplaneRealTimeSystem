package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class BarometerLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    Barometer barometer;
    Altimeter altimeter;

    public BarometerLogic(Barometer barometer, Altimeter altimeter) {
        this.altimeter = altimeter;
        this.barometer = barometer;
    }

    @Override
    public void run() {
        changePressure();
        transmit(barometer.getPressure());
    }

    // algorithm to determine if the pressure outside would increase or decrease cabin pressure
    public void changePressure(){
        int altitude = altimeter.getAltitude();
        double outsidePressure = (50000 - altitude)*12.5/50000;
        if (outsidePressure > barometer.getPressure()) {
            barometer.setPressure(0.5);
        } else {
            barometer.setPressure(-0.5);
        }
    }

    public void transmit(double pressure) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, Key.PRESSURE.name, false, null, String.valueOf(pressure).getBytes());
            System.out.println("[BAROMETER] Cabin Pressure: " + pressure);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
