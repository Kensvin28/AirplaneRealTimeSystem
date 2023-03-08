package Controller;

import Consumer.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public abstract class ControllerLogic implements FlightMode {
    ConnectionFactory cf = new ConnectionFactory();
    int altitude;
    double pressure;
    int speed;

    Engine engine;
    LandingGear landingGear;
    OxygenMasks oxygenMasks;
    Pressurizer pressurizer;
    WingFlaps wingFlaps;
    boolean landing = false;
    boolean approaching = false;
    boolean end = false;
    boolean landingGearDown = false;
    Connection con;
    Channel chan;
    Phaser phaser;

    public ControllerLogic(Engine engine, LandingGear landingGear, OxygenMasks oxygenMasks, Pressurizer pressurizer, WingFlaps wingFlaps, Phaser phaser) {
        this.engine = engine;
        this.landingGear = landingGear;
        this.oxygenMasks = oxygenMasks;
        this.pressurizer = pressurizer;
        this.wingFlaps = wingFlaps;
        this.phaser = phaser;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLanding(boolean landing) {
        this.landing = landing;
        if(landing) {
            try {
                System.err.println("Plane is going to land");
                if(chan.isOpen()) {
                    chan.close();
                }
                if(con.isOpen()) {
                    con.close();
                }
                phaser.arriveAndDeregister();
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setApproaching(boolean approaching) {
        this.approaching = approaching;
        if(approaching) {
            try {
                System.err.println("Approaching runway...");
                if(chan.isOpen()) {
                    chan.close();
                }
                if(con.isOpen()) {
                    con.close();
                }
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void setEnd(boolean end) {
        this.end = end;
        if(end) {
            try {
                off();
                Thread.sleep(3000);
                System.err.println("Plane landed successfully");
                if(chan.isOpen()) {
                    chan.close();
                }
                if(con.isOpen()) {
                    con.close();
                }
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void handleOxygenMasks() {
        String instruction = "";
        if (pressure < 8 && !oxygenMasks.isActive()) {
            instruction = "drop";
            System.err.println("[CONTROLLER] Dropping oxygen masks");
            transmit(instruction, Key.OXYGEN_MASKS.name);
        }
    }

    public void handlePressurizer() {
        String instruction = "";

        if (pressure > 12) instruction = "release";
        else if (pressure > 10) return;
        else if (pressure > 8) instruction = "suck";
        else if (pressure > 1) {
            System.err.println("[CONTROLLER] ALERT! PRESSURE LOW");
            instruction = "suck";
        }
        System.out.println("[CONTROLLER] Telling pressurizer to " + instruction + " air");
        transmit(instruction, Key.PRESSURIZER.name);
    }

    public void handleLandingGear() {
        String instruction = "";
        if(!landingGearDown) {
            if (altitude < 2000 && speed < 400 && !landingGear.isActive()) {
                instruction = "lower";
                System.out.println("[CONTROLLER] Instructing landing gear to be lowered");
                transmit(instruction, Key.LANDING_GEAR.name);
                landingGearDown = true;
                phaser.arriveAndDeregister();
            }
        }
    }

    void receive() {
        try {
            chan.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "topic");
            String qName = chan.queueDeclare().getQueue();
//            chan.basicQos(1);
            chan.queueBind(qName, Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "#");
            chan.basicConsume(qName, (x, msg) -> {
                String sender = msg.getEnvelope().getRoutingKey();
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                handleMessage(body, sender);
                chan.basicAck(msg.getEnvelope().getDeliveryTag(), false);
            }, x -> {

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void handleMessage(String message, String sender) {
        switch (sender) {
            case "altitude" -> {
                altitude = Integer.parseInt(message);
                CompletableFuture.runAsync(this::handleWingFlaps);
                CompletableFuture.runAsync(this::handleLandingGear);
            }
            case "pressure" -> {
                pressure = Double.parseDouble(message);
                CompletableFuture.runAsync(this::handlePressurizer);
                CompletableFuture.runAsync(this::handleOxygenMasks);
                CompletableFuture.runAsync(this::handleWingFlaps);
            }
            case "speed" -> {
                speed = Integer.parseInt(message);
                System.out.println(speed);
                CompletableFuture.runAsync(this::handleEngine);
            }
            case "weather" -> {

            }
        }
    }

    void transmit(String instruction, String key){
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, key, false, null, instruction.getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void off(){
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, "topic");
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.ENGINE.name + ".off", false, null, "off".getBytes());
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.LANDING_GEAR.name + ".off", false, null, "off".getBytes());
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.OXYGEN_MASKS.name + ".off", false, null, "off".getBytes());
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.PRESSURIZER.name + ".off", false, null, "off".getBytes());
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.TAIL_FLAPS.name + ".off", false, null, "off".getBytes());
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, Key.WING_FLAPS.name + ".off", false, null, "off".getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
