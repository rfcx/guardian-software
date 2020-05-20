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

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cWriteByte(JNIEnv *env, jobject this, jint i2cAdapter, jbyte mainAddress, jbyte address)
{
	jint ret;
	ret = i2cWriteByte(i2cAdapter, mainAddress, address) ;

	if ( ret == -1 ) {
		__android_log_print(ANDROID_LOG_ERROR, TAG, "i2cWriteByte(%d, %d) failed!", (unsigned int) i2cAdapter, (unsigned int) address);
		return JNI_FALSE;
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cWriteByte(%d, %d) succeeded", (unsigned int) i2cAdapter, (unsigned int) address);
	}

	return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_rfcx_guardian_i2c_I2cTools_i2cWriteBytes(JNIEnv *env, jobject this, jint i2cAdapter,jbyte address, jint length, jbyteArray byteArray)
{
	jint ret;
	int i;

	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, byteArray, NULL);

	uint8_t bytes[length] ;

	for(i=0; i<length; i++)
	{
		bytes[i] = bufferPtr[i];
	}

	(*env)->ReleaseByteArrayElements(env, byteArray, bufferPtr, 0);

	ret = i2cWriteBytes(i2cAdapter, address, length, bytes) ;

	if ( ret == -1 ) {
		__android_log_print(ANDROID_LOG_ERROR, TAG, "i2cWriteBytes(%d, %d, bytearray) failed!", (unsigned int) i2cAdapter, (unsigned int) length);
		return JNI_FALSE;
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, TAG, "i2cWriteBytes(%d, %d, bytearray) succeeded", (unsigned int) i2cAdapter, (unsigned int) length);
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
