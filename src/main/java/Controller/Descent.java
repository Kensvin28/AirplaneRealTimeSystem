package Controller;

import Consumer.*;

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
            if(chan.isOpen()) {
                chan.close();
            }
            if(chan2.isOpen()){
                chan2.close();
            }
            if(con.isOpen()) {
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

