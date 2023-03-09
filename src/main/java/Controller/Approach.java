package Controller;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeoutException;

public class Approach extends ControllerLogic implements Runnable {
    Phaser phaser;

    public Approach(Phaser phaser) {
        super(phaser);
        landingGearDown = true;
        this.phaser = phaser;
        phaser.register();
    }

    public void setEnd() {
        try {
            off();
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
        } finally {
            System.err.println("Plane landed successfully");
        }
    }

    @Override
    public void run() {
        receive();
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

        // Touch down braking
        if (altitude == 0) instruction = "-100";
        // Stop plane
        if (speed == 0) instruction = "0";
        // End program
        if (speed == 0 && throttle == 0) phaser.arriveAndDeregister();

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

