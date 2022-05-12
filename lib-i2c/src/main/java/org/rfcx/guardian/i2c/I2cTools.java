package org.rfcx.guardian.i2c;

public class I2cTools {

    static {
        System.loadLibrary("i2c");
    }

    public int i2cInit(int number) {
        return i2cOpenAdapter(number);
    }

    public String i2cGet(int i2cAdapter, int mainAddress, int dataAddress, boolean returnDecimal, boolean isWordData) throws Exception {
        int value;

        i2cSetSlave(i2cAdapter, mainAddress);
        if (isWordData) {
            value = i2cReadWord(i2cAdapter, (byte) dataAddress);
            if (value < 0) {
                throw new Exception("Read Failed");
            }
        } else {
            if (i2cSetSlave(i2cAdapter, mainAddress)) {
                value = i2cReadByte(i2cAdapter, (byte) dataAddress);
                if (value < 0) {
                    throw new Exception("Read Failed");
                }
            } else {
                throw new Exception("Set Slave Failed");
            }
        }
        return (returnDecimal) ? value + "" : String.format("0x%x", value);
    }

    public String i2cGet(int i2cAdapter, String mainAddress, String dataAddress, boolean returnDecimal, boolean isWordData) throws Exception {
        return i2cGet(i2cAdapter, hexStringToInt(mainAddress), hexStringToInt(dataAddress), returnDecimal, isWordData);
    }

    public boolean i2cSet(int i2cAdapter, int mainAddress, int dataAddress, int data, boolean isWordData) {
        i2cSetSlave(i2cAdapter, mainAddress);
        if (isWordData) {
            return i2cWriteWord(i2cAdapter, (byte) dataAddress, (char) data);
        }
        return i2cWriteByte(i2cAdapter, (byte) dataAddress, (byte) data);
    }

    public boolean i2cSet(int i2cAdapter, String mainAddress, String dataAddress, String data, boolean isWordData) {
        return i2cSet(i2cAdapter, hexStringToInt(mainAddress), hexStringToInt(dataAddress), hexStringToInt(data), isWordData);
    }

    public void i2cDeInit(int i2cAdapter) {
        i2cClose(i2cAdapter);
    }

    private int hexStringToInt(String hex) {
        return Integer.decode(hex);
    }

    private native int i2cOpenAdapter(int adapterNumber);

    private native boolean i2cSetSlave(int i2cAdapter, int address);

    private native boolean i2cWriteByte(int i2cAdapter, byte dataAddress, byte data);

    private native boolean i2cWriteWord(int i2cAdapter, byte dataAddress, char data);

    private native int i2cReadByte(int i2cAdapter, byte address);

    private native int i2cReadWord(int i2cAdapter, byte address);

    private native void i2cClose(int i2cAdapter);
}
