package Controller;

public enum Key {
    ALTITUDE("altitude"),
    WEATHER("weather"),
    SPEED("speed"),
    PRESSURE("pressure"),
    WING_FLAPS("wingFlaps"),
    TAIL_FLAPS("tailFlaps"),
    PRESSURIZER("pressurizer"),
    OXYGEN_MASKS("oxygenMasks"),
    LANDING_GEAR("landingGear"),
    ENGINE("engine")
    ;

    public final String name;

    Key(String name) {
        this.name = name;
    }
}
