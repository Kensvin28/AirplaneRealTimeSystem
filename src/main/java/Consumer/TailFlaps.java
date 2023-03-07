package Consumer;

import Producer.Altimeter;

public class TailFlaps {
    int angle = 0;

    public int getAngle() { return angle; }
    public void setAngle(int angleChange){
        angle += angleChange;
    }

    public TailFlaps(Altimeter altimeter) {

    }
}
