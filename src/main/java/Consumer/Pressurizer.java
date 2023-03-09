package Consumer;

import Producer.Barometer;

public class Pressurizer {
    boolean active = false;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean state) {
        active = state;
    }

    public Pressurizer() {
    }
}