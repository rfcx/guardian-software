#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#define TAG "Rfcx-I2c"
#define BUFFER_SIZE 64

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cOpenAdapter(JNIEnv *env, jobject this,
                                                            jint adapterNumber) {
    jint ret;
    ret = i2cOpenAdaptor(adapterNumber);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cOpenAdaptor(%d) failed!",
                            (unsigned int) adapterNumber);
        ret = -1;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cOpenAdaptor(%d) succeeded",
                            (unsigned int) adapterNumber);
    }

    return ret;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cSetSlave(JNIEnv *env, jobject this, jint i2cAdapter,
                                                             jint address) {
    jint ret;
    ret = i2cSetSlave(i2cAdapter, address);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cSetSlave(%d, %d) failed!",
                            (unsigned int) i2cAdapter, (unsigned int) address);
        return JNI_FALSE;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cSetSlave(%d, %d) succeeded",
                            (unsigned int) i2cAdapter, (unsigned int) address);
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cReadByte(JNIEnv *env, jobject this, jint i2cAdapter,
                                                         jbyte address) {
    jint ret;
    ret = i2cReadByte(i2cAdapter, address);

    if (ret == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "i2cReadByte(%d, %d) failed!",
                            (unsigned int) i2cAdapter, (unsigned int) address);
        return -1;
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cReadByte(%d, %d) succeeded",
                            (unsigned int) i2cAdapter, (unsigned int) address);
    }

    return ret;
}

JNIEXPORT void JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cClose(JNIEnv *env, jobject this, jint i2cAdapter) {
    i2cClose(i2cAdapter);

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cClose(%d) succeeded",
                        (unsigned int) i2cAdapter);

}
