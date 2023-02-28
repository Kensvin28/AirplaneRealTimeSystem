import Consumer.Engine;
import Consumer.LandingGear;
import Consumer.OxygenMasks;
import Consumer.WingFlaps;
import Producer.Altimeter;
import Controller.Controller;
import Producer.Speedometer;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Simulation {
    public static void main(String[] args) {
        Altimeter altimeter = new Altimeter();
        Speedometer speedometer = new Speedometer();
        new Controller();
        new WingFlaps(altimeter);
        new Engine(speedometer, altimeter);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(20);
//        timer.scheduleAtFixedRate(new Altimeter(), 0, 3, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Speedometer(), 0, 3, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Barometer(), 0, 1, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Thermometer(), 0, 1, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new WeatherSystem(), 0, 10, TimeUnit.SECONDS);

//        timer.scheduleAtFixedRate(new Controller(), 0, 3, TimeUnit.SECONDS);
//
//        timer.scheduleAtFixedRate(new WingFlaps(), 0, 3, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Engine(), 0, 3, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new LandingGear(), 0, 1, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new OxygenMasks(), 0, 1, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Pressurizer(), 0, 1, TimeUnit.SECONDS);
//        timer.scheduleAtFixedRate(new Rudder(), 0, 1, TimeUnit.SECONDS);
    }
}
