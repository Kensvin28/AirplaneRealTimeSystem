package Controller;

public enum Exchange {
    SENSOR_CONTROLLER_EXCHANGE("sensorControllerExchange"),
    CONTROLLER_ACTUATOR_EXCHANGE("controllerActuatorExchange"),
    ACTUATOR_SENSOR_EXCHANGE("actuatorSensorExchange");

    public final String name;

    private Exchange(String name) {
        this.name = name;
    }
}
