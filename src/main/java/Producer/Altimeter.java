package Producer;

import java.util.Random;

public class Altimeter {
    Random rand = new Random();
    // feet above sea level
    volatile int altitude;

    public int getAltitude() {
        return altitude;
    }

    synchronized public void setAltitude(int altitudeChange) {
        if (altitudeChange != 0) {
            if (altitude + altitudeChange < 0) {
                altitude = 0;
            } else {
                altitude += altitudeChange;
            }
        }
    }

    public Altimeter() {
        altitude = rand.nextInt(30_000, 50_000);
    }
}

