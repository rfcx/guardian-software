LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS := -llog
LOCAL_MODULE := i2c
LOCAL_SRC_FILES := i2c-wrapper.c i2c.c
include $(BUILD_SHARED_LIBRARY)
