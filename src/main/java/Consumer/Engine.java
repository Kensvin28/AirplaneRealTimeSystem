package Consumer;

import Producer.Altimeter;
import Producer.Speedometer;

public class Engine {
    Speedometer speedometer;
    Altimeter altimeter;
    int throttle = 75;

    public Engine(Speedometer speedometer, Altimeter altimeter) {
        this.speedometer = speedometer;
        this.altimeter = altimeter;
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int newThrottle) {
        if(throttle+newThrottle<-100){
            throttle = -100;
        } else {
            throttle += newThrottle;
        }
    }

}
