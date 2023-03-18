package Producer;

import Controller.Weather;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class WeatherSystem {
    String weather;
    Random random = new Random();

    public String getWeather() {
        return weather;
    }

    public void setWeather() {
        double probability = random.nextDouble();
        // set weather to sunny 90% of the time and stormy 10% of the time
        if(probability <= 0.9)
            weather = String.valueOf(Weather.values()[0]);
        else
            weather = String.valueOf(Weather.values()[1]);
    }

    public WeatherSystem() {
    }
}