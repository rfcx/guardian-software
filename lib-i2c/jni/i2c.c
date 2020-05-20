#include <stdio.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

#define MAX_PATH 50

int i2cOpenAdapter(uint8_t adapterNumber) {
    char fsBuf[MAX_PATH];
    int i2cAdapter;

    snprintf(fsBuf, sizeof(fsBuf), "/dev/i2c-%d", adapterNumber);

    i2cAdapter = open(fsBuf, O_RDWR);

    if (i2cAdapter < 0) {
        return -1;
    }

    return i2cAdapter;
}


int i2cSetSlave(int i2cAdapter, uint8_t address) {
    if (ioctl(i2cAdapter, I2C_SLAVE, address) < 0) {
        return -1;
    }

    return 0;
}

int i2cSetAddress(int i2cAdapter, unsigned char address) {
    if (i2c_smbus_write_byte(i2cAdapter, address) < 0) {
        return -1;
    }

    return 0;
}

int i2cWriteByte(int i2cAdapter, unsigned char address, unsigned char byte)
{
	unsigned char buff[2];

   	buff[0] = i2cAdapter;
   	buff[1] = byte;

	if(write(i2cAdapter, buff, 2)!=2)
	{
		return -1;
	}

	return 0;
}

int i2cWriteWord(int i2cAdapter, unsigned char address, unsigned short word) {

    if(i2c_smbus_write_word_data(i2cAdapter, address, word)){
        return -1;
    }

    return 0;
}

int i2cReadByte(int i2cAdapter, unsigned char address) {
    int byte;

    i2cSetAddress(i2cAdapter, address);

    if ((byte = i2c_smbus_read_byte(i2cAdapter)) < 0) {
        return -1;
    }

    return byte;
}

int i2cReadWord(int i2cAdapter, unsigned char address) {
    int value;

    if ((value = i2c_smbus_read_word_data(i2cAdapter, address)) < 0) {
        return -1;
    }

    return value;
}

void i2cClose(int i2cAdapter) {
    close(i2cAdapter);

    i2cAdapter = -1;
}