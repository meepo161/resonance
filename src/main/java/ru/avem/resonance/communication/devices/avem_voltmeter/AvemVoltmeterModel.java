package ru.avem.resonance.communication.devices.avem_voltmeter;

import java.util.Observable;
import java.util.Observer;

public class AvemVoltmeterModel extends Observable {
    public static final int RESPONDING_PARAM = 0;
    public static final int U_AMP_PARAM = 1;
    public static final int U_RMS_PARAM = 2;
    private int deviceID;
    private boolean readResponding;
    private boolean writeResponding;

    AvemVoltmeterModel(Observer observer, int deviceID) {
        addObserver(observer);
        this.deviceID = deviceID;
    }


 void resetResponding() {
        readResponding = true;
        writeResponding = true;
    }

    void setReadResponding(boolean readResponding) {
        this.readResponding = readResponding;
        setResponding();
    }

    void setWriteResponding(boolean writeResponding) {
        this.writeResponding = writeResponding;
        setResponding();
    }

    private void setResponding() {
        notice(RESPONDING_PARAM, readResponding && writeResponding);
    }

    void setUAMP(float u) {
        notice(U_AMP_PARAM, u);
    }

    void setURMS(float u) {
        notice(U_RMS_PARAM, u);
    }

    private void notice(int param, Object value) {
        setChanged();
        notifyObservers(new Object[]{deviceID, param, value});
    }
}