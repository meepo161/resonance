package ru.avem.resonance.communication.devices.avem_voltmeter;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;
import java.util.Observer;

public class AvemVoltmeterController implements DeviceController {
    private static final short U_AMP_REGISTER = 0;
    private static final short U_RMS_REGISTER = 2;
    public static final short CHANGE_SHOW_VALUE = 108;

    private static final int NUM_OF_WORDS_IN_REGISTER = 1;
    private static final short NUM_OF_REGISTERS = NUM_OF_WORDS_IN_REGISTER;

    private AvemVoltmeterModel model;
    private byte address;
    private ModbusController modbusController;
    private byte readAttempt = NUMBER_OF_READ_ATTEMPTS;
    private byte readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    private byte writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    private byte writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    private boolean isNeedToRead;

    public AvemVoltmeterController(int address, Observer observer, ModbusController controller, int deviceID) {
        this.address = (byte) address;
        model = new AvemVoltmeterModel(observer, deviceID);
        modbusController = controller;
    }

    @Override
    public void resetAllAttempts() {
        resetReadAttempts();
        resetReadAttemptsOfAttempts();
        resetWriteAttempts();
        resetWriteAttemptsOfAttempts();
    }

    private void resetReadAttempts() {
        readAttempt = NUMBER_OF_READ_ATTEMPTS;
    }

    private void resetReadAttemptsOfAttempts() {
        readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    }

    private void resetWriteAttempts() {
        writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    }

    private void resetWriteAttemptsOfAttempts() {
        writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    }

    @Override
    public void read(Object... args) {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        if (thereAreReadAttempts()) {
            readAttempt--;
            switch ((Integer) args[0]) {
                case 0:
                    if (!getUAMP().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                        read(args);
                    } else {
                        resetReadAttempts();
                        resetReadAttemptsOfAttempts();
                    }
                    break;
                case 1:
                    if (!getURMS().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                        read(args);
                    } else {
                        resetReadAttempts();
                        resetReadAttemptsOfAttempts();
                    }
                    break;
            }
        } else {
            readAttemptOfAttempt--;
            if (readAttemptOfAttempt <= 0) {
                model.setReadResponding(false);
            } else {
                resetReadAttempts();
            }
        }

        ModbusController.RequestStatus status = modbusController.readInputRegisters(
                address, U_AMP_REGISTER, NUM_OF_REGISTERS, inputBuffer);

        if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            resetReadAttempts();
            resetReadAttemptsOfAttempts();
            model.setUAMP(inputBuffer.getFloat());
        } else {
            read(args);
        }
    }

    private ModbusController.RequestStatus getUAMP() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusUAMP = modbusController.readInputRegisters(
                address, U_AMP_REGISTER, NUM_OF_REGISTERS, inputBuffer);
        if (statusUAMP.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setReadResponding(true);
                resetReadAttempts();
                resetReadAttemptsOfAttempts();
                model.setUAMP(inputBuffer.getFloat());
            } catch (Exception ignored) {
            }
        }
        return statusUAMP;
    }

    private ModbusController.RequestStatus getURMS() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusURMS = modbusController.readInputRegisters(
                address, U_RMS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
        if (statusURMS.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setReadResponding(true);
                resetReadAttempts();
                resetReadAttemptsOfAttempts();
                model.setURMS(inputBuffer.getFloat());
            } catch (Exception ignored) {
            }
        }
        return statusURMS;
    }

    @Override
    public void write(Object... args) {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        byte[] value = new byte[1];
        if (args[1] instanceof Integer) {
            value = intToByteArray((int) args[1]);
        } else if (args[1] instanceof Float) {
            value = floatToByteArray((float) args[1]);
        }
        if (thereAreWriteAttempts()) {
            writeAttempt--;
            ModbusController.RequestStatus status = modbusController.writeSingleHoldingRegister(address,
                    (short) args[0], value, inputBuffer);
            if (status.equals(ModbusController.RequestStatus.PORT_NOT_INITIALIZED)) {
                return;
            }
            if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                model.setWriteResponding(true);
                resetWriteAttempts();
                resetWriteAttemptsOfAttempts();
            } else {
                write(args);
            }
        } else {
            writeAttemptOfAttempt--;
            if (writeAttemptOfAttempt <= 0) {
                model.setWriteResponding(false);
            } else {
                resetWriteAttempts();
            }
        }
    }

    private byte[] intToByteArray(int i) {
        ByteBuffer convertBuffer = ByteBuffer.allocate(4);
        convertBuffer.clear();
        return convertBuffer.putInt(i).array();
    }

    private byte[] floatToByteArray(float f) {
        ByteBuffer convertBuffer = ByteBuffer.allocate(4);
        convertBuffer.clear();
        return convertBuffer.putFloat(f).array();
    }

    @Override
    public boolean thereAreReadAttempts() {
        return readAttempt > 0;
    }

    @Override
    public boolean thereAreWriteAttempts() {
        return writeAttempt > 0;
    }

    @Override
    public boolean isNeedToRead() {
        return isNeedToRead;
    }

    @Override
    public void setNeedToRead(boolean isNeedToRead) {
        if (isNeedToRead) {
            model.resetResponding();
        }
        this.isNeedToRead = isNeedToRead;
    }

    @Override
    public void resetAllDeviceStateOnAttempts() {
        readAttempt = 1;
        readAttemptOfAttempt = 0;
        writeAttempt = 1;
        writeAttemptOfAttempt = 0;
    }
}