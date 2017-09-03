LOCAL_PATH := $(call my-dir)

LOCAL_LDLIBS := -llog

#
# libogg
#
include $(CLEAR_VARS)

OGG_DIR   := $(LOCAL_PATH)/libogg-1.3.1

include $(OGG_DIR)/Android.mk

#
# libopus
#
include $(CLEAR_VARS)

OPUS_DIR   := $(LOCAL_PATH)/opus-1.1.1

include $(OPUS_DIR)/Android.mk

#
# opus wrapper
#
include $(CLEAR_VARS)

LOCAL_C_INCLUDES    := \
$(LOCAL_PATH)/opus-1.1.1/include \
$(LOCAL_PATH)/libogg-1.3.1/include

#LOCAL_CFLAGS    := -UHAVE_CONFIG_H
LOCAL_CFLAGS    := -UHAVE_CONFIG_H -DFIXED_POINT=1

#opus-1.1.1/silk \
#opus-1.1.1/silk/fixed \
#opus-1.1.1/celt
LOCAL_MODULE := opusenc
LOCAL_SRC_FILES := opusenc.c lpc.c opus_header.c audio-in.c
LOCAL_SHARED_LIBRARIES = libopus libogg

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
