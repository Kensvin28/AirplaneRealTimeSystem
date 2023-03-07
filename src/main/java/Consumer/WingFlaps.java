package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Altimeter;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class WingFlaps {
    int angle = 0;

    public int getAngle() { return angle; }
    public void setAngle(int angleChange){
        angle += angleChange;
    }

    public WingFlaps(Altimeter altimeter) {

    }
}