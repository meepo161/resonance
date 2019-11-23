package ru.avem.resonance.communication.devices.latr;

import java.util.Observable;
import java.util.Observer;

public class LatrModel extends Observable {
    public static final int RESPONDING_PARAM = 0;
    public static final int STATUS_PARAM = 1;
    private static final int ENDS_STATUS_PARAM = 2;
    public static final int U_PARAM = 3;
    private int deviceID;
    private boolean readResponding;
    private boolean writeResponding;

    LatrModel(Observer observer, int deviceID) {
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

    void setStatus(byte status) {
        notice(STATUS_PARAM, status);
    }

    void setEndsStatus(int endsStatus) {
        notice(ENDS_STATUS_PARAM, endsStatus);
    }

    void setU(float u) {
        notice(U_PARAM, u);
    }

    private void notice(int param, Object value) {
        setChanged();
        notifyObservers(new Object[]{deviceID, param, value});
    }

}