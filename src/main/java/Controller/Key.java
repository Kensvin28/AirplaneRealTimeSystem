package Controller;

public enum Key {
    ALTITUDE("altitude"),
    WEATHER("weather"),
    SPEED("speed"),
    PRESSURE("pressure"),
    WING_FLAPS("wingFlaps"),
    PRESSURIZER("pressurizer"),
    OXYGEN_MASKS("oxygen_masks"),
    LANDING_GEAR("landing_gear"),
    ENGINE("engine")
    ;

    public final String name;

    private Key(String name) {
        this.name = name;
    }
}
