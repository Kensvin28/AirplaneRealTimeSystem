//package Controller;
//
//import Consumer.*;
//
//public class TouchDown extends ControllerLogic implements Runnable {
//
//    public TouchDown(Engine engine, LandingGear landingGear, OxygenMasks oxygenMasks, Pressurizer pressurizer, WingFlaps wingFlaps) {
//        super(engine, landingGear, oxygenMasks, pressurizer, wingFlaps);
//    }
//
//    @Override
//    public void run() {
//        receive();
//    }
//
//    public void handleTailFlaps() {
//    }
//
//    public void handleEngine() {
//        String instruction = "";
//        instruction = "-100";
//        System.out.println("[CONTROLLER] Telling engine to change throttle to " + instruction + "%");
//        transmit(instruction, Key.ENGINE.name);
//    }
//
//    public void handleWingFlaps() {
//        String instruction = "-60";
//        System.out.println("[CONTROLLER] Telling flap to change its angle to " + instruction + "°");
//        transmit(instruction, Key.WING_FLAPS.name);
//    }
//}