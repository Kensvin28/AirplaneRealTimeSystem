package Producer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.*;

public class Altimeter {
    Random rand = new Random();
    int altitude;

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitudeChange){
        if(altitudeChange != 0) {
            if(altitude+altitudeChange<0){
                altitude = 0;
            } else {
                altitude += altitudeChange;
                System.out.println("[ALTIMETER] New Altitude: " + altitude);
            }
        }
    }

    public Altimeter() {
        altitude = rand.nextInt(30_000, 50_000);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new AltimeterLogic(), 0, 1, TimeUnit.SECONDS);
    }
}

