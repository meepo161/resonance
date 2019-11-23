package ru.avem.resonance.communication.devices.pm130;

import ru.avem.resonance.communication.devices.DeviceController;
import ru.avem.resonance.communication.modbus.ModbusController;

import java.nio.ByteBuffer;
import java.util.Observer;


public class PM130Controller implements DeviceController {
//    private static final short I1_REGISTER = 13318; // мгновенные
//    private static final short VL1_REGISTER = 13372;
//    private static final short P_REGISTER = 13696;

    private static final short I1_REGISTER = 13958; // за 1 секунду
    private static final short VL1_REGISTER = 14012;
    private static final short P_REGISTER = 14336;
    private static final short F_REGISTER = 14468;

    private static final int CONVERT_BUFFER_SIZE = 4;
    private static final int U_MULTIPLIER = 1;
    private static final float U_DIVIDER = 10.f;
    private static final float I_DIVIDER = 100.f;
    private static final int I_MULTIPLIER = 1000;
    private static final int NUM_OF_WORDS_IN_REGISTER = 2;
    private static final short NUM_OF_REGISTERS = 3 * NUM_OF_WORDS_IN_REGISTER;

    private PM130Model model;
    private ModbusController modbusController;
    private byte readAttempt = NUMBER_OF_READ_ATTEMPTS;
    private byte readAttemptOfAttempt = NUMBER_OF_READ_ATTEMPTS_OF_ATTEMPTS;
    private byte writeAttempt = NUMBER_OF_WRITE_ATTEMPTS;
    private byte writeAttemptOfAttempt = NUMBER_OF_WRITE_ATTEMPTS_OF_ATTEMPTS;
    private boolean isNeedToRead;
    private byte address;

    public PM130Controller(int address, Observer observer, ModbusController controller, int deviceID) {
        this.address = (byte) address;
        model = new PM130Model(observer, deviceID);
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

    public void read(Object... args) {
        if (thereAreReadAttempts()) {
            readAttempt--;
            switch ((Integer) args[0]) {
                case 1:
                    if (!getI().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                        read(args);
                    } else {
                        resetReadAttempts();
                        resetReadAttemptsOfAttempts();
                    }
                    break;
                case 2:
                    if (!getU().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                        read(args);
                    } else {
                        resetReadAttempts();
                        resetReadAttemptsOfAttempts();
                    }
                    break;
                case 3:
                    if (!getP().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
                        read(args);
                    } else {
                        resetReadAttempts();
                        resetReadAttemptsOfAttempts();
                    }
                    break;
                case 4:
                    if (!getF().equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
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
    }

    private ModbusController.RequestStatus getI() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusI = modbusController.readInputRegisters(
                address, I1_REGISTER, NUM_OF_REGISTERS, inputBuffer);
        if (statusI.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setI1(convertUINTtoINT(inputBuffer.getInt()) * I_MULTIPLIER / I_DIVIDER / 1000);
                model.setI2(convertUINTtoINT(inputBuffer.getInt()) * I_MULTIPLIER / I_DIVIDER / 1000);
                model.setI3(convertUINTtoINT(inputBuffer.getInt()) * I_MULTIPLIER / I_DIVIDER / 1000);
            } catch (Exception ignored) {
            }
        }
        return statusI;
    }

    private ModbusController.RequestStatus getU() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusV = modbusController.readInputRegisters(
                address, VL1_REGISTER, NUM_OF_REGISTERS, inputBuffer);
        if (statusV.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setV1(convertUINTtoINT(inputBuffer.getInt()) * U_MULTIPLIER / U_DIVIDER);
                model.setV2(convertUINTtoINT(inputBuffer.getInt()) * U_MULTIPLIER / U_DIVIDER);
                model.setV3(convertUINTtoINT(inputBuffer.getInt()) * U_MULTIPLIER / U_DIVIDER);
            } catch (Exception ignored) {
            }
        }
        return statusV;
    }

    private ModbusController.RequestStatus getP() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusP = modbusController.readInputRegisters(
                address, P_REGISTER, (short) 8, inputBuffer);
        if (statusP.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setP1(convertMidEndianINTtoINT(inputBuffer.getInt()) / 1000.0f);
                inputBuffer.getInt();
                model.setS1(convertUINTtoINT(inputBuffer.getInt()) / 1000.0f);
                model.setCos(convertMidEndianINTtoINT(inputBuffer.getInt()) / 1000.0f);
            } catch (Exception ignored) {
            }
        }
        return statusP;
    }

    //    @NonNull
    private ModbusController.RequestStatus getF() {
        ByteBuffer inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
        ModbusController.RequestStatus statusF = modbusController.readInputRegisters(
                address, F_REGISTER, (short) 2, inputBuffer);
        if (statusF.equals(ModbusController.RequestStatus.FRAME_RECEIVED)) {
            model.setReadResponding(true);
            try {
                model.setF(convertUINTtoINT(inputBuffer.getInt()) / 100.0f);
            } catch (Exception ignored) {
            }
        }
        return statusF;
    }

    @Override
    public void write(Object... args) {
    }

    private long convertUINTtoINT(int i) {
        ByteBuffer convertBuffer = ByteBuffer.allocate(CONVERT_BUFFER_SIZE);
        convertBuffer.clear();
        convertBuffer.putInt(i);
        convertBuffer.flip();
        short rightSide = convertBuffer.getShort();
        short leftSide = convertBuffer.getShort();
        convertBuffer.clear();
        convertBuffer.putShort(leftSide);
        convertBuffer.putShort(rightSide);
        convertBuffer.flip();
        int preparedInt = convertBuffer.getInt();
        return (long) preparedInt & 0xFFFFFFFFL;
    }

    private int convertMidEndianINTtoINT(int i) {
        ByteBuffer convertBuffer = ByteBuffer.allocate(CONVERT_BUFFER_SIZE);
        convertBuffer.clear();
        convertBuffer.putInt(i);
        convertBuffer.flip();
        short rightSide = convertBuffer.getShort();
        short leftSide = convertBuffer.getShort();
        convertBuffer.clear();
        convertBuffer.putShort(leftSide);
        convertBuffer.putShort(rightSide);
        convertBuffer.flip();
        return convertBuffer.getInt();
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