package ru.avem.resonance.communication;

import ru.avem.resonance.Constants;
import ru.avem.resonance.communication.connections.Connection;
import ru.avem.resonance.communication.connections.SerialConnection;
import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.devices.avem_voltmeter.AvemVoltmeterController;
import ru.avem.resonance.communication.devices.deltaC2000.DeltaCP2000Controller;
import ru.avem.resonance.communication.devices.latr.LatrController;
import ru.avem.resonance.communication.devices.pm130.PM130Controller;
import ru.avem.resonance.communication.devices.pr200.OwenPRController;
import ru.avem.resonance.communication.modbus.ModbusController;
import ru.avem.resonance.communication.modbus.RTUController;
import ru.avem.resonance.utils.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static ru.avem.resonance.communication.devices.DeviceController.*;
import static ru.avem.resonance.communication.devices.avem_voltmeter.AvemVoltmeterController.CHANGE_SHOW_VALUE;
import static ru.avem.resonance.communication.devices.deltaC2000.DeltaCP2000Controller.*;
import static ru.avem.resonance.communication.devices.latr.LatrController.*;
import static ru.avem.resonance.communication.devices.pr200.OwenPRController.*;
import static ru.avem.resonance.utils.Utils.sleep;


public class CommunicationModel extends Observable implements Observer {
    public static final Object LOCK = new Object();

    private static CommunicationModel instance = new CommunicationModel();

    private Connection RS485Connection;

    public OwenPRController owenPRController;
    public AvemVoltmeterController avemVoltmeterController;
    public AvemVoltmeterController avemKiloVoltmeterController;
    public PM130Controller pm130Controller;
    public DeltaCP2000Controller deltaCP2000Controller;
    public LatrController latrController;

    private int kms1;
    private int kms2;

    private boolean lastOne;
    private boolean isFinished;

    private volatile boolean isDeviceStateOn;

    public List<DeviceController> devicesControllers = new ArrayList<>();

    private CommunicationModel() {

        connectMainBus();
        ModbusController modbusController = new RTUController(RS485Connection);

        pm130Controller = new PM130Controller(0x29, this, modbusController, PM130_ID);
        devicesControllers.add(pm130Controller);

        avemVoltmeterController = new AvemVoltmeterController(0x0B, this, modbusController, AVEM_ID);
        devicesControllers.add(avemVoltmeterController);

        owenPRController = new OwenPRController(0x04, this, modbusController, PR200_ID);
        devicesControllers.add(owenPRController);

        latrController = new LatrController(0xF0, this, modbusController, LATR_ID);
        devicesControllers.add(latrController);

        deltaCP2000Controller = new DeltaCP2000Controller(0x5B, this, modbusController, DELTACP2000_ID);
        devicesControllers.add(deltaCP2000Controller);

        avemKiloVoltmeterController = new AvemVoltmeterController(0x15, this, modbusController, KILOAVEM_ID);
        devicesControllers.add(avemKiloVoltmeterController);

        new Thread(() -> {
            while (!isFinished) {
                for (DeviceController deviceController : devicesControllers) {
                    if (deviceController.isNeedToRead()) {
                        if (deviceController instanceof LatrController) {
                            for (int i = 0; i <= 2; i++) {
                                deviceController.read(i);
                            }
                        } else if (deviceController instanceof PM130Controller) {
                            for (int i = 1; i <= 3; i++) {
                                deviceController.read(i);
                            }
                        } else if (deviceController instanceof DeltaCP2000Controller) {
                            for (int i = 0; i <= 1; i++) {
                                deviceController.read(i);
                            }
                        } else if (deviceController instanceof AvemVoltmeterController) {
                            for (int i = 0; i <= 1; i++) {
                                deviceController.read(i);
                            }
                        } else if (deviceController instanceof OwenPRController) {
                            for (int i = 0; i <= 1; i++) {
                                deviceController.read(i);
                            }
                        } else {
                            deviceController.read();
                        }
                        if (deviceController instanceof OwenPRController) {
                            resetDog();
                            for (int i = 0; i <= 1; i++) {
                                deviceController.read(i);
                            }
                        }
                    }
                    if (isDeviceStateOn) {
                        deviceController.resetAllDeviceStateOnAttempts();
                    }
                }
                sleep(1);
            }
        }).start();
    }

    public static CommunicationModel getInstance() {
        return instance;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    private void notice(int deviceID, int param, Object value) {
        setChanged();
        notifyObservers(new Object[]{deviceID, param, value});
    }

    @Override
    public void update(Observable o, Object values) {
        int modelId = (int) (((Object[]) values)[0]);
        int param = (int) (((Object[]) values)[1]);
        Object value = (((Object[]) values)[2]);
        notice(modelId, param, value);
    }

    public void setNeedToReadAllDevices(boolean isNeed) {
        owenPRController.setNeedToRead(isNeed);
        avemVoltmeterController.setNeedToRead(isNeed);
        avemKiloVoltmeterController.setNeedToRead(isNeed);
        deltaCP2000Controller.setNeedToRead(isNeed);
        pm130Controller.setNeedToRead(isNeed);
        latrController.setNeedToRead(isNeed);
    }

    public void setNeedToReadForDebug(boolean isNeed) {
        owenPRController.setNeedToRead(isNeed);
        deltaCP2000Controller.setNeedToRead(isNeed);
    }

    public void resetAllDevices() {
        owenPRController.resetAllAttempts();
        avemVoltmeterController.resetAllAttempts();
        avemKiloVoltmeterController.resetAllAttempts();
        deltaCP2000Controller.resetAllAttempts();
        pm130Controller.resetAllAttempts();
        latrController.resetAllAttempts();
    }

    private void connectMainBus() {
        RS485Connection = new SerialConnection(
                Constants.Communication.RS485_DEVICE_NAME,
                Constants.Communication.BAUDRATE,
                Constants.Communication.DATABITS,
                Constants.Communication.STOPBITS,
                Constants.Communication.PARITY,
                Constants.Communication.WRITE_TIMEOUT,
                Constants.Communication.READ_TIMEOUT);
        Logger.withTag("DEBUG_TAG").log("connectMainBus");
        if (!RS485Connection.isInitiatedConnection()) {
            Logger.withTag("DEBUG_TAG").log("!isInitiatedMainBus");
            RS485Connection.closeConnection();
            RS485Connection.initConnection();
        }
    }

    public void finalizeAllDevices() {
        for (DeviceController deviceController : devicesControllers) {
            deviceController.setNeedToRead(false);
        }
    }

    private void resetDog() {
        owenPRController.write(RESET_DOG, 1, 1);
    }

    public void resetResPR200() {
        owenPRController.write(RES, 1, 1);
    }


    public void offAllKms() {
        kms1 = 0;
        writeToKms1Register(kms1);
        kms2 = 0;
        writeToKms2Register(kms2);
    }

    public void onAllKms() {
        kms1 = 1;
        writeToKms1Register(kms1);
        kms2 = 1;
        writeToKms2Register(kms2);
    }

    private void writeToKms1Register(int value) {
        owenPRController.write(KMS1_REGISTER, 1, value);
    }

    private void writeToKms2Register(int value) {
        owenPRController.write(KMS2_REGISTER, 1, value);
    }

    public void onRegisterInTheKms(int numberOfRegister, int kms) {
        int mask = (int) Math.pow(2, --numberOfRegister);
        try {
            int kmsField = CommunicationModel.class.getDeclaredField("kms" + kms).getInt(this);
            kmsField |= mask;
            CommunicationModel.class.getDeclaredMethod(String.format("%s%d%s", "writeToKms", kms, "Register"), int.class).invoke(this, kmsField);
            CommunicationModel.class.getDeclaredField("kms" + kms).set(this, kmsField);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
        }
        Logger.withTag("DEBUG_TAG").log("numberOfRegister=" + numberOfRegister + " kms=" + kms);
        Logger.withTag("DEBUG_TAG").log("1=" + kms1 + " 2=" + kms2);
    }

    public void offRegisterInTheKms(int numberOfRegister, int kms) {
        int mask = ~(int) Math.pow(2, --numberOfRegister);
        try {
            int kmsField = CommunicationModel.class.getDeclaredField("kms" + kms).getInt(this);
            kmsField &= mask;
            CommunicationModel.class.getDeclaredMethod(String.format("%s%d%s", "writeToKms", kms, "Register"), int.class).invoke(this, kmsField);
            CommunicationModel.class.getDeclaredField("kms" + kms).set(this, kmsField);
        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
        }
        Logger.withTag("DEBUG_TAG").log("numberOfRegister=" + numberOfRegister + " kms=" + kms);
        Logger.withTag("DEBUG_TAG").log("1=" + kms1 + " 2=" + kms2);
    }

    public void initOwenPrController() {
        owenPRController.resetAllAttempts();
        resetResPR200();
        owenPRController.resetAllAttempts();
        owenPRController.setNeedToRead(true);
        offAllKms();
        owenPRController.write(MODE_REGISTER, 1, 1);
    }

    public void startObject() {
        deltaCP2000Controller.write(CONTROL_REGISTER, 1, 0b10);
    }

    public void changeRotation() {
        deltaCP2000Controller.write(CONTROL_REGISTER, 1, 0b0011_0000);
    }

    public void stopObject() {
        deltaCP2000Controller.write(CONTROL_REGISTER, 1, 0b1);
    }

    public void setObjectParams(int fOut, int voltageP1, int fP1) {
        deltaCP2000Controller.write(MAX_VOLTAGE_REGISTER, 1, 400 * 10);
        deltaCP2000Controller.write(MAX_FREQUENCY_REGISTER, 1, 51 * 100);
        deltaCP2000Controller.write(NOM_FREQUENCY_REGISTER, 1, 50 * 100);
        deltaCP2000Controller.write(CURRENT_FREQUENCY_OUTPUT_REGISTER, 1, fOut);
        deltaCP2000Controller.write(POINT_1_VOLTAGE_REGISTER, 1, voltageP1);
        deltaCP2000Controller.write(POINT_1_FREQUENCY_REGISTER, 1, fP1);
        deltaCP2000Controller.write(POINT_2_VOLTAGE_REGISTER, 1, 40);
        deltaCP2000Controller.write(POINT_2_FREQUENCY_REGISTER, 1, 50);
    }

    public void setEndsVFDParams(int paramEndUp, int paramEndDown) {
        deltaCP2000Controller.write(END_UP_CONTROL_REGISTER, 1, paramEndUp);
        deltaCP2000Controller.write(END_DOWN_CONTROL_REGISTER, 1, paramEndDown);
    }

    public void setObjectFCur(int fCur) {
        deltaCP2000Controller.write(CURRENT_FREQUENCY_OUTPUT_REGISTER, 1, fCur);
    }

    public void setObjectUMax(int voltageMax) {
        deltaCP2000Controller.write(POINT_1_VOLTAGE_REGISTER, 1, voltageMax);
    }

    public void resetLATR() {
        latrController.write(START_STOP_REGISTER, 0x5A5A5A5A);
    }

    public void startUpLATRUp(float voltage, boolean isNeedReset) {
        Logger.withTag("STARTUP_LATR").log("startUpLATR");
        if (isNeedReset) {
            latrController.write(START_STOP_REGISTER, 0x5A5A5A5A);
        }
        int minDutty = 400;
        int maxDutty = 200;
        float corridor = 0.05f;
        float delta = 0.05f;
        int timeMinPulse = 50;
        int timeMaxPulse = 300;
        float timeMinPulsePercent = 20.0f;
        float timeMaxPulsePercent = 22.0f;
        float minDuttyPercent = 24.0f;
        float maxDuttyPercent = 28.0f;
        float timeMinPeriod = 10.0f;
        float timeMaxPeriod = 100.0f;
        float minVoltage = 400f;
        Logger.withTag("REGULATION").log("voltage=" + voltage);
        latrController.write(VALUE_REGISTER, voltage);
//        latrController.write(TIME_MIN_PULSE_REGISTER, timeMinPulse);
//        latrController.write(TIME_MAX_PULSE_REGISTER, timeMaxPulse);
//        latrController.write(MIN_DUTTY_REGISTER, minDutty);
//        latrController.write(MAX_DUTTY_REGISTER, maxDutty);
        latrController.write(IR_TIME_PERIOD_MIN, timeMinPulsePercent);
        latrController.write(IR_TIME_PERIOD_MAX, timeMaxPulsePercent);
        latrController.write(IR_TIME_PULSE_MIN_PERCENT, timeMinPeriod);
        latrController.write(IR_TIME_PULSE_MAX_PERCENT, timeMaxPeriod);
        latrController.write(IR_DUTY_MIN_PERCENT, minDuttyPercent);
        latrController.write(IR_DUTY_MAX_PERCENT, maxDuttyPercent);
        latrController.write(REGULATION_TIME_REGISTER, 300000);
        latrController.write(CORRIDOR_REGISTER, corridor);
        latrController.write(DELTA_REGISTER, delta);
        latrController.write(MIN_VOLTAGE_LIMIT_REGISTER, minVoltage);
        latrController.write(START_STOP_REGISTER, 1);
    }

    public void startUpLATRDown(float voltage, boolean isNeedReset) {
        Logger.withTag("STARTUP_LATR").log("startUpLATR");
        if (isNeedReset) {
            latrController.write(START_STOP_REGISTER, 0x5A5A5A5A);
        }
        int minDutty = 400;
        int maxDutty = 200;
        float corridor = 0.05f;
        float delta = 0.05f;
        int timeMinPulse = 50;
        int timeMaxPulse = 300;
        float timeMinPulsePercent = 40.0f;
        float timeMaxPulsePercent = 50.0f;
        float minDuttyPercent = 34.0f;
        float maxDuttyPercent = 36.0f;
        float timeMinPeriod = 500.0f;
        float timeMaxPeriod = 500.0f;
        float minVoltage = 400f;
        Logger.withTag("REGULATION").log("voltage=" + voltage);
        latrController.write(VALUE_REGISTER, voltage);
//        latrController.write(TIME_MIN_PULSE_REGISTER, timeMinPulse);
//        latrController.write(TIME_MAX_PULSE_REGISTER, timeMaxPulse);
//        latrController.write(MIN_DUTTY_REGISTER, minDutty);
//        latrController.write(MAX_DUTTY_REGISTER, maxDutty);
        latrController.write(IR_TIME_PERIOD_MIN, timeMinPulsePercent);
        latrController.write(IR_TIME_PERIOD_MAX, timeMaxPulsePercent);
        latrController.write(IR_TIME_PULSE_MIN_PERCENT, timeMinPeriod);
        latrController.write(IR_TIME_PULSE_MAX_PERCENT, timeMaxPeriod);
        latrController.write(IR_DUTY_MIN_PERCENT, minDuttyPercent);
        latrController.write(IR_DUTY_MAX_PERCENT, maxDuttyPercent);
        latrController.write(REGULATION_TIME_REGISTER, 300000);
        latrController.write(CORRIDOR_REGISTER, corridor);
        latrController.write(DELTA_REGISTER, delta);
        latrController.write(MIN_VOLTAGE_LIMIT_REGISTER, minVoltage);
        latrController.write(START_STOP_REGISTER, 1);
    }

    public void startUpLATRCharge(float voltage, boolean isNeedReset) {
        Logger.withTag("STARTUP_LATR").log("startUpLATR");
        if (isNeedReset) {
            latrController.write(START_STOP_REGISTER, 0x5A5A5A5A);
        }
        int minDutty = 400;
        int maxDutty = 200;
        float corridor = 0.01f;
        float delta = 0.002f;
        int timeMinPulse = 50;
        int timeMaxPulse = 300;
        float timeMinPulsePercent = 25.0f;
        float timeMaxPulsePercent = 25.0f;
        float minDuttyPercent = 50.0f;
        float maxDuttyPercent = 50.0f;
        float timeMinPeriod = 100.0f;
        float timeMaxPeriod = 100.0f;
        float minVoltage = 400f;
        Logger.withTag("REGULATION").log("voltage=" + voltage);
        latrController.write(VALUE_REGISTER, voltage);
//        latrController.write(TIME_MIN_PULSE_REGISTER, timeMinPulse);
//        latrController.write(TIME_MAX_PULSE_REGISTER, timeMaxPulse);
//        latrController.write(MIN_DUTTY_REGISTER, minDutty);
//        latrController.write(MAX_DUTTY_REGISTER, maxDutty);
        latrController.write(IR_TIME_PERIOD_MIN, timeMinPulsePercent);
        latrController.write(IR_TIME_PERIOD_MAX, timeMaxPulsePercent);
        latrController.write(IR_TIME_PULSE_MIN_PERCENT, timeMinPeriod);
        latrController.write(IR_TIME_PULSE_MAX_PERCENT, timeMaxPeriod);
        latrController.write(IR_DUTY_MIN_PERCENT, minDuttyPercent);
        latrController.write(IR_DUTY_MAX_PERCENT, maxDuttyPercent);
        latrController.write(REGULATION_TIME_REGISTER, 300000);
        latrController.write(CORRIDOR_REGISTER, corridor);
        latrController.write(DELTA_REGISTER, delta);
        latrController.write(MIN_VOLTAGE_LIMIT_REGISTER, minVoltage);
        latrController.write(START_STOP_REGISTER, 1);
    }

    public void startUpLATRWithRegulationSpeed(float voltage, boolean isNeedReset, float dutty, float timeMaxPulsePercent) {
        Logger.withTag("STARTUP_LATR").log("startUpLATR");
        if (isNeedReset) {
            latrController.write(START_STOP_REGISTER, 0x5A5A5A5A);
        }
        float corridor = 0.05f;
        float delta = 0.05f;
        float timeMinPulsePercent = 100.0f;
        float timeMinPeriod = 100.0f;
        float timeMaxPeriod = 100.0f;
        float minVoltage = 440f;
        Logger.withTag("REGULATION").log("voltage=" + voltage);
        latrController.write(VALUE_REGISTER, voltage);
        latrController.write(IR_TIME_PERIOD_MIN, timeMinPeriod);
        latrController.write(IR_TIME_PERIOD_MAX, timeMaxPeriod);
        latrController.write(IR_TIME_PULSE_MIN_PERCENT, timeMinPulsePercent);
        latrController.write(IR_TIME_PULSE_MAX_PERCENT, timeMaxPulsePercent);
        latrController.write(IR_DUTY_MIN_PERCENT, dutty);
        latrController.write(IR_DUTY_MAX_PERCENT, dutty);
        latrController.write(REGULATION_TIME_REGISTER, 300000);
        latrController.write(CORRIDOR_REGISTER, corridor);
        latrController.write(DELTA_REGISTER, delta);
        latrController.write(MIN_VOLTAGE_LIMIT_REGISTER, minVoltage);
        latrController.write(START_STOP_REGISTER, 1);
    }


    public void startLATR() {
        latrController.write(START_STOP_REGISTER, 1);
    }

    public void stopLATR() {
        latrController.write(START_STOP_REGISTER, 0);
    }

    public void setKiloAvemShowValue(int value) {
        avemKiloVoltmeterController.write(CHANGE_SHOW_VALUE, value);
    }

    public void initExperimentDevices() {
        pm130Controller.setNeedToRead(true);
        pm130Controller.resetAllAttempts();
        avemVoltmeterController.setNeedToRead(true);
        avemVoltmeterController.resetAllAttempts();
        avemKiloVoltmeterController.setNeedToRead(true);
        avemKiloVoltmeterController.resetAllAttempts();
        owenPRController.setNeedToRead(true);
        owenPRController.resetAllAttempts();
        latrController.setNeedToRead(true);
        latrController.resetAllAttempts();
        deltaCP2000Controller.setNeedToRead(true);
        deltaCP2000Controller.resetAllAttempts();
    }

    public void initLatrOnly() {
        latrController.setNeedToRead(true);
        latrController.resetAllAttempts();
    }

    public void разрешениеНаЗапуск_On() {
        onRegisterInTheKms(1, 1);
    }

    public void короткозамыкатель_On() {
        onRegisterInTheKms(2, 1);
    }

    public void приемКоманды_On() {
        onRegisterInTheKms(3, 1);
    }

    public void последовательнаяСхема_On() {
        onRegisterInTheKms(4, 1);
    }

    public void параллельнаяСхема_On() {
        onRegisterInTheKms(5, 1);
    }

    public void авэм_On() {
        onRegisterInTheKms(6, 1);
    }

    public void звук_On() {
        onRegisterInTheKms(7, 1);
    }

    public void таймер_On() {
        onRegisterInTheKms(8, 1);
    }

    public void разрешениеНаЗапуск_Off() {
        offRegisterInTheKms(1, 1);
    }

    public void короткозамыкатель_Off() {
        offRegisterInTheKms(2, 1);
    }

    public void приемКоманды_Off() {
        offRegisterInTheKms(3, 1);
    }

    public void последовательнаяСхема_Off() {
        offRegisterInTheKms(4, 1);
    }

    public void параллельнаяСхема_Off() {
        offRegisterInTheKms(5, 1);
    }

    public void авэм_Off() {
        offRegisterInTheKms(6, 1);
    }

    public void звук_Off() {
        offRegisterInTheKms(7, 1);
    }

    public void таймер_Off() {
        offRegisterInTheKms(8, 1);
    }

    public void подсветкаТаймер_On() {
        onRegisterInTheKms(1, 2);
    }

    public void подсветкаНапряжения_On() {
        onRegisterInTheKms(2, 2);
    }

    public void внимание_On() {
        onRegisterInTheKms(3, 2);
    }

    public void подсветкаТаймер_Off() {
        offRegisterInTheKms(1, 2);
    }

    public void подсветкаНапряжения_Off() {
        offRegisterInTheKms(2, 2);
    }

    public void внимание_Off() {
        offRegisterInTheKms(3, 2);
    }

    public void setDeviceStateOn(boolean deviceStateOn) {
        isDeviceStateOn = deviceStateOn;
    }
}
