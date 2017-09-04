#include "org_rfcx_guardian_I2cLib.h"
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>
#include <unistd.h>
#include <stdio.h>

#include <linux/i2c.h>

#include <memory.h>
#include <malloc.h>
//library for log
#include <android/log.h>

#define APPNAME "BiemmeI2c"
static const char *TAG="org_rfcx";

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

//should be modified according your package name
JNIEXPORT jint JNICALL Java_org_rfcx_guardian_i2c_I2cLib_writeI2C(JNIEnv * env, jclass clazz)
{
char *bufByte;
int slaveAddr = 0x4;
int res = 0, i = 0, j = 0;
int mode = 0;
int len = 10;
//i2c slave device address
    //open the file that corresponds to the i2c on the Ltouch board
    int fd = open("/dev/i2c-1", O_RDWR);
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "OPEN i2c, file handler: %d", fd);
    //allocate memory for the array that will contains the data to be written
    bufByte = (char*) malloc((len + 1)*sizeof(char));
    if (bufByte == 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "i2c no memory available");
        return -1;
}
    bufByte[0] = mode;
    for (i = 1; i < len+1; i++){
        //store into the array 10 values starting from 65.
        //Why 65? Because the Arduino sketch will interpret them as character values
        //so will be displayed in the monitor as A,B,C, ... etc
        bufByte[i] = i+64;
}
    //specify the Address of the slave device (in my project the Arduino)
    res = ioctl(fd, I2C_SLAVE, slaveAddr);
    if (res != 0) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Can't set slave address");
return -2; }
    //write data
    if ((j = write(fd, bufByte, len)) != len) {
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "write fail i2c (%d)", j);
return -3; }
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "I2C: %d byte(s) written", j);
    //free memory and close file handler
    free(bufByte);
    close(fd);
    //return 0 when no errors occur, negative value otherwise
    return 0;
}
