package ru.avem.resonance.communication.devices.deltaC2000;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;
import java.util.Observer;

public class DeltaCP2000Controller implements DeviceController {
    private static final short ERRORS_REGISTER = 0x2100;
    public static final short STATUS_REGISTER = 0x2101;
    private static final short ENDS_STATUS_REGISTER = 0x041A;
    public static final short END_UP_CONTROL_REGISTER = 0x0405;
    public static final short END_DOWN_CONTROL_REGISTER = 0x0406;
    public static final short CURRENT_FREQUENCY_INPUT_REGISTER = 0x2103;
    public static final short CONTROL_REGISTER = 0x2000;
    public static final short CURRENT_FREQUENCY_OUTPUT_REGISTER = 0x2001;
    public static final short MAX_FREQUENCY_REGISTER = 0x0100;
    public static final short NOM_FREQUENCY_REGISTER = 0x0101;
    public static final short MAX_VOLTAGE_REGISTER = 0x0102;
    public static final short POINT_1_FREQUENCY_REGISTER = 0x0103;
    public static final short POINT_1_VOLTAGE_REGISTER = 0x0104;
    public static final short POINT_2_FREQUENCY_REGISTER = 0x0105;
    public static final short POINT_2_VOLTAGE_REGISTER = 0x0106;

    private static final int NUM_OF_WORDS_IN_REGISTER = 1;
    private static final short NUM_OF_REGISTERS = 4 * NUM_OF_WORDS_IN_REGISTER;

    private DeltaCP2000Model model;
    private byte address;
    private ModbusController modbusController;
    private byte readAttempt = NUMBER_OF_READ_ATTEMPTS;
    private byte readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    private byte writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    private byte writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    private boolean isNeedToRead;

    public DeltaCP2000Controller(int address, Observer observer, ModbusController controller, int deviceID) {
        this.address = (byte) address;
        model = new DeltaCP2000Model(observer, deviceID);
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
                ModbusController.RequestStatus status = modbusController.readMultipleHoldingRegisters(
                        address, ERRORS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    model.setErrors(inputBuffer.getShort());
                    model.setStatusVfd(inputBuffer.getShort());
                    inputBuffer.getShort();
                    model.setCurrentFrequency(inputBuffer.getShort());
                    resetReadAttempts();
                    resetReadAttemptsOfAttempts();
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
                ModbusController.RequestStatus status = modbusController.readMultipleHoldingRegisters(
                        address, ENDS_STATUS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setEndsStatus(inputBuffer.getShort());
                    inputBuffer.getShort();
                    inputBuffer.getShort();
                    inputBuffer.getShort();
                    resetReadAttempts();
                    resetReadAttemptsOfAttempts();
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
        short register = (short) args[0];
        int numOfRegisters = (int) args[1];
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ByteBuffer dataBuffer = ByteBuffer.allocate(4 * numOfRegisters);
        for (int i = 2; i < numOfRegisters + 2; i++) {
            dataBuffer.putShort((short) ((int) args[i]));
        }
        dataBuffer.flip();

        if (thereAreWriteAttempts()) {
            writeAttempt--;
            ModbusController.RequestStatus status = modbusController.writeMultipleHoldingRegisters(
                    address, register, (short) numOfRegisters, dataBuffer, inputBuffer);
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