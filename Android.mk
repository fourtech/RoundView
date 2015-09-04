LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := RoundLayoutDemo
LOCAL_SRC_FILES := $(call all-java-files-under, src)

include $(BUILD_PACKAGE)
