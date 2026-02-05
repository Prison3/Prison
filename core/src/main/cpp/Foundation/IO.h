
#ifndef VIRTUALM_IO_H
#define VIRTUALM_IO_H

#include <jni.h>

#include <list>
#include <iostream>
#include "NativeCore.h"

using namespace std;

class IO {
public:
    static void init(JNIEnv *env);

    struct RelocateInfo {
        const char *targetPath;
        const char *relocatePath;
    };

    static void add_rule(const char *targetPath, const char *relocatePath);

    static jstring redirect_path(JNIEnv *env, jstring path);

    static jobject redirect_path(JNIEnv *env, jobject path);

    static const char *redirect_path(const char *__path);
    
    static const char *relocate_path(const char *__path, char *buf, size_t buf_size);
};


#endif //VIRTUALM_IO_H
