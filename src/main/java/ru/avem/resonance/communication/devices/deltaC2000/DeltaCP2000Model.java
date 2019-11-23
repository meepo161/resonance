package ru.avem.resonance.communication.devices.deltaC2000;

import java.util.Observable;
import java.util.Observer;

public class DeltaCP2000Model extends Observable {
    public static final int RESPONDING_PARAM = 0;
    private static final int ERROR1_PARAM = 1;
    private static final int ERROR2_PARAM = 2;
    private static final int CURRENT_FREQUENCY_PARAM = 3;
    public static final int ENDS_STATUS_PARAM = 4;
    public static final int STATUS_VFD = 5;

    private int deviceID;
    private boolean readResponding;
    private boolean writeResponding;

    DeltaCP2000Model(Observer observer, int deviceID) {
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

    void setEndsStatus(short endsStatus) {
        notice(ENDS_STATUS_PARAM, endsStatus);
    }

    void setStatusVfd(short statusVFD) {
        notice(STATUS_VFD, statusVFD);
    }

    void setErrors(short errors) {
        notice(ERROR1_PARAM, (short) ((errors >> 8) & 0xFF));
        notice(ERROR2_PARAM, (short) (errors & 0xFF));
    }

    void setCurrentFrequency(short currentFrequency) {
        notice(CURRENT_FREQUENCY_PARAM, currentFrequency);
    }

    private void notice(int param, Object value) {
        setChanged();
        notifyObservers(new Object[]{deviceID, param, value});
    }
}