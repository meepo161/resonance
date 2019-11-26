package ru.avem.resonance.db.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import ru.avem.resonance.model.Point;

import javax.xml.bind.annotation.XmlRootElement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

@XmlRootElement
@DatabaseTable(tableName = "protocols")
public class Protocol {
    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField
    private String serialNumber;
    @DatabaseField
    private String type;
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> timesResonance = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> voltageResonance = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> speedResonance = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> timesViu = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> voltageViu = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> speedViu = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> timesViuDC = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> voltageViuDC = new ArrayList<>(0);
    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Double> speedViuDC = new ArrayList<>(0);
    @DatabaseField
    private String typeExperiment;
    @DatabaseField
    private String position1;
    @DatabaseField
    private String position1Number;
    @DatabaseField
    private String position1FullName;
    @DatabaseField
    private String position2;
    @DatabaseField
    private String position2Number;
    @DatabaseField
    private String position2FullName;
    @DatabaseField
    private long millis = System.currentTimeMillis();
    @DatabaseField
    private String date;

    @DatabaseField(canBeNull = false, dataType = DataType.SERIALIZABLE)
    private ArrayList<Point> points;

    public Protocol() {
        // ORMLite and XML binder need a no-arg constructor
    }

    public Protocol(String serialNumber, TestItem selectedTestItem, Account firstTester, Account secondTester, long millis) {
        setObject(selectedTestItem);
        this.serialNumber = serialNumber;
        this.position1 = firstTester.getPosition();
        this.position1Number = firstTester.getNumber();
        this.position1FullName = firstTester.getFullName();
        this.position2 = secondTester.getPosition();
        this.position2Number = secondTester.getNumber();
        this.position2FullName = secondTester.getFullName();
        this.millis = millis;
        this.date = new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(millis);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public TestItem getObject() {
        return new TestItem(type, timesResonance, voltageResonance, speedResonance,
                timesViu, voltageViu, speedViu,
                timesViuDC, voltageViuDC, speedViuDC);
    }

    public void setObject(TestItem object) {
        type = object.getType();
        timesResonance = object.getTimesResonance();
        voltageResonance = object.getVoltageResonance();
        speedResonance = object.getSpeedResonance();
        timesViu = object.getTimesViu();
        voltageViu = object.getVoltageViu();
        speedViu = object.getSpeedViu();
        timesViuDC = object.getTimesViuDC();
        voltageViuDC = object.getVoltageViuDC();
        speedViuDC = object.getSpeedViuDC();
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
        this.date = new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(millis);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<Double> getTimesResonance() {
        return timesResonance;
    }

    public void setTimesResonance(ArrayList<Double> timesResonance) {
        this.timesResonance = timesResonance;
    }

    public ArrayList<Double> getVoltageResonance() {
        return voltageResonance;
    }

    public void setVoltageResonance(ArrayList<Double> voltageResonance) {
        this.voltageResonance = voltageResonance;
    }

    public ArrayList<Double> getTimesViu() {
        return timesViu;
    }

    public void setTimesViu(ArrayList<Double> timesViu) {
        this.timesViu = timesViu;
    }

    public ArrayList<Double> getVoltageViu() {
        return voltageViu;
    }

    public void setVoltageViu(ArrayList<Double> voltageViu) {
        this.voltageViu = voltageViu;
    }

    public ArrayList<Double> getTimesViuDC() {
        return timesViuDC;
    }

    public void setTimesViuDC(ArrayList<Double> timesViuDC) {
        this.timesViuDC = timesViuDC;
    }

    public ArrayList<Double> getVoltageViuDC() {
        return voltageViuDC;
    }

    public void setVoltageViuDC(ArrayList<Double> voltageViuDC) {
        this.voltageViuDC = voltageViuDC;
    }

    public ArrayList<Double> getSpeedResonance() {
        return speedResonance;
    }

    public void setSpeedResonance(ArrayList<Double> speedResonance) {
        this.speedResonance = speedResonance;
    }

    public ArrayList<Double> getSpeedViu() {
        return speedViu;
    }

    public void setSpeedViu(ArrayList<Double> speedViu) {
        this.speedViu = speedViu;
    }

    public ArrayList<Double> getSpeedViuDC() {
        return speedViuDC;
    }

    public void setSpeedViuDC(ArrayList<Double> speedViuDC) {
        this.speedViuDC = speedViuDC;
    }

    public String getPosition1() {
        return position1;
    }

    public void setPosition1(String position1) {
        this.position1 = position1;
    }

    public String getPosition1Number() {
        return position1Number;
    }

    public void setPosition1Number(String position1Number) {
        this.position1Number = position1Number;
    }

    public String getPosition1FullName() {
        return position1FullName;
    }

    public void setPosition1FullName(String position1FullName) {
        this.position1FullName = position1FullName;
    }

    public String getPosition2() {
        return position2;
    }

    public void setPosition2(String position2) {
        this.position2 = position2;
    }

    public String getPosition2Number() {
        return position2Number;
    }

    public void setPosition2Number(String position2Number) {
        this.position2Number = position2Number;
    }

    public String getPosition2FullName() {
        return position2FullName;
    }

    public void setPosition2FullName(String position2FullName) {
        this.position2FullName = position2FullName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTypeExperiment() {
        return typeExperiment;
    }

    public void setTypeExperiment(String typeExperiment) {
        this.typeExperiment = typeExperiment;
    }

    public ArrayList<Point> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<Point> points) {
        this.points = points;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("Время проведения испытания: HH:mm:ss");
        return String.format("%s. № %s (%s) %s", id, serialNumber, type, sdf.format(millis));
    }
}