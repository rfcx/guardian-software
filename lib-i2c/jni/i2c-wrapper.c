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
    ret = i2cOpenAdapter(adapterNumber);

    return ret;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cSetSlave(JNIEnv *env, jobject this, jint i2cAdapter,
                                                             jint address) {
    jint ret;
    ret = i2cSetSlave(i2cAdapter, address);

    if (ret == -1) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cWriteByte(JNIEnv *env, jobject this, jint i2cAdapter, jbyte dataAddress, jbyte value)
{
	jint ret;
	ret = i2cWriteByte(i2cAdapter, dataAddress, value) ;

	if ( ret == -1 ) {
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cWriteWord(JNIEnv *env, jobject this, jint i2cAdapter, jbyte dataAddress, jchar value)
{
	jint ret;
	ret = i2cWriteWord(i2cAdapter, dataAddress, value) ;

	if ( ret == -1 ) {
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cReadByte(JNIEnv *env, jobject this, jint i2cAdapter,
                                                         jbyte address) {
    jint ret;
    ret = i2cReadByte(i2cAdapter, address);

    return ret;
}

JNIEXPORT jint JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cReadWord(JNIEnv *env, jobject this, jint i2cAdapter,
                                                         jbyte address) {
    jint ret;
    ret = i2cReadWord(i2cAdapter, address);

    return ret;
}


JNIEXPORT void JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cClose(JNIEnv *env, jobject this, jint i2cAdapter) {
    i2cClose(i2cAdapter);

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cClose(%d) succeeded",
                        (unsigned int) i2cAdapter);

}
