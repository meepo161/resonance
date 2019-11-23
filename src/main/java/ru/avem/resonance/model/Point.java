package ru.avem.resonance.model;

import java.io.Serializable;

public class Point implements Serializable {
    private String measuringUOut;
    private String measuringIC;
    private String measuringTime;

    public Point(String measuringUOut, String measuringIC, String measuringTime) {
        this.measuringUOut = measuringUOut;
        this.measuringIC = measuringIC;
        this.measuringTime = measuringTime;
    }

    public String getMeasuringUOut() {
        return measuringUOut;
    }

    public String getMeasuringIC() {
        return measuringIC;
    }

    public String getMeasuringTime() {
        return measuringTime;
    }

    @Override
    public String toString() {
        return "Point{" +
                "measuringUOut=" + measuringUOut +
                ", measuringIC=" + measuringIC +
                ", measuringTime=" + measuringTime +
                '}';
    }
}
