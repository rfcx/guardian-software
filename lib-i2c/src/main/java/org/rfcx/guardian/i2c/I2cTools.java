package org.rfcx.guardian.i2c;

public class I2cTools {

    public int i2cInit(int number) {
        return i2cOpenAdaptor(number);
    }

    public String i2cGet(int i2cAdapter, int mainAddress, int dataAddress) {
        if (i2cSetSlave(i2cAdapter, mainAddress)) {
            int value = i2cReadByte(i2cAdapter, (byte) dataAddress);
            if (value < 0) {
                return "Read Failed";
            }
            return String.format("0x%x", value);
        }
        return "Set Slave Failed";
    }

    public void i2cDeInit(int i2cAdapter) {
        i2cClose(i2cAdapter);
    }


    private native int i2cOpenAdaptor(int adaptorNumber);

    private native boolean i2cSetSlave(int i2cFD, int adress);

    private native int i2cReadByte(int i2cFD, byte add);

    private native void i2cClose(int i2cFD);

    static {
        System.loadLibrary("i2c");
    }
}
