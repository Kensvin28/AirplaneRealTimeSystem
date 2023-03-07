package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
import Producer.Speedometer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Engine {
    Speedometer speedometer;
    Altimeter altimeter;
    int throttle = 75;

    public Engine(Speedometer speedometer, Altimeter altimeter) {
        this.speedometer = speedometer;
        this.altimeter = altimeter;
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int newThrottle) {
        if(throttle+newThrottle<-100){
            throttle = -100;
        } else {
            throttle += newThrottle;
        }
    }

}
