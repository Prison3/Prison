#ifndef PRISON_LOGGER_H
#define PRISON_LOGGER_H

#include <android/log.h>

#define TAG "PrisonNative"

// Log level control: Set to 0 to disable all logs, 1 to enable
#ifndef ENABLE_LOGGING
#define ENABLE_LOGGING 1
#endif

#if ENABLE_LOGGING
// Verbose logging (can be disabled for performance)
#ifndef ENABLE_VERBOSE_LOG
#define ENABLE_VERBOSE_LOG 0
#endif

// Debug logging
#ifndef ENABLE_DEBUG_LOG
#define ENABLE_DEBUG_LOG 1
#endif

// Info logging
#ifndef ENABLE_INFO_LOG
#define ENABLE_INFO_LOG 1
#endif

// Warning logging
#ifndef ENABLE_WARN_LOG
#define ENABLE_WARN_LOG 1
#endif

// Error logging (always enabled if logging is enabled)
#define ENABLE_ERROR_LOG 1

// Log macro implementations
#if ENABLE_VERBOSE_LOG
#define log_print_verbose(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#else
#define log_print_verbose(...) ((void)0)
#endif

#if ENABLE_DEBUG_LOG
#define log_print_debug(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define log_print_debug(...) ((void)0)
#endif

#if ENABLE_INFO_LOG
#define log_print_info(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#else
#define log_print_info(...) ((void)0)
#endif

#if ENABLE_WARN_LOG
#define log_print_warn(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#else
#define log_print_warn(...) ((void)0)
#endif

#if ENABLE_ERROR_LOG
#define log_print_error(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#else
#define log_print_error(...) ((void)0)
#endif

#else // ENABLE_LOGGING == 0
// All logging disabled
#define log_print_verbose(...) ((void)0)
#define log_print_debug(...) ((void)0)
#define log_print_info(...) ((void)0)
#define log_print_warn(...) ((void)0)
#define log_print_error(...) ((void)0)
#endif // ENABLE_LOGGING

// Android-style log macros (compatible with Android logging conventions)
#define ALOGV(...) log_print_verbose(__VA_ARGS__)
#define ALOGD(...) log_print_debug(__VA_ARGS__)
#define ALOGI(...) log_print_info(__VA_ARGS__)
#define ALOGW(...) log_print_warn(__VA_ARGS__)
#define ALOGE(...) log_print_error(__VA_ARGS__)

#endif // PRISON_LOGGER_H
