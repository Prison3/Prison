LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libdobby
LOCAL_SRC_FILES := Dobby/$(TARGET_ARCH_ABI)/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := xdl
LOCAL_CXXFLAGS := -std=c++11 -fno-exceptions -fno-rtti
LOCAL_EXPORT_C_INCLUDES:=$(LOCAL_PATH) $(LOCAL_PATH)/Foundation
LOCAL_SRC_FILES := Foundation/xdl/xdl.c \
    Foundation/xdl/xdl_iterate.c \
    Foundation/xdl/xdl_linker.c \
    Foundation/xdl/xdl_lzma.c \
    Foundation/xdl/xdl_util.c
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/Foundation
include $(BUILD_STATIC_LIBRARY)


include $(CLEAR_VARS)
# Collect all source files in the current directory
SRC1 := $(wildcard $(LOCAL_PATH)/*.cpp) $(wildcard $(LOCAL_PATH)/*.c)
# Collect all source files in Foundation/
SRC2 := $(wildcard $(LOCAL_PATH)/Foundation/*.cpp) $(wildcard $(LOCAL_PATH)/Foundation/*.c)
# Collect all source files in Hook/
SRC3 := $(wildcard $(LOCAL_PATH)/Hook/*.cpp) $(wildcard $(LOCAL_PATH)/Hook/*.c)
# JniHook files are now in Foundation/

LOCAL_MODULE := prison
LOCAL_SRC_FILES := Foundation/NativeCore.cpp \
Foundation/SandboxFs.cpp \
Foundation/elf_util.cpp \
Foundation/VirtualSpoof.cpp \
Foundation/Utils.cpp \
Foundation/AntiDetection.cpp \
Hook/VMClassLoaderHook.cpp \
Hook/UnixFileSystemHook.cpp \
Hook/DexFileHook.cpp \
Hook/LibcHook.cpp \
Hook/ZlibHook.cpp \
Hook/RuntimeHook.cpp \
Hook/BinderHook.cpp \
Hook/JniHook.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/Foundation
LOCAL_CFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -std=c++17
LOCAL_CPPFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -Werror -fms-extensions
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all,-z,max-page-size=16384
LOCAL_ARM_MODE := arm

LOCAL_CPP_FEATURES := exceptions
LOCAL_STATIC_LIBRARIES := libdobby xdl
LOCAL_LDLIBS := -llog -landroid -lz
include $(BUILD_SHARED_LIBRARY)
