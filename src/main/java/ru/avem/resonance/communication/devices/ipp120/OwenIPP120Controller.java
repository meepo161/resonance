package ru.avem.resonance.communication.devices.ipp120;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;

public class OwenIPP120Controller implements DeviceController {

    private byte address;
    private ModbusController modbusController;

    public static final int ТОКОВАЯ_ЗАЩИТА_ОИ = 0;
    public static final int ТОКОВАЯ_ЗАЩИТА_ДО_ТР = 1;
    public static final int ТОКОВАЯ_ЗАЩИТА_ЗА_ТР = 2;
    public static final int ВКЛЮЧИТЕ_РУБИЛЬНИК = 3;
    public static final int НАЖМИТЕ_ПУСК = 4;
    public static final int ПОИСК_РЕЗОНАНСА = 5;
    public static final int ЗАВЕРШЕНИЕ_ОПЫТА = 6;
    public static final int ИНИЦИАЛИЗАЦИЯ_УСТРОЙСТВ = 7;
    public static final int ДВЕРИ_ШКАФА = 8;
    public static final int АВТОМАТИЧЕСКИЙ_РЕЖИМ = 9;
    public static final int РЕЖИМ_ИСПЫТАНИЯ = 10;
    public static final int ОТКЛЮЧИТЕ_РУБИЛЬНИК = 11;
    public static final int ОШИБКА = 12;

    public OwenIPP120Controller(int address, ModbusController controller, int id) {
        this.address = (byte) address;
        modbusController = controller;
    }

    @Override
    public void write(Object... args) {
        short register = (short) args[0];
        int numOfRegisters = (int) args[1];
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ByteBuffer dataBuffer = ByteBuffer.allocate(2 * numOfRegisters);
        for (int i = 2; i < numOfRegisters + 2; i++) {
            dataBuffer.putShort((short) ((int) args[i]));
        }
        dataBuffer.flip();

        modbusController.writeMultipleHoldingRegisters(
                address, register, (short) numOfRegisters, dataBuffer, inputBuffer);
    }

    @Override
    public void resetAllAttempts() {
    }

    @Override
    public void read(Object... args) {
    }

    @Override
    public boolean thereAreReadAttempts() {
        return true;
    }

    @Override
    public boolean thereAreWriteAttempts() {
        return true;
    }

    @Override
    public boolean isNeedToRead() {
        return true;
    }

    @Override
    public void setNeedToRead(boolean isNeedToRead) {
    }

    @Override
    public void resetAllDeviceStateOnAttempts() {
    }
}