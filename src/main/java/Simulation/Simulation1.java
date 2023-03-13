package Simulation;

import Consumer.*;
import Controller.Approach;
import Controller.Cruising;
import Controller.Descent;
import Producer.*;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.*;

public class Simulation1 {
    public static void main(String[] args) {
        simulate();
    }

        @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    //Throughput: (default mode) To measure the throughput of a piece of code. This is used to measure the number of times a method is executed in a certain time. Use this when the method takes only a few milliseconds.
    //AverageTime: This is to get the average time the method takes to execute.
    //SampleTime: Sampled time for each operation. Shows p50, p90, p99, min and max times.
    //SingleShotTime:  This measures the time for a single operation. Use this when you want to account for the cold start time also.
    //All: Measures all of the above.
    @Measurement(iterations = 3)
    //It is used to set the default measurement parameters for the benchmark. It allows to specify the number of iterations and the time for which each is to be executed.
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(iterations = 1)
    @Fork(1) //run each iteration value times
    public static void simulate() {
        System.out.println("----Simulation started----");
        long startTime = System.nanoTime();
        final int PERIOD = 500;
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

        // start actuators
        timer.scheduleAtFixedRate(engineLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(landingGearLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(oxygenMasksLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(pressurizerLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(tailFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(wingFlapsLogic, 0, PERIOD, TimeUnit.MILLISECONDS);

        // start controller
//        ExecutorService ex = Executors.newFixedThreadPool(1);
        Cruising cruising = new Cruising(timer, phaser);
        CompletableFuture.runAsync(cruising);
//        ex.submit(cruising);

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
        timer.schedule(pressureLoss, random.nextInt(10, 20), TimeUnit.SECONDS);

        Runnable changeMode = () -> {
            // change to descending mode
            while (true) {
                if (weatherSystem.getWeather().equals("SUNNY")) {
                    weatherSystemLogic.stopWeatherChange();
                    cruising.setLanding();
//                    ex.shutdownNow();
                    phaser.arriveAndAwaitAdvance();
                    break;
                }
            }

            // change to descending mode
//            ExecutorService ex2 = Executors.newFixedThreadPool(1);
            Descent descent = new Descent(phaser);
            CompletableFuture.runAsync(descent);
//            ex2.submit(descent);
            phaser.arriveAndAwaitAdvance();

            // change to approaching mode
            descent.setApproaching();
//            ex2.shutdownNow();
//            ExecutorService ex3 = Executors.newFixedThreadPool(1);
            Approach approach = new Approach(phaser);
            CompletableFuture.runAsync(approach);
//            ex3.submit(approach);
            phaser.arriveAndAwaitAdvance();

            // end simulation
            approach.setEnd();
//            ex3.shutdown();
            timer.shutdownNow();

            phaser.arriveAndDeregister();

            long endTime = System.nanoTime();
            System.out.println("----Simulation finished----");
            System.out.printf("Simulation duration: %f s", (float) (endTime - startTime)/1_000_000_000);
        };
        timer.schedule(changeMode, 20, TimeUnit.SECONDS);
    }
}