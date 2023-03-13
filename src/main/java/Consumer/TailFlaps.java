package Consumer;

import Producer.Altimeter;

public class TailFlaps {
    volatile int angle = 0;

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angleChange) {
        angle = angleChange;
    }

    public TailFlaps() {
    }
}
