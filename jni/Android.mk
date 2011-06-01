#color conversion lib
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := yuv420sp2rgb
LOCAL_SRC_FILES := yuv420sp2rgb.c

include $(BUILD_SHARED_LIBRARY)

