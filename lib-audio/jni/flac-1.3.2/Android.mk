LOCAL_PATH_OLD := $(LOCAL_PATH)
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/flac_sources.mk

LOCAL_MODULE := flac

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include \
$(LOCAL_PATH)/src/libFLAC/include \
$(LOCAL_PATH)/../libogg-1.3.3/include

LOCAL_CFLAGS := -O3 -DHAVE_CONFIG_H

LOCAL_SRC_FILES := $(FLAC_SOURCES)

LOCAL_STATIC_LIBRARIES := ogg

include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(LOCAL_PATH_OLD)
