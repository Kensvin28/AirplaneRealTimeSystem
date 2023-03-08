package Simulation;

import Consumer.*;
import Producer.*;
import Controller.*;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

public class Simulation {
    public static void main(String[] args) {
        final int PERIOD = 100;
        Random random = new Random();

        // initialise phaser for cruising, descending, and approach phases
        Phaser phaser = new Phaser();
        Phaser connection = new Phaser();
        phaser.register();
        connection.register();

        // initialise sensors
        Altimeter altimeter = new Altimeter();
        Barometer barometer = new Barometer(altimeter);
        Speedometer speedometer = new Speedometer();
        WeatherSystem weatherSystem = new WeatherSystem();

        // initialise actuators
        Engine engine = new Engine(speedometer, altimeter);
        LandingGear landingGear = new LandingGear();
        OxygenMasks oxygenMasks = new OxygenMasks();
        Pressurizer pressurizer = new Pressurizer(barometer);
        TailFlaps tailFlaps = new TailFlaps(altimeter);
        WingFlaps wingFlaps = new WingFlaps(altimeter);

        ScheduledExecutorService timer = Executors.newScheduledThreadPool(40);

        AltimeterLogic altimeterLogic = new AltimeterLogic(altimeter);
        SpeedometerLogic speedometerLogic = new SpeedometerLogic(speedometer);
        BarometerLogic barometerLogic = new BarometerLogic(barometer, altimeter);
        WeatherSystemLogic weatherSystemLogic = new WeatherSystemLogic(weatherSystem);

        EngineLogic engineLogic = new EngineLogic(engine, speedometer, altimeter, connection);
        LandingGearLogic landingGearLogic = new LandingGearLogic(landingGear, connection);
        OxygenMasksLogic oxygenMasksLogic = new OxygenMasksLogic(oxygenMasks, connection);
        PressurizerLogic pressurizerLogic = new PressurizerLogic(pressurizer, barometer, connection);
        TailFlapsLogic tailFlapsLogic = new TailFlapsLogic(tailFlaps, altimeter, connection);
        WingFlapsLogic wingFlapsLogic = new WingFlapsLogic(wingFlaps, altimeter, connection);

        timer.scheduleAtFixedRate(altimeterLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(speedometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(barometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(weatherSystemLogic, 0, 10, TimeUnit.SECONDS);

        timer.scheduleAtFixedRate(engineLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(landingGearLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(oxygenMasksLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(pressurizerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(tailFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wingFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

//        timer.scheduleAtFixedRate(altimeterLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(speedometerLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(barometerLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(weatherSystemLogic, 0, 10, TimeUnit.SECONDS);
//
//        timer.scheduleAtFixedRate(engineLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(landingGearLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(oxygenMasksLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(pressurizerLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(tailFlapsLogic, 0, 500, TimeUnit.MILLISECONDS);
//        timer.scheduleAtFixedRate(wingFlapsLogic, 0, 500, TimeUnit.MILLISECONDS);

        ExecutorService ex = Executors.newFixedThreadPool(8);
        Cruising cruising = new Cruising(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
        ex.submit(cruising);

        Runnable changeMode = () -> {
            // change to descending mode
            cruising.setLanding(true);
            ex.shutdownNow();
            phaser.arriveAndAwaitAdvance();

            // change to descending mode
            ExecutorService ex2 = Executors.newCachedThreadPool();
            Descent descent = new Descent(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
            ex2.submit(descent);
            phaser.arriveAndAwaitAdvance();

            // change to approaching mode
            descent.setApproaching(true);
            ex2.shutdownNow();
            ExecutorService ex3 = Executors.newCachedThreadPool();
            Approach approach = new Approach(engine, landingGear, oxygenMasks, pressurizer, wingFlaps, phaser);
            ex3.submit(approach);
            phaser.arriveAndAwaitAdvance();

            approach.setEnd(true);
            ex3.shutdownNow();
            timer.shutdownNow();
//            int i = 1;
//            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
//            for (Thread t: threadSet
//            ) {
//                System.out.println(i + ". " + t + " " + t.getState());
//                i++;
//            }
            phaser.arriveAndDeregister();
            connection.arriveAndDeregister();
            phaser.forceTermination();
        };

        timer.schedule(changeMode, 2, TimeUnit.SECONDS);

        // Sudden loss of pressure simulation
        Runnable pressureLoss = () -> {
            barometer.setPressure(-5);
        };
        timer.schedule(pressureLoss, random.nextInt(10), TimeUnit.MILLISECONDS);
    }
}