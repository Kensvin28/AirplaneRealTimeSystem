package Consumer;

import Controller.Exchange;
import Controller.Key;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class LandingGear {
    boolean active = false;

    public void setActive(boolean state){
        active = state;
    }

    public boolean isActive() {
        return active;
    }

    public LandingGear() {

    }
}
