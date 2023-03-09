package Simulation;

import Consumer.*;
import Producer.*;
import Controller.*;

import java.util.Random;
import java.util.concurrent.*;

public class Simulation {
    public static void main(String[] args) {
        final int PERIOD = 100;
        Random random = new Random();

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

        // initialise executor
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(40);

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

        System.out.println("----Simulation started----");
        // start sensors
        timer.scheduleAtFixedRate(altimeterLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(speedometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(barometerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(weatherSystemLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wayFinderLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

        // start actuators
        timer.scheduleAtFixedRate(engineLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(landingGearLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(oxygenMasksLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(pressurizerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(tailFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wingFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

        // start controller
        ExecutorService ex = Executors.newFixedThreadPool(8);
        Cruising cruising = new Cruising(timer, phaser);
        ex.submit(cruising);

        Runnable changeMode = () -> {
            // change to descending mode
            while(true) {
                if (weatherSystem.getWeather().equals("SUNNY")) {
                    weatherSystemLogic.stopWeatherChange();
                    cruising.setLanding();
                    ex.shutdownNow();
                    phaser.arriveAndAwaitAdvance();
                    break;
                }
            }

            // change to descending mode
            ExecutorService ex2 = Executors.newCachedThreadPool();
            Descent descent = new Descent(phaser);
            ex2.submit(descent);
            phaser.arriveAndAwaitAdvance();

            // change to approaching mode
            descent.setApproaching();
            ex2.shutdownNow();
            ExecutorService ex3 = Executors.newCachedThreadPool();
            Approach approach = new Approach(phaser);
            ex3.submit(approach);
            phaser.arriveAndAwaitAdvance();

            // end simulation
            approach.setEnd();
            ex3.shutdown();
            timer.shutdownNow();

            phaser.arriveAndDeregister();

            System.out.println("----Simulation finished----");
        };
        timer.schedule(changeMode, 2, TimeUnit.SECONDS);

        // sudden loss of pressure simulation
        Runnable pressureLoss = () -> barometer.setPressure(-5);
        timer.schedule(pressureLoss, random.nextInt(10), TimeUnit.MILLISECONDS);
    }
}