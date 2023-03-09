package Consumer;

import Producer.Altimeter;

public class WingFlaps {
    int angle = 0;

    public int getAngle() {
        return angle;
    }

    public void setAngle(int newAngle) {
        angle = newAngle;
    }

    public void changeAngle(int angleChange) {
        angle += angleChange;
    }

    public WingFlaps() {

    }
}