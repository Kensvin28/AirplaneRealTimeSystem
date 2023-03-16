package Controller;

public enum Exchange {
    SENSOR_CONTROLLER_EXCHANGE("sensorControllerExchange"),
    CONTROLLER_ACTUATOR_EXCHANGE("controllerActuatorExchange"),
    ACTUATOR_CONTROLLER_EXCHANGE("actuatorControllerExchange");

    public final String name;

    Exchange(String name) {
        this.name = name;
    }
}
