package Producer;

import java.util.Random;

public class Barometer {
    Altimeter altimeter;
    Random rand = new Random();
    // psi
    double pressure;

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressureChange) {
        // set the lowest pressure limit to 3
        if (pressure < 3) {
            pressure = 3;
        } else {
            pressure += pressureChange;
            System.out.println("[BAROMETER] New Pressure: " + pressure);
        }
    }

    public Barometer(Altimeter altimeter) {
        this.altimeter = altimeter;
        pressure = rand.nextInt(10, 13);
    }
}
