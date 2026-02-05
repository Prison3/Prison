#include "Hooks.h"
#include "Foundation/SandboxFs.h"
#include "Foundation/NativeCore.h"




HOOK_JNI(jint, getCallingUid, JNIEnv *env, jobject obj) {
    int orig = orig_getCallingUid(env, obj);
    return NativeCore::getCallingUid(env, orig);
}


void BinderHook::install(JNIEnv *env) {
    const char *clazz = "android/os/Binder";
    JniHook::HookJniFun(env, clazz, "getCallingUid", "()I", (void *) new_getCallingUid,
                        (void **) (&orig_getCallingUid), true);
}