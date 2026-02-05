#ifndef PRISON_HOOKS_H
#define PRISON_HOOKS_H

#include <jni.h>

#define HOOK_SYMBOL(handle, func) hook_function(handle, #func, (void*) new_##func, (void**) &orig_##func)
//#define HOOK_SYMBOL2(handle, func, new_func, old_func) hook_function(handle, #func, (void*) new_func, (void**)old_func)
#define HOOK_DEF(ret, func, ...) \
  ret (*orig_##func)(__VA_ARGS__); \
  ret new_##func(__VA_ARGS__)



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
        static void hideXposed();
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