package Producer;

import Controller.Weather;

public class WeatherSystem {
    String weather;

    public String getWeather() {
        return weather;
    }

    public void setWeather(String newWeather){
        weather = newWeather;
        System.out.println("[WEATHER] New Weather: " + weather);
    }

    public WeatherSystem() {
        weather = String.valueOf(Weather.values()[(int)(Math.random()*(Weather.values().length))]);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new WeatherSystemLogic(), 0, 1, TimeUnit.SECONDS);
    }
}