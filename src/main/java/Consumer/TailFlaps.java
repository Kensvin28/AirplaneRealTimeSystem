package Consumer;

import Producer.Altimeter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TailFlaps {
    int angle = 0;

    public int getAngle() { return angle; }
    public void setAngle(int angleChange){
        angle += angleChange;
    }

    public TailFlaps(Altimeter altimeter) {

    }
}
