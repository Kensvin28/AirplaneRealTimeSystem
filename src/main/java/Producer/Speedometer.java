package Producer;

import java.util.Random;

public class Speedometer {
    Random rand = new Random();
    int speed;
    final int MAX_SPEED = 600;

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speedChange) {
        if(speedChange != 0) {
            // speed limit
            if (speed + speedChange < 0) {
                speed = 0;
            } else if (speed + speedChange > MAX_SPEED) {
                speed = MAX_SPEED;
            } else {
                speed += speedChange;
                System.out.println("[SPEEDOMETER] New Speed: " + speed);
            }
        }
    }

    public Speedometer() {
        speed = rand.nextInt(100, 600);
//        ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
//        timer.scheduleAtFixedRate(new SpeedometerLogic(), 0, 1, TimeUnit.SECONDS);
    }
}
