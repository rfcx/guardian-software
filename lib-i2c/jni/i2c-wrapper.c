#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#define TAG "Rfcx-I2c"
#define BUFFER_SIZE 64

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cOpenAdaptor(JNIEnv *env, jobject this,
                                                            jint adaptorNumber) {
    jint ret;
    ret = i2cOpenAdaptor(adaptorNumber);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cOpenAdaptor(%d) failed!",
                            (unsigned int) adaptorNumber);
        ret = -1;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cOpenAdaptor(%d) succeeded",
                            (unsigned int) adaptorNumber);
    }

    return ret;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cSetSlave(JNIEnv *env, jobject this, jint i2cFD,
                                                             jint address) {
    jint ret;
    ret = i2cSetSlave(i2cFD, address);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cSetSlave(%d, %d) failed!",
                            (unsigned int) i2cFD, (unsigned int) address);
        return JNI_FALSE;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cSetSlave(%d, %d) succeeded",
                            (unsigned int) i2cFD, (unsigned int) address);
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cReadByte(JNIEnv *env, jobject this, jint i2cFD,
                                                         jbyte add) {
    jint ret;
    ret = i2cReadByte(i2cFD, add);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cReadByte(%d) failed!",
                            (unsigned int) i2cFD);
        return -1;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cReadByte(%d) succeeded",
                            (unsigned int) i2cFD);
    }

    return ret;
}

JNIEXPORT void JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cClose(JNIEnv *env, jobject this, jint i2cFD) {
    i2cClose(i2cFD);

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cClose(%d, bytearray) succeeded",
                        (unsigned int) i2cFD);

}
