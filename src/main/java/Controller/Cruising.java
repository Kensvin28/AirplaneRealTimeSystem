package Controller;

import Consumer.*;

import java.util.concurrent.Phaser;

public class Cruising extends ControllerLogic implements Runnable {


    public Cruising(Engine engine, LandingGear landingGear, OxygenMasks oxygenMasks, Pressurizer pressurizer, WingFlaps wingFlaps, Phaser phaser) {
        super(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
        phaser.register();
    }

    public void run() {
        receive();
    }

    public void handleTailFlaps() {
    }

    // Speed to 525
    public void handleEngine() {
        String instruction = "";
        if (speed > 550) instruction = "25";
        else if (speed > 525) instruction = "50";
        else if (speed > 500) instruction = "75";
        else if (speed >= 100) instruction = "100";
        System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
        transmit(instruction, Key.ENGINE.name);
    }

    // Angle to 0
    public void handleWingFlaps() {
        String instruction = "0";
        if (altitude > 49_000 || pressure < 8) instruction = "-60";
        else if (altitude > 45_000) instruction = "-30";
        else if (altitude > 40_000) return;
        else if (altitude > 35_000) instruction = "30";
        else if (altitude >= 30_000) instruction = "60";

        System.out.println("[CONTROLLER] Telling flap to change its angle to " + instruction + "°");
        transmit(instruction, Key.WING_FLAPS.name);
    }
}