package Controller;

import java.io.IOException;
import java.util.concurrent.*;

public class Cruising extends ControllerLogic implements Runnable {
    Phaser phaser;

    public Cruising(ScheduledExecutorService timer, Phaser phaser) {
        super(phaser);
        System.out.println("[CONTROLLER] Target direction: " + target);
        landingGearDown = false;
        this.phaser = phaser;
        phaser.register();

        // refresh target waypoint every 10 seconds
        timer.scheduleAtFixedRate(() -> {
            refreshTarget();
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void run() {
        receive();
    }

    // set new target
    public void refreshTarget() {
        target = 30 * random.nextInt(0, 12);
        System.out.println("[CONTROLLER] New target waypoint: " + target);
    }

    public void setLanding() {
        try {
            System.err.println("Plane is going to land");
            if (chan.isOpen()) {
                chan.close();
            }
            if (chan2.isOpen()) {
                chan2.close();
            }
            if (con.isOpen()) {
                con.close();
            }
            phaser.arriveAndDeregister();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
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
        int instruction = 0;
        if (altitude > 49_000 || pressure < 8) instruction = -60;
        else if (altitude > 45_000 || weather.equals(Weather.STORMY)) instruction = -30;
        else if (altitude > 40_000) return;
        else if (altitude > 35_000) instruction = 30;
        else if (altitude >= 30_000) instruction = 60;

        if (instruction != wingFlapsAngle) {
            System.out.println("[CONTROLLER] Telling wing flaps to change its angle to " + instruction + "°");
        }
        transmit(String.valueOf(instruction), Key.WING_FLAPS.name);
    }
}