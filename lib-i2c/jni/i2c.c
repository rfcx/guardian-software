#include <stdio.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

#define MAX_PATH 50

int i2cOpenAdaptor(uint8_t adaptorNumber) {
    char fsBuf[MAX_PATH];
    int i2cFD;

    snprintf(fsBuf, sizeof(fsBuf), "/dev/i2c-%d", adaptorNumber);

    i2cFD = open(fsBuf, O_RDWR);

    if (i2cFD < 0) {
        return -1;
    }

    return i2cFD;
}


int i2cSetSlave(int i2cFD, uint8_t address) {
    if (ioctl(i2cFD, I2C_SLAVE, address) < 0) {
        return -1;
    }

    return 0;
}

int i2cSetAddress(int i2cFD, unsigned char add) {
    if (i2c_smbus_write_byte(i2cFD, add) < 0) {
        return -1;
    }

    return 0;
}

int i2cReadByte(int i2cFD, unsigned char add) {
    int byte;

    i2cSetAddress(i2cFD, add);

    if ((byte = i2c_smbus_read_byte(i2cFD)) < 0) {
        return -1;
    }

    return byte;
}

void i2cClose(int i2cFD) {
    close(i2cFD);

    i2cFD = -1;
}