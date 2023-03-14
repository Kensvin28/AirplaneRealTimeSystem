package Controller;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;

public class Approach extends ControllerLogic implements Runnable {
    Phaser phaser;

    public Approach(Phaser phaser, int target) {
        super(phaser, target);
        landingGearDown = true;
        this.phaser = phaser;
        this.target = target;
        phaser.register();
    }

    // end the simulation
    public void setEnd() {
        try {
            off();
            if (chan.isOpen()) {
                chan.close();
            }
            if (chan2.isOpen()) {
                chan2.close();
            }
            if (con.isOpen()) {
                con.close();
            }
            ex.shutdown();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            System.err.println("Plane landed successfully");
        }
    }

    @Override
    public void run() {
        receive();
    }

    // speed to 250
    synchronized public void handleEngine() {
        int instruction = 0;
        if (speed > 400) instruction = 10;
        else if (speed > 300) instruction = 25;
        else if (speed > 250) instruction = 50;
        else if (speed > 200) instruction = 75;
        else if (speed >= 100) instruction = 100;
        else if (speed == 0) instruction = 0;

        // touch down braking
        if (altitude == 0) instruction = -100;
        // stop plane
        if (speed == 0) instruction = 0;
        // end last phase
        if (speed == 0 && throttle == 0) phaser.arriveAndDeregister();

        if(instruction != throttle) {
            System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
        }
        transmit(String.valueOf(instruction), Key.ENGINE.name);
    }

    // wing flaps to -30
    synchronized public void handleWingFlaps() {
        int instruction = -30;
        String display = "[CONTROLLER] Telling wing flaps to change its angle to " + instruction + "°";

        // brake when touch down
        if (altitude == 0) {
            instruction = -90;
            display = "[CONTROLLER] Telling wing flaps to change its angle to brake";
        }

        if (instruction != wingFlapsAngle) {
            System.out.println(display);
        }
        transmit(String.valueOf(instruction), Key.WING_FLAPS.name);
    }

}

