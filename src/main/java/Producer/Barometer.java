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
    Altimeter altimeter;
    Random rand = new Random();
    double pressure;

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressureChange) {
        // set lowest pressure limit to 3
        if(pressure < 3) {
            pressure = 3;
        } else{
            pressure += pressureChange;
            System.out.println("[BAROMETER] New Pressure: " + pressure);
        }
    }

    public Barometer(Altimeter altimeter) {
        this.altimeter = altimeter;
        pressure = rand.nextInt(10, 13);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new BarometerLogic(), 0, 1, TimeUnit.SECONDS);
    }
}
