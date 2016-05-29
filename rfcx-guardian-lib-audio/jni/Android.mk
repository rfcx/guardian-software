LOCAL_PATH := $(call my-dir)

#
# libmp3lame
#
include $(CLEAR_VARS)
		 
LOCAL_MODULE    	:= libmp3lame
LOCAL_SRC_FILES 	:= \
./libmp3lame/bitstream.c \
./libmp3lame/encoder.c \
./libmp3lame/fft.c \
./libmp3lame/gain_analysis.c \
./libmp3lame/id3tag.c \
./libmp3lame/lame.c \
./libmp3lame/mpglib_interface.c \
./libmp3lame/newmdct.c \
./libmp3lame/presets.c \
./libmp3lame/psymodel.c \
./libmp3lame/quantize.c \
./libmp3lame/quantize_pvt.c \
./libmp3lame/reservoir.c \
./libmp3lame/set_get.c \
./libmp3lame/tables.c \
./libmp3lame/takehiro.c \
./libmp3lame/util.c \
./libmp3lame/vbrquantize.c \
./libmp3lame/VbrTag.c \
./libmp3lame/version.c \
./mp3lame.c

LOCAL_LDLIBS := -llog
		
include $(BUILD_SHARED_LIBRARY)

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
