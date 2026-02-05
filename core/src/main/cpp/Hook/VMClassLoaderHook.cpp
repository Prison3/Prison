

#include <cstring>
#include "Hooks.h"


HOOK_JNI(jobject, findLoadedClass, JNIEnv *env, jobject obj, jobject class_loader, jstring name) {
    const char * nameC = env->GetStringUTFChars(name, JNI_FALSE);
    if (strstr(nameC, "de/robv/android/xposed/") ||
        strstr(nameC, "me/weishu/epic") ||
        strstr(nameC, "me/weishu/exposed") ||
        strstr(nameC, "de.robv.android") ||
        strstr(nameC, "me.weishu.epic") ||
        strstr(nameC, "me.weishu.exposed")) {
        env->ReleaseStringUTFChars(name, nameC);
        return nullptr;
    }
    jobject result = orig_findLoadedClass(env, obj, class_loader, name);
    env->ReleaseStringUTFChars(name, nameC);
    return result;
}

void VMClassLoaderHook::install(JNIEnv *env) {
    const char *className = "java/lang/VMClassLoader";
    JniHook::HookJniFun(env, className, "findLoadedClass", "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;",
                        (void *) new_findLoadedClass,
                        (void **) (&orig_findLoadedClass), true);
}

