package ru.avem.resonance.communication.devices.pr200;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;
import java.util.Observer;

public class OwenPRController implements DeviceController {
    private static final short STATES_PROTECTIONS_REGISTER = 513;
    public static final short MODE_REGISTER = 514;
    private static final short STATES_BUTTONS_REGISTER = 515;
    public static final short KMS1_REGISTER = 516;
    public static final short KMS2_REGISTER = 517;
    public static final short RESET_DOG = 512;
    public static final short RES = 518;

    private static final int NUM_OF_WORDS_IN_REGISTER = 1;
    private static final short NUM_OF_REGISTERS = 2 * NUM_OF_WORDS_IN_REGISTER;

    private OwenPRModel model;
    private byte address;
    private ModbusController modbusController;
    private byte readAttempt = NUMBER_OF_READ_ATTEMPTS;
    private byte readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    private byte writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    private byte writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    private boolean isNeedToRead;

    public OwenPRController(int address, Observer observer, ModbusController controller, int id) {
        this.address = (byte) address;
        model = new OwenPRModel(observer, id);
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
                ModbusController.RequestStatus status = modbusController.readInputRegisters(
                        address, STATES_PROTECTIONS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    resetReadAttempts();
                    model.setStatesProtections(inputBuffer.getShort());
                    model.setMode(inputBuffer.getShort());
//                model.setStatesButtons(inputBuffer.getShort());
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
                ModbusController.RequestStatus status = modbusController.readInputRegisters(
                        address, STATES_BUTTONS_REGISTER, NUM_OF_REGISTERS, inputBuffer);
                if (status.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                    model.setReadResponding(true);
                    resetReadAttempts();
                    model.setStatesButtons(inputBuffer.getShort());
                    inputBuffer.getShort();
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
        ByteBuffer dataBuffer = ByteBuffer.allocate(2 * numOfRegisters);
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