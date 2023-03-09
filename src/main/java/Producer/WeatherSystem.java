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
        if(probability <= 0.6)
            weather = String.valueOf(Weather.values()[0]);
        else
            weather = String.valueOf(Weather.values()[1]);
    }

    public WeatherSystem() {
    }
}