
#ifndef VIRTUALM_VMCORE_H
#define VIRTUALM_VMCORE_H

#include <jni.h>

#define VMCORE_CLASS "com/android/prison/core/NativeCore"

class NativeCore {
public:
    static JavaVM *getJavaVM();
    static int getApiLevel();
    static int getCallingUid(JNIEnv *env, int orig);
    static jstring redirectPathString(JNIEnv *env, jstring path);
    static jobject redirectPathFile(JNIEnv *env, jobject path);
    static jlongArray loadEmptyDex(JNIEnv *env);
    static const char* getPackageName();
    static const char* getAppExternDir();

    static const char *getExternalFilesDir();
};


#endif //VIRTUALM_VMCORE_H
