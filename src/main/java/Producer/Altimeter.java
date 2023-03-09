package Producer;

import java.util.Random;

public class Altimeter {
    Random rand = new Random();
    // feet above sea level
    int altitude;

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitudeChange) {
        if (altitudeChange != 0) {
            if (altitude + altitudeChange < 0) {
                altitude = 0;
            } else {
                altitude += altitudeChange;
                System.out.println("[ALTIMETER] New Altitude: " + altitude);
            }
        }
    }

    public Altimeter() {
        altitude = rand.nextInt(30_000, 50_000);
    }
}

