package ru.avem.resonance.communication.devices.pr200;

import java.util.Observable;
import java.util.Observer;

public class OwenPRModel extends Observable {
    public static final int RESPONDING_PARAM = 0;
    private static final int РУЧНОЙ_РЕЖИМ = 1;
    public static final int РУЧНОЙ_РЕЖИМ_С_ПК = 2;
    public static final int ПЕРЕМЕННОЕ = 3;
    public static final int ПЕРЕМЕННОЕ_С_РЕЗОНАНСОМ = 4;
    public static final int ПОСТОЯННОЕ = 5;
    public static final int КОНТРОЛЬ_РУБИЛЬНИКА = 6;
    public static final int СТАРТ_ТАЙМЕР = 7;
    public static final int СТОП_ТАЙМЕР = 8;

    public static final int КОНТРОЛЬ_ПУСКА = 9;
    public static final int ТКЗ_ДО_ТРАНСФОРМАТОРА = 10;
    public static final int КОНТРОЛЬ_ДВЕРЕЙ_ШСО = 11;
    public static final int ТКЗ_ОИ = 12;
    public static final int ТКЗ_ПОСЛЕ_ТРАНСФОРМАТОРА = 13;
    public static final int СТОП_ИСПЫТАНИЯ = 14;
    public static final int ПОДЪЕМ_НАПРЯЖЕНИЯ = 15;
    public static final int УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ = 16;
    private static final int УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ_ОТЖАТ = 17;

    private int deviceID;
    private boolean readResponding;
    private boolean writeResponding;

    OwenPRModel(Observer observer, int deviceID) {
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

    void setStatesProtections(short statesProtections) {
        notice(ТКЗ_ДО_ТРАНСФОРМАТОРА, (statesProtections & 0b1) > 0);
        notice(ТКЗ_ОИ, (statesProtections & 0b10) > 0);
        notice(ТКЗ_ПОСЛЕ_ТРАНСФОРМАТОРА, (statesProtections & 0b100) > 0);
        notice(КОНТРОЛЬ_ДВЕРЕЙ_ШСО, (statesProtections & 0b1000) > 0);
        notice(КОНТРОЛЬ_ПУСКА, (statesProtections & 0b10000) > 0);
        notice(КОНТРОЛЬ_РУБИЛЬНИКА, (statesProtections & 0b100000) > 0);
    }

    void setMode(short mode) {
        notice(РУЧНОЙ_РЕЖИМ, (mode & 0b1) > 0);
        notice(РУЧНОЙ_РЕЖИМ_С_ПК, (mode & 0b10) > 0);
        notice(ПЕРЕМЕННОЕ, (mode & 0b100) > 0);
        notice(ПЕРЕМЕННОЕ_С_РЕЗОНАНСОМ, (mode & 0b1000) > 0);
        notice(ПОСТОЯННОЕ, (mode & 0b10000) > 0);
    }

    void setStatesButtons(short statesButtons) {
        notice(СТАРТ_ТАЙМЕР, (statesButtons & 0b1) > 0);
        notice(СТОП_ТАЙМЕР, (statesButtons & 0b10) > 0);
        notice(СТОП_ИСПЫТАНИЯ, (statesButtons & 0b100) > 0);
        notice(ПОДЪЕМ_НАПРЯЖЕНИЯ, (statesButtons & 0b1000) > 0);
        notice(УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ, (statesButtons & 0b10000) > 0);
        notice(УМЕНЬШЕНИЕ_НАПРЯЖЕНИЯ_ОТЖАТ, (statesButtons & 0b10000) == 0);
    }

    private void notice(int param, Object value) {
        setChanged();
        notifyObservers(new Object[]{deviceID, param, value});
    }
}