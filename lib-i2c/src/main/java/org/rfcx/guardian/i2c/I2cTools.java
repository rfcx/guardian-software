package org.rfcx.guardian.i2c;

public class I2cTools {

    public int i2cInit(int number) {
        return i2cOpenAdapter(number);
    }

    public String i2cGet(int i2cAdapter, int mainAddress, int dataAddress, boolean returnDecimal) throws Exception {
        if (i2cSetSlave(i2cAdapter, mainAddress)) {
            int value = i2cReadByte(i2cAdapter, (byte) dataAddress);
            if (value < 0) {
                throw new Exception("Read Failed");
            }
            return (returnDecimal) ? value+"" : String.format("0x%x", value);
        }
        throw new Exception("Set Slave Failed");
    }

    public String i2cGet(int i2cAdapter, String mainAddress, String dataAddress, boolean returnDecimal) throws Exception {
        return i2cGet(i2cAdapter, hexStringToInt(mainAddress), hexStringToInt(dataAddress), returnDecimal);
    }

    public void i2cDeInit(int i2cAdapter) {
        i2cClose(i2cAdapter);
    }

    private int hexStringToInt(String hex) {
        return Integer.decode(hex);
    }


    private native int i2cOpenAdapter(int adapterNumber);

    private native boolean i2cSetSlave(int i2cAdapter, int address);

    private native int i2cReadByte(int i2cAdapter, byte address);

    private native void i2cClose(int i2cAdapter);

    static {
        System.loadLibrary("i2c");
    }
}
