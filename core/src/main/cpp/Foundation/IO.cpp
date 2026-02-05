
#include "IO.h"
#include "Log.h"

jmethodID getAbsolutePathMethodId;

list<IO::RelocateInfo> relocate_rule;
char *replace(const char *str, const char *src, const char *dst) {
    const char *pos = str;
    int count = 0;
    while ((pos = strstr(pos, src))) {
        count++;
        pos += strlen(src);
    }

    size_t result_len = strlen(str) + (strlen(dst) - strlen(src)) * count + 1;
    char *result = (char *) malloc(result_len);
    memset(result, 0, strlen(result));

    const char *left = str;
    const char *right = nullptr;

    while ((right = strstr(left, src))) {
        strncat(result, left, right - left);
        strcat(result, dst);
        right += strlen(src);
        left = right;
    }
    strcat(result, left);
    return result;
}

const char *IO::redirect_path(const char *__path) {
    // Block ALL resource-cache related paths that cause issues
    if (strstr(__path, "resource-cache")) {
        ALOGD("Blocking resource-cache path: %s", __path);
        return "/dev/null";
    }
    
    // Block any path containing @idmap
    if (strstr(__path, "@idmap")) {
        ALOGD("Blocking idmap path: %s", __path);
        return "/dev/null";
    }
    
    // Block any path containing systemui with problematic extensions
    if (strstr(__path, "systemui") && (strstr(__path, ".frro") || strstr(__path, "-accent-") || strstr(__path, "-dynamic-") || strstr(__path, "-neutral-"))) {
        ALOGD("Blocking systemui problematic path: %s", __path);
        return "/dev/null";
    }
    
    // Block any path containing the specific problematic patterns
    if (strstr(__path, "data@resource-cache@")) {
        ALOGD("Blocking data@resource-cache@ pattern: %s", __path);
        return "/dev/null";
    }
    
    // Block any path containing .frro files
    if (strstr(__path, ".frro")) {
        ALOGD("Blocking .frro file: %s", __path);
        return "/dev/null";
    }
    
    // Block any path containing systemui
    if (strstr(__path, "systemui")) {
        ALOGD("Blocking systemui path: %s", __path);
        return "/dev/null";
    }

    list<IO::RelocateInfo>::iterator iterator;
    for (iterator = relocate_rule.begin(); iterator != relocate_rule.end(); ++iterator) {
        IO::RelocateInfo info = *iterator;
        if (strstr(__path, info.targetPath) && !strstr(__path, "/prison/")) {
            char *ret = replace(__path, info.targetPath, info.relocatePath);
            // ALOGD("redirectPath %s  => %s", __path, ret);
            return ret;
        }
    }
    return __path;
}

jstring IO::redirect_path(JNIEnv *env, jstring path) {
//    const char * pathC = env->GetStringUTFChars(path, JNI_FALSE);
//    const char *redirect = redirect_path(pathC);
//    env->ReleaseStringUTFChars(path, pathC);
//    return env->NewStringUTF(redirect);
    return NativeCore::redirectPathString(env, path);
}

jobject IO::redirect_path(JNIEnv *env, jobject path) {
//    auto pathStr =
//            reinterpret_cast<jstring>(env->CallObjectMethod(path, getAbsolutePathMethodId));
//    jstring redirect = redirect_path(env, pathStr);
//    jobject file = env->NewObject(fileClazz, fileNew, redirect);
//    env->DeleteLocalRef(pathStr);
//    env->DeleteLocalRef(redirect);
    return NativeCore::redirectPathFile(env, path);
}

void IO::add_rule(const char *targetPath, const char *relocatePath) {
    IO::RelocateInfo info{};
    info.targetPath = targetPath;
    info.relocatePath = relocatePath;
    relocate_rule.push_back(info);
}

const char *IO::relocate_path(const char *__path, char *buf, size_t buf_size) {
    const char *redirected = redirect_path(__path);
    if (redirected == __path) {
        // No redirection, copy original path
        strncpy(buf, __path, buf_size - 1);
        buf[buf_size - 1] = '\0';
        return buf;
    } else {
        // Path was redirected, copy redirected path
        strncpy(buf, redirected, buf_size - 1);
        buf[buf_size - 1] = '\0';
        return buf;
    }
}

void IO::init(JNIEnv *env) {
    jclass tmpFile = env->FindClass("java/io/File");
    getAbsolutePathMethodId = env->GetMethodID(tmpFile, "getAbsolutePath", "()Ljava/lang/String;");
}
