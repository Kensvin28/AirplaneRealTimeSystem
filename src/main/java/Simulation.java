import Consumer.*;
import Producer.Altimeter;
import Controller.Controller;
import Producer.Barometer;
import Producer.Speedometer;
import Producer.WeatherSystem;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Simulation {
    public static void main(String[] args) {
        Altimeter altimeter = new Altimeter();
        Barometer barometer = new Barometer();
        Speedometer speedometer = new Speedometer();
        WeatherSystem weatherSystem = new WeatherSystem();
        new Controller();
        new WingFlaps(altimeter);
        new Pressurizer(barometer);
        new OxygenMasks();
        new LandingGear();
        new Engine(speedometer, altimeter);
    }
}
