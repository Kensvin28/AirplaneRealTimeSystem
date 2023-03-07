package Controller;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class Controller {
    public Controller() {
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(10);
//        timer.scheduleAtFixedRate(new ControllerLogic(), 0, 1, TimeUnit.SECONDS);
    }
}