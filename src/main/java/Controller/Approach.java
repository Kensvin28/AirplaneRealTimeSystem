package Controller;

import Consumer.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;

public class Approach extends ControllerLogic implements Runnable {

    public Approach(Engine engine, LandingGear landingGear, OxygenMasks oxygenMasks, Pressurizer pressurizer, WingFlaps wingFlaps, Phaser phaser) {
        super(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
        phaser.register();
    }

    @Override
    public void run() {
        receive();
    }

    void handleMessage(String message, String sender) {
        switch (sender) {
            case "altitude" -> {
                altitude = Integer.parseInt(message);
                CompletableFuture.runAsync(this::handleWingFlaps);
                CompletableFuture.runAsync(this::handleLandingGear);
            }
            case "pressure" -> {
                //TODO: Plane down
                pressure = Double.parseDouble(message);
                CompletableFuture.runAsync(this::handlePressurizer);
                CompletableFuture.runAsync(this::handleOxygenMasks);
                CompletableFuture.runAsync(this::handleWingFlaps);
            }
            case "speed" -> {
                speed = Integer.parseInt(message);
                if(speed==0){
                    phaser.arriveAndDeregister();
                }
                CompletableFuture.runAsync(this::handleEngine);
            }
            case "weather" -> {

            }
        }
    }

    public void handleTailFlaps() {
    }

    // Speed to 250
    public void handleEngine() {
        String instruction = "";
        if (speed > 400) instruction = "10";
        else if (speed > 300) instruction = "25";
        else if (speed > 250) instruction = "50";
        else if (speed > 200) instruction = "75";
        else if (speed >= 100) instruction = "100";
        else if (speed == 0) instruction = "0";

        // Touch down
        if (altitude == 0) instruction = "-100";
        System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
        transmit(instruction, Key.ENGINE.name);
    }

    // Wing flaps to -30
    public void handleWingFlaps() {
        String instruction = "-30";

        // Brake when touch down
        if (altitude == 0) instruction = "brake";
        System.out.println("[CONTROLLER] Telling flap to change its angle to " + instruction + "°");
        transmit(instruction, Key.WING_FLAPS.name);
    }

}

