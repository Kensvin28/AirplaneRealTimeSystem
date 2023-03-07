package Consumer;

import Controller.Exchange;
import Controller.Key;
import Producer.Barometer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Pressurizer {
    boolean active = false;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean state){
        active = state;
    }

    public Pressurizer(Barometer barometer) {

    }
}