package Simulation;

import Consumer.*;
import Controller.Approach;
import Controller.Cruising;
import Controller.Descent;
import Producer.*;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Simulation1 {
    public static void main(String[] args) {
        simulate();
    }

    public static void simulate() {
        System.out.println("----Simulation started----");
        long startTime = System.nanoTime();
        // fixed rate for executing logics (milliseconds)
        final int PERIOD = 500;
        // delay before descending (seconds)
        final int DELAY = 10;
        Random random = new Random();
        AtomicInteger target = new AtomicInteger(30 * random.nextInt(0, 12));

        // initialise phaser for cruising, descending, and approach phases
        Phaser phaser = new Phaser();
        phaser.register();

        // initialise sensors
        Altimeter altimeter = new Altimeter();
        Barometer barometer = new Barometer(altimeter);
        Speedometer speedometer = new Speedometer();
        WeatherSystem weatherSystem = new WeatherSystem();
        WayFinder wayFinder = new WayFinder();

        // initialise actuators
        Engine engine = new Engine(speedometer, altimeter);
        LandingGear landingGear = new LandingGear();
        OxygenMasks oxygenMasks = new OxygenMasks();
        Pressurizer pressurizer = new Pressurizer();
        TailFlaps tailFlaps = new TailFlaps();
        WingFlaps wingFlaps = new WingFlaps();

        // initialise logics
        AltimeterLogic altimeterLogic = new AltimeterLogic(altimeter);
        SpeedometerLogic speedometerLogic = new SpeedometerLogic(speedometer);
        BarometerLogic barometerLogic = new BarometerLogic(barometer, altimeter);
        WeatherSystemLogic weatherSystemLogic = new WeatherSystemLogic(weatherSystem);
        WayFinderLogic wayFinderLogic = new WayFinderLogic(wayFinder);

        EngineLogic engineLogic = new EngineLogic(engine, speedometer);
        LandingGearLogic landingGearLogic = new LandingGearLogic(landingGear);
        OxygenMasksLogic oxygenMasksLogic = new OxygenMasksLogic(oxygenMasks);
        PressurizerLogic pressurizerLogic = new PressurizerLogic(pressurizer, barometer);
        TailFlapsLogic tailFlapsLogic = new TailFlapsLogic(tailFlaps, wayFinder);
        WingFlapsLogic wingFlapsLogic = new WingFlapsLogic(wingFlaps, altimeter);

        // initialise executor
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(20);

        // start actuators
        timer.scheduleAtFixedRate(engineLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(landingGearLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(oxygenMasksLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(pressurizerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(tailFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wingFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

        // start controller
        ExecutorService ex = Executors.newFixedThreadPool(1);
        Cruising cruising = new Cruising(timer, phaser, target.get());
        ex.submit(cruising);

        // start sensors
        timer.scheduleAtFixedRate(altimeterLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(speedometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(barometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(weatherSystemLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wayFinderLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

        // sudden loss of pressure simulation
        Runnable pressureLoss = () -> {
            barometer.setPressure(-5);
        };
        timer.schedule(pressureLoss, random.nextInt(5, DELAY), TimeUnit.SECONDS);

        // change to descending mode
        Runnable changeMode = () -> {
            while (true) {
                // proceed descent when weather is sunny
                if (weatherSystem.getWeather().equals("SUNNY")) {
                    weatherSystemLogic.stopWeatherChange();
                    cruising.setLanding();
                    target.set(cruising.getTarget());
                    ex.shutdownNow();
                    phaser.arriveAndAwaitAdvance();
                    break;
                } else {
                    weatherSystem.setWeather();
                }
            }

            // change to descending mode
            ExecutorService ex2 = Executors.newFixedThreadPool(1);
            Descent descent = new Descent(phaser, target.get());
            ex2.submit(descent);
            phaser.arriveAndAwaitAdvance();

            // change to approaching mode
            descent.setApproaching();
            ex2.shutdownNow();
            ExecutorService ex3 = Executors.newFixedThreadPool(1);
            Approach approach = new Approach(phaser, target.get());
            ex3.submit(approach);
            phaser.arriveAndAwaitAdvance();

            // end simulation
            approach.setEnd();
            ex3.shutdown();
            timer.shutdownNow();

            phaser.arriveAndDeregister();

            long endTime = System.nanoTime();
            System.out.println("----Simulation finished----");
            System.out.printf("Simulation duration: %f s",
                    (float) (endTime - startTime)/1_000_000_000);
        };
        timer.schedule(changeMode, DELAY, TimeUnit.SECONDS);
    }
}