package Controller;

import Consumer.*;

import java.util.concurrent.Phaser;

public class Descent extends ControllerLogic implements Runnable {

    public Descent(Engine engine, LandingGear landingGear, OxygenMasks oxygenMasks, Pressurizer pressurizer, WingFlaps wingFlaps, Phaser phaser) {
        super(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
        phaser.register();
    }

    @Override
    public void run() {
        receive();
    }

    public void handleTailFlaps() {
    }

    // Speed to 300
    public void handleEngine() {
        String instruction = "";
        if (speed > 500) instruction = "10";
        else if (speed > 400) instruction = "25";
        else if (speed > 300) instruction = "50";
        else if (speed > 200) instruction = "75";
        else if (speed >= 100) instruction = "100";

        System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
        transmit(instruction, Key.ENGINE.name);
    }

    public void handleWingFlaps() {
        String instruction = "-60";
        System.out.println("[CONTROLLER] Telling flap to change its angle to " + instruction + "°");
        transmit(instruction, Key.WING_FLAPS.name);
    }
}

