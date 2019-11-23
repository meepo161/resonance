package ru.avem.resonance.communication.devices.latr;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;
import java.util.Observer;

public class LatrController implements DeviceController {
    private static final short U_RMS_REGISTER = 2;

    private static final short ENDS_STATUS_REGISTER = 140;
    public static final short VALUE_REGISTER = 141;
    public static final short MIN_DUTTY_REGISTER = 142;
    public static final short MAX_DUTTY_REGISTER = 143;
    public static final short REGULATION_TIME_REGISTER = 144;
    public static final short CORRIDOR_REGISTER = 145;
    public static final short DELTA_REGISTER = 146;
    public static final short TIME_MIN_PULSE_REGISTER = 148;
    public static final short TIME_MAX_PULSE_REGISTER = 149;
    public static final short MIN_VOLTAGE_LIMIT_REGISTER = 150;
    public static final short START_STOP_REGISTER = 151;
    public static final short IR_TIME_PULSE_MAX_PERCENT = 171;
    public static final short IR_TIME_PULSE_MIN_PERCENT = 172;
    public static final short IR_DUTY_MAX_PERCENT = 173;
    public static final short IR_DUTY_MIN_PERCENT = 174;
    public static final short IR_TIME_PERIOD_MAX = 175;
    public static final short IR_TIME_PERIOD_MIN = 176;


    private static final int NUM_OF_WORDS_IN_REGISTER = 1;
    private static final short NUM_OF_REGISTERS = NUM_OF_WORDS_IN_REGISTER;

    private LatrModel model;
    private byte address;
    private ModbusController modbusController;
    private byte readAttempt = NUMBER_OF_READ_ATTEMPTS;
    private byte readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    private byte writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    private byte writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    private boolean isNeedToRead;

    public LatrController(int address, Observer observer, ModbusController controller, int id) {
        this.address = (byte) address;
        model = new LatrModel(observer, id);
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
        int type = (int) args[0];
        if (type == 0) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
            if (thereAreReadAttempts()) {
                readAttempt--;
                ModbusController.RequestStatus status = modbusController.readStatus(
                        address, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    resetReadAttempts();
                    resetReadAttemptsOfAttempts();
                    inputBuffer.position(2);
                    model.setStatus(inputBuffer.get());
                } else {
                    read(args);
                }
            } else {
                readAttemptOfAttempt--;
                if (readAttemptOfAttempt <= 0) {
                    model.setReadResponding(false);
                } else {
                    resetReadAttempts();
                }
            }
        } else if (type == 1) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
            if (thereAreReadAttempts()) {
                readAttempt--;
                ModbusController.RequestStatus status = modbusController.readInputRegisters(
                        address, ENDS_STATUS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    resetReadAttempts();
                    resetReadAttemptsOfAttempts();
                    model.setEndsStatus(inputBuffer.getInt());
                } else {
                    read(args);
                }
            } else {
                readAttemptOfAttempt--;
                if (readAttemptOfAttempt <= 0) {
                    model.setReadResponding(false);
                } else {
                    resetReadAttempts();
                }
            }
        } else if (type == 2) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
            if (thereAreReadAttempts()) {
                readAttempt--;
                ModbusController.RequestStatus status = modbusController.readInputRegisters(
                        address, U_RMS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    resetReadAttempts();
                    resetReadAttemptsOfAttempts();
                    model.setU(inputBuffer.getFloat());
                } else {
                    read(args);
                }
            } else {
                readAttemptOfAttempt--;
                if (readAttemptOfAttempt <= 0) {
                    model.setReadResponding(false);
                } else {
                    resetReadAttempts();
                }
            }
        }
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