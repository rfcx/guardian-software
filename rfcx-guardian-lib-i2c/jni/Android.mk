LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := org_rfcx_guardian_I2cLib.c
LOCAL_MODULE := org_rfcx_guardian_I2cLib

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

