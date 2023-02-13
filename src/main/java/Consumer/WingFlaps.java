package Consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class WingFlaps {
    int angle = 0;

    public void setFlapAngle(int flapAngle) {
        this.angle = flapAngle;
    }

    public static void main(String[] args) {
        ScheduledExecutorService wingFlapsActuator = Executors.newScheduledThreadPool(1);
        WingFlaps wingFlaps = new WingFlaps();
        wingFlapsActuator.scheduleAtFixedRate(new WingFlapsLogic(wingFlaps), 0, 3, TimeUnit.SECONDS);
    }
}

class WingFlapsLogic implements Runnable {
    ConnectionFactory cf = new ConnectionFactory();
    private static final String EXCHANGE_NAME = "controllerActuatorExchange";
    WingFlaps wingFlaps;

    public WingFlapsLogic(WingFlaps wingFlaps) {
        this.wingFlaps = wingFlaps;
    }

    public void setAngle(int newAngle) {
        while(wingFlaps.angle != newAngle){
            if (wingFlaps.angle < newAngle) {
                wingFlaps.setFlapAngle(wingFlaps.angle + 5);
                System.out.println("FLAP: Flap up to " + wingFlaps.angle + "°");
            } else {
                wingFlaps.setFlapAngle(wingFlaps.angle - 5);
                System.out.println("FLAP: Flap down to " + wingFlaps.angle + "°");
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void run() {
        try {
            Connection con = cf.newConnection();
            Channel chan = con.createChannel();
            chan.exchangeDeclare(EXCHANGE_NAME, "direct");
            String qName = chan.queueDeclare().getQueue();
            chan.basicQos(1);
            chan.queueBind(qName, EXCHANGE_NAME, "");
            chan.basicConsume(qName, (x, msg) -> {
                String message = new String(msg.getBody(), StandardCharsets.UTF_8);
                if (!message.equals("do nothing")) {
                    int newAngle = Integer.parseInt(message);
                    setAngle(newAngle);
                } else {
                    System.out.println("FLAP: No changes");
                }
            }, x -> {

            });
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}

