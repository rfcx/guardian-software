#Backing up previous LOCAL_PATH so it does not screw with the root Android.mk file
#LOCAL_PATH_OLD := $(LOCAL_PATH)
#LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libogg

LOCAL_CFLAGS    := \
    -DFIXED_POINT -DUSE_KISS_FFT -DEXPORT="" -UHAVE_CONFIG_H \
    -D_ANDROID
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/libogg-1.3.1/include
LOCAL_SRC_FILES := \
    libogg-1.3.1/src/bitwise.c \
    libogg-1.3.1/src/framing.c

include $(BUILD_SHARED_LIBRARY)

#Putting previous LOCAL_PATH back here
#LOCAL_PATH := $(LOCAL_PATH_OLD)
