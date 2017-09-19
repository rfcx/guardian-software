LOCAL_PATH := $(call my-dir)

LOCAL_LDLIBS := -llog

#
# i2clib
#
include $(CLEAR_VARS)
LOCAL_MODULE := i2clib
LOCAL_SRC_FILES := i2clib.c

LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
