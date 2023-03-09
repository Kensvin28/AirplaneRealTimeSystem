package Controller;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.*;

public abstract class ControllerLogic implements FlightMode {
    ConnectionFactory cf = new ConnectionFactory();
    Random random = new Random();

    int altitude;
    double pressure;
    int speed;
    Weather weather;
    int direction;
    int target = 30 * random.nextInt(0, 12);
    int throttle;

    boolean landingGearDown;
    boolean oxygenMasksDown = false;
    String pressurizerState;
    int tailFlapsAngle;
    int wingFlapsAngle;

    Connection con;
    Channel chan;
    Channel chan2;
    Phaser phaser;

    public ControllerLogic(Phaser phaser) {
        this.phaser = phaser;
        try {
            con = cf.newConnection();
            chan = con.createChannel();
            chan2 = con.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public void handleOxygenMasks() {
        String instruction;
        if (pressure < 8 && !isOxygenMasksDown()) {
            instruction = "drop";
            System.err.println("[CONTROLLER] Dropping oxygen masks");
            transmit(instruction, Key.OXYGEN_MASKS.name);
            setOxygenMasksDown(true);
        }
    }

    synchronized public void handleLandingGear() {
        String instruction;
        if (!isLandingGearDown()) {
            if (altitude < 2000 && speed < 400) {
                instruction = "lower";
                System.out.println("[CONTROLLER] Instructing landing gear to be lowered");
                transmit(instruction, Key.LANDING_GEAR.name);
            }
        }
    }

    synchronized public void handlePressurizer() {
        String instruction = "";

        // release air
        if (pressure > 12) instruction = "release";
        else if (pressure > 10) return;
        // suck air
        else if (pressure > 8) instruction = "suck";
        else if (pressure > 1) {
            System.err.println("[CONTROLLER] ALERT! PRESSURE LOW");
            instruction = "suck maximum";
            CompletableFuture.runAsync(this::handleOxygenMasks);
        }

        if (!instruction.equals(pressurizerState)) {
                System.out.println("[CONTROLLER] Telling pressurizer to " + instruction + " air");
        }
        transmit(instruction, Key.PRESSURIZER.name);
    }

    synchronized private void handleDirection() {
        int instruction;
        int difference = target - direction;
        if (difference < 0)
            difference += 360;

        if (difference > 270)
            instruction = -30;
        else if (difference > 180)
            instruction = -60;
        else if (difference > 90)
            instruction = 60;
        else if (difference > 0)
            instruction = 30;
        else instruction = 0;

        if (instruction != tailFlapsAngle) {
            System.out.println("[CONTROLLER] Instructing tail flaps to tilt " + instruction + "°");
            transmit(String.valueOf(instruction), Key.TAIL_FLAPS.name);
        }
    }

    synchronized private void handleWeather() {
        // evade storm
        if (weather.equals(Weather.STORMY)) {
            target = 30 * random.nextInt(0, 12);
            System.err.println("[CONTROLLER] Bad weather ahead, diverting to " + target + "°");
        }
    }

    public void setOxygenMasksDown(boolean down) {
        oxygenMasksDown = down;
    }

    public boolean isOxygenMasksDown() {
        return oxygenMasksDown;
    }

    public void setLandingGearDown(String message) {
        landingGearDown = Boolean.parseBoolean(message);
    }

    public boolean isLandingGearDown() {
        return landingGearDown;
    }

    void receive() {
        try {
            // receive messages from sensors
            chan.exchangeDeclare(Exchange.SENSOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            String qName = chan.queueDeclare().getQueue();
            chan.queueBind(qName, Exchange.SENSOR_CONTROLLER_EXCHANGE.name, "#");
            chan.basicConsume(qName, (x, msg) -> {
                String sender = msg.getEnvelope().getRoutingKey();
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                handleMessage(body, sender);
            }, x -> {

            });

            // receive messages from actuators
            chan2.exchangeDeclare(Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            qName = chan2.queueDeclare().getQueue();
            chan2.queueBind(qName, Exchange.ACTUATOR_CONTROLLER_EXCHANGE.name, "#");
            chan2.basicConsume(qName, (x, msg) -> {
                String sender = msg.getEnvelope().getRoutingKey();
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                handleMessage(body, sender);
            }, x -> {

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(String message, String sender) {
        if (sender.contains("pressure")) {
            pressure = Double.parseDouble(message);
            CompletableFuture.runAsync(this::handlePressurizer);
        }
        else if (sender.contains("altitude")) {
            altitude = Integer.parseInt(message);
            CompletableFuture.runAsync(this::handleWingFlaps);
            CompletableFuture.runAsync(this::handleLandingGear);
        } else if (sender.contains("speed")) {
            speed = Integer.parseInt(message);
            CompletableFuture.runAsync(this::handleEngine);
        } else if (sender.contains("weather")) {
            if (weather == null || !weather.equals(Weather.valueOf(message))) {
                weather = Weather.valueOf(message);
                CompletableFuture.runAsync(this::handleWeather);
            }
        } else if (sender.contains("direction")) {
            direction = Integer.parseInt(message);
            CompletableFuture.runAsync(this::handleDirection);
        } else if (sender.contains("landingGear")) {
            setLandingGearDown(message);
            if (isLandingGearDown()) {
                phaser.arriveAndDeregister();
            }
        } else if (sender.contains("engine")) {
            throttle = Integer.parseInt(message);
            System.out.println("[CONTROLLER] Engine throttle at " + throttle + "%");
        } else if (sender.contains("pressurizer")) {
            pressurizerState = message;
            System.out.println("[CONTROLLER] Pressurizing: " + pressurizerState);
        } else if (sender.contains("tailFlaps")) {
            tailFlapsAngle = Integer.parseInt(message);
            System.out.println("[CONTROLLER] Tail flaps angle at " + tailFlapsAngle + "°");
        } else if (sender.contains("wingFlaps")) {
            wingFlapsAngle = Integer.parseInt(message);
            System.out.println("[CONTROLLER] Wing flaps angle at " + wingFlapsAngle + "°");
        }
    }

    void transmit(String instruction, String key) {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, key, false, null, instruction.getBytes());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    // Turn off the actuator consumers
    void off() {
        try (Connection connection = cf.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, BuiltinExchangeType.TOPIC);
            LinkedList<String> linkedList = new LinkedList<>();
            linkedList.add(Key.ENGINE.name + ".off");
            linkedList.add(Key.LANDING_GEAR.name + ".off");
            linkedList.add(Key.OXYGEN_MASKS.name + ".off");
            linkedList.add(Key.PRESSURIZER.name + ".off");
            linkedList.add(Key.TAIL_FLAPS.name + ".off");
            linkedList.add(Key.WING_FLAPS.name + ".off");
            linkedList.parallelStream().forEach(key -> {
                try {
                    channel.basicPublish(Exchange.CONTROLLER_ACTUATOR_EXCHANGE.name, key, false, null, "off".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
