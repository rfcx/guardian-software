LOCAL_PATH := $(call my-dir)

#LOCAL_LDLIBS := -llog

#
# libogg
#
include $(CLEAR_VARS)
OGG_VERSION := 1.3.3
OGG_DIR   := $(LOCAL_PATH)/libogg-$(OGG_VERSION)
include $(OGG_DIR)/Android.mk

#
# libflac
#
include $(CLEAR_VARS)
FLAC_VERSION := 1.3.2
FLAC_DIR   := $(LOCAL_PATH)/flac-$(FLAC_VERSION)
include $(FLAC_DIR)/Android.mk

#
# libopus
#
include $(CLEAR_VARS)
OPUS_VERSION := 1.3
OPUS_DIR   := $(LOCAL_PATH)/opus-$(OPUS_VERSION)
include $(OPUS_DIR)/Android.mk

#
# opus wrapper
#
include $(CLEAR_VARS)
LOCAL_C_INCLUDES    := \
$(LOCAL_PATH)/opus-$(OPUS_VERSION)/include \
$(LOCAL_PATH)/libogg-$(OGG_VERSION)/include

#LOCAL_CFLAGS    := -UHAVE_CONFIG_H
LOCAL_CFLAGS    := -UHAVE_CONFIG_H -DFIXED_POINT=1

#opus-$(OPUS_VERSION)/silk \
#opus-$(OPUS_VERSION)/silk/fixed \
#opus-$(OPUS_VERSION)/celt
LOCAL_MODULE := opusenc
LOCAL_SRC_FILES := opusenc.c lpc.c opus_header.c audio-in.c
LOCAL_SHARED_LIBRARIES = libopus libogg

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
