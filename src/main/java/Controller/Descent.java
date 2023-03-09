package Controller;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;

public class Descent extends ControllerLogic implements Runnable {
    Phaser phaser;

    public Descent(Phaser phaser) {
        super(phaser);
        landingGearDown = false;
        this.phaser = phaser;
        phaser.register();
    }

    public void setApproaching() {
        try {
            System.err.println("Approaching runway...");
            if (chan.isOpen()) {
                chan.close();
            }
            if (chan2.isOpen()) {
                chan2.close();
            }
            if (con.isOpen()) {
                con.close();
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        receive();
    }

    // speed to 300
    synchronized public void handleEngine() {
        int instruction = 0;
        if (speed > 500) instruction = 10;
        else if (speed > 400) instruction = 25;
        else if (speed > 300) instruction = 50;
        else if (speed > 200) instruction = 75;
        else if (speed >= 100) instruction = 100;

        if(instruction != throttle) {
            System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
        }
        transmit(String.valueOf(instruction), Key.ENGINE.name);
    }

    // wing flap to -60
    synchronized public void handleWingFlaps() {
        int instruction = -60;
        if (instruction != wingFlapsAngle) {
            System.out.println("[CONTROLLER] Telling wing flaps to change its angle to " + instruction + "°");
        }
        transmit(String.valueOf(instruction), Key.WING_FLAPS.name);
    }
}

