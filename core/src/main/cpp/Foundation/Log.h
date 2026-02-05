#include <android/log.h>

#define TAG "PrisonNative"

#if 1
#define log_print_error(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define log_print_warn(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define log_print_debug(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define log_print_error(...)
#define log_print_warn(...)
#define log_print_debug(...)
#endif

#define ALOGE(...) log_print_error(__VA_ARGS__)
#define ALOGW(...) log_print_warn(__VA_ARGS__)
#define ALOGD(...) log_print_debug(__VA_ARGS__)

#ifndef SPEED_LOG_H
#define SPEED_LOG_H 1

#endif
