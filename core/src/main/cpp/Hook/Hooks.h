#ifndef PRISON_HOOKS_H
#define PRISON_HOOKS_H

#include <jni.h>
#include "Foundation/ArtMethod.h"
#include "xdl/xdl.h"
#include "Dobby/dobby.h"

#define HOOK_SYMBOL(handle, func) hook_function(handle, #func, (void*) new_##func, (void**) &orig_##func)
#define HOOK_DEF(ret, func, ...) \
  ret (*orig_##func)(__VA_ARGS__); \
  ret new_##func(__VA_ARGS__)


#define HOOK_JNI(ret, func, ...) \
ret (*orig_##func)(__VA_ARGS__); \
ret new_##func(__VA_ARGS__)

static inline void hook_function(void *handle, const char *symbol, void *new_func, void **old_func) {
    void *sym = xdl_sym(handle, symbol, nullptr);
    if (sym == nullptr) {
        return;
    }
    DobbyHook(sym, new_func, old_func);
}

class JniHook {
public:
    static void InitJniHook(JNIEnv *env, int api_level);
    static void HookJniFun(JNIEnv *env, const char *class_name, const char *method_name, const char *sign, void * new_fun, void ** orig_fun,
                            bool is_static);
    static void HookJniFun(JNIEnv *env, jobject java_method, void *new_fun, void **orig_fun, bool is_static);
};


class BinderHook {
public:
    static void install(JNIEnv *env);
};

class ZlibHook {
    public:
        static void install();
};

class VMClassLoaderHook {
    public:
        static void install(JNIEnv *env);
};

class LibcHook {
    public:
        static void install();
};


class UnixFileSystemHook {
    public:
        static void install(JNIEnv *env);
};

class RuntimeHook {
    public:
        static void install(JNIEnv *env);
    };
    
  
class DexFileHook{
    public:
        static void install(JNIEnv *env);
        static void setFileReadonly(const char* filePath);
};

#endif //PRISON_BASEHOOK_H