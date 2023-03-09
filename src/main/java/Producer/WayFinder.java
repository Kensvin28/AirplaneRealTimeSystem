package Producer;

import java.util.Random;

public class WayFinder {
    Random rand = new Random();
    // feet above sea level
    int direction;

    public void setDirection(int directionChange){
        // manage direction to be within 0-360
        if(direction + directionChange > 360){
            direction = (direction + directionChange) % 360;
        } else if(direction + directionChange < 0){
            direction = 360 + (direction + directionChange);
        } else {
            direction += directionChange;
        }
    }

    public int getDirection() {
        return direction;
    }

    public WayFinder() {
        direction = 30 * rand.nextInt(0, 12);
    }
}
