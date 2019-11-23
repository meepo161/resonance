package ru.avem.resonance.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Experiment3Model {

    private final StringProperty voltage;
    private final StringProperty voltageARN;
    private final StringProperty currentB;
    private final StringProperty currentOI;
    private final StringProperty currentA;
    private final StringProperty time;
    private final StringProperty result;
    private List<StringProperty> properties = new ArrayList<>();
    private ArrayList<StringProperty> protocol = new ArrayList<>();


    public Experiment3Model() {
        voltage = new SimpleStringProperty("");
        voltageARN = new SimpleStringProperty("");
        currentB = new SimpleStringProperty("");
        currentOI = new SimpleStringProperty("");
        currentA = new SimpleStringProperty("");
        time = new SimpleStringProperty("");
        result = new SimpleStringProperty("");
        properties.addAll(Arrays.asList(
                voltage,
                voltageARN,
                currentB,
                currentOI,
                currentA,
                time,
                result));
    }

    public Experiment3Model(
            String voltage, String voltageARN, String currentB, String currentOI, String frequency, String coefAmp, String currentA, String time, String result) {
        this.voltage = new SimpleStringProperty(voltage);
        this.voltageARN = new SimpleStringProperty(voltageARN);
        this.currentB = new SimpleStringProperty(currentB);
        this.currentOI = new SimpleStringProperty(currentOI);
        this.currentA = new SimpleStringProperty(currentA);
        this.time = new SimpleStringProperty(time);
        this.result = new SimpleStringProperty(result);
        protocol.addAll(Arrays.asList(
                this.voltage,
                this.voltageARN,
                this.currentB,
                this.currentOI,
                this.currentA,
                this.result
        ));
    }

    public String getVoltage() {
        return voltage.get();
    }

    public StringProperty voltageProperty() {
        return voltage;
    }

    public void setVoltage(String voltage) {
        this.voltage.set(voltage);
    }

    public String getVoltageARN() {
        return voltageARN.get();
    }

    public StringProperty voltageARNProperty() {
        return voltageARN;
    }

    public void setVoltageARN(String voltageARN) {
        this.voltageARN.set(voltageARN);
    }

    public String getCurrentB() {
        return currentB.get();
    }

    public StringProperty currentBProperty() {
        return currentB;
    }

    public void setCurrentB(String currentB) {
        this.currentB.set(currentB);
    }

    public String getCurrentOI() {
        return currentOI.get();
    }

    public StringProperty currentOIProperty() {
        return currentOI;
    }

    public void setCurrentOI(String currentOI) {
        this.currentOI.set(currentOI);
    }

    public String getResult() {
        return result.get();
    }

    public StringProperty resultProperty() {
        return result;
    }

    public void setResult(String result) {
        this.result.set(result);
    }

    public ArrayList<StringProperty> getProtocol() {
        return protocol;
    }

    public void setProtocol(ArrayList<StringProperty> protocol) {
        this.protocol = protocol;
    }

    public List<StringProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<StringProperty> properties) {
        this.properties = properties;
    }

    public void clearProperties() {
        properties.forEach(stringProperty -> stringProperty.set(""));
    }

    public String getCurrentA() {
        return currentA.get();
    }

    public StringProperty currentAProperty() {
        return currentA;
    }

    public void setCurrentA(String currentA) {
        this.currentA.set(currentA);
    }

    public String getTime() {
        return time.get();
    }

    public StringProperty timeProperty() {
        return time;
    }

    public void setTime(String time) {
        this.time.set(time);
    }
}
