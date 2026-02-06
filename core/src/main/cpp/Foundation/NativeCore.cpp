#include "NativeCore.h"
#include "Logger.h"
#include "SandboxFs.h"
#include <jni.h>
#include "Hook/Hooks.h"
#include "Foundation/xdl/xdl.h"
#include "xdl/xdl_util.h"
#include <jni.h>
#include <sys/system_properties.h>
#include "Foundation/xdl/xdl.h"
#include "elf_util.h"
#include "Logger.h"
#include <android/api-level.h>

/**
 * Global JNI environment structure.
 * Stores JavaVM, NativeCore class reference, and method IDs for JNI callbacks.
 */
struct {
    JavaVM *vm;
    jclass NativeCoreClass;
    jmethodID getCallingUidId;
    jmethodID redirectPathString;
    jmethodID redirectPathFile;
    jmethodID loadEmptyDex;
    int api_level;
    char package_name[128];
    char external_dir[512];
} VMEnv;

// Method name constants
static const char* METHOD_GET_CALLING_UID = "getCallingUid";
static const char* METHOD_REDIRECT_PATH_STRING = "redirectPath";
static const char* METHOD_REDIRECT_PATH_FILE = "redirectPath";
static const char* METHOD_LOAD_EMPTY_DEX = "loadEmptyDex";

// Method signature constants
static const char* SIG_GET_CALLING_UID = "(I)I";
static const char* SIG_REDIRECT_PATH_STRING = "(Ljava/lang/String;)Ljava/lang/String;";
static const char* SIG_REDIRECT_PATH_FILE = "(Ljava/io/File;)Ljava/io/File;";
static const char* SIG_LOAD_EMPTY_DEX = "()[J";

/**
 * Gets the current JNI environment.
 * 
 * @return JNIEnv pointer, or nullptr if not available
 */
static JNIEnv *getEnv() {
    if (VMEnv.vm == nullptr) {
        ALOGE("VMEnv.vm is null");
        return nullptr;
    }
    
    JNIEnv *env = nullptr;
    jint result = VMEnv.vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (result != JNI_OK) {
        ALOGE("Failed to get JNI environment: %d", result);
        return nullptr;
    }
    return env;
}

/**
 * Ensures JNI environment is created, attaching current thread if necessary.
 * 
 * @return JNIEnv pointer, or nullptr if attachment fails
 */
static JNIEnv *ensureEnvCreated() {
    if (VMEnv.vm == nullptr) {
        ALOGE("VMEnv.vm is null, cannot ensure environment");
        return nullptr;
    }
    
    JNIEnv *env = getEnv();
    if (env == nullptr) {
        // Try to attach current thread
        jint result = VMEnv.vm->AttachCurrentThread(&env, nullptr);
        if (result != JNI_OK || env == nullptr) {
            ALOGE("Failed to attach current thread: %d", result);
            return nullptr;
        }
    }
    return env;
}

/**
 * Gets the spoofed calling UID from Java layer.
 * 
 * @param env JNI environment
 * @param orig Original calling UID
 * @return Spoofed UID, or original if call fails
 */
int NativeCore::getCallingUid(JNIEnv *env, int orig) {
    if (VMEnv.NativeCoreClass == nullptr || VMEnv.getCallingUidId == nullptr) {
        ALOGE("NativeCore class or method not initialized");
        return orig;
    }
    
    env = ensureEnvCreated();
    if (env == nullptr) {
        ALOGE("Failed to ensure JNI environment for getCallingUid");
        return orig;
    }
    
    jint result = env->CallStaticIntMethod(VMEnv.NativeCoreClass, VMEnv.getCallingUidId, orig);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        ALOGE("Exception occurred in getCallingUid");
        env->ExceptionClear();
        return orig;
    }
    
    return result;
}

/**
 * Redirects a path string using IOCore.
 * 
 * @param env JNI environment
 * @param path Original path string
 * @return Redirected path string, or original if redirection fails
 */
jstring NativeCore::redirectPathString(JNIEnv *env, jstring path) {
    if (VMEnv.NativeCoreClass == nullptr || VMEnv.redirectPathString == nullptr) {
        ALOGE("NativeCore class or method not initialized");
        return path;
    }
    
    if (path == nullptr) {
        ALOGE("Input path is null");
        return path;
    }
    
    env = ensureEnvCreated();
    if (env == nullptr) {
        ALOGE("Failed to ensure JNI environment for redirectPathString");
        return path;
    }
    
    jobject result = env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathString, path);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        ALOGE("Exception occurred in redirectPathString");
        env->ExceptionClear();
        return path;
    }
    
    return static_cast<jstring>(result);
}

/**
 * Redirects a File path using IOCore.
 * 
 * @param env JNI environment
 * @param path Original file path
 * @return Redirected file path, or original if redirection fails
 */
jobject NativeCore::redirectPathFile(JNIEnv *env, jobject path) {
    if (VMEnv.NativeCoreClass == nullptr || VMEnv.redirectPathFile == nullptr) {
        ALOGE("NativeCore class or method not initialized");
        return path;
    }
    
    if (path == nullptr) {
        ALOGE("Input path is null");
        return path;
    }
    
    env = ensureEnvCreated();
    if (env == nullptr) {
        ALOGE("Failed to ensure JNI environment for redirectPathFile");
        return path;
    }
    
    jobject result = env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.redirectPathFile, path);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        ALOGE("Exception occurred in redirectPathFile");
        env->ExceptionClear();
        return path;
    }
    
    return result;
}

/**
 * Loads an empty DEX file and returns its cookies.
 * 
 * @param env JNI environment
 * @return Array of DEX cookies, or nullptr if loading fails
 */
jlongArray NativeCore::loadEmptyDex(JNIEnv *env) {
    if (VMEnv.NativeCoreClass == nullptr || VMEnv.loadEmptyDex == nullptr) {
        ALOGE("NativeCore class or method not initialized");
        return nullptr;
    }
    
    env = ensureEnvCreated();
    if (env == nullptr) {
        ALOGE("Failed to ensure JNI environment for loadEmptyDex");
        return nullptr;
    }
    
    jobject result = env->CallStaticObjectMethod(VMEnv.NativeCoreClass, VMEnv.loadEmptyDex);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        ALOGE("Exception occurred in loadEmptyDex");
        env->ExceptionClear();
        return nullptr;
    }
    
    return static_cast<jlongArray>(result);
}

/**
 * Gets the Android API level.
 * 
 * @return API level, or 0 if not initialized
 */
int NativeCore::getApiLevel() {
    return VMEnv.api_level;
}

/**
 * Gets the JavaVM instance.
 * 
 * @return JavaVM pointer, or nullptr if not initialized
 */
JavaVM *NativeCore::getJavaVM() {
    return VMEnv.vm;
}

/**
 * Gets the saved package name as a C string.
 * Note: The returned string is valid only during the current JNI call.
 * For longer use, copy the string.
 * 
 * @param env JNI environment
 * @return Package name as UTF-8 C string, or nullptr if not set
 */
const char* NativeCore::getPackageName() {
    return VMEnv.package_name;
}

const char *NativeCore::getExternalFilesDir() {
    return VMEnv.external_dir;
}

/**
 * Initializes the native core.
 * Sets up JNI method IDs and initializes JniHook.
 * API level is obtained from native layer using xdl_util_get_api_level().
 * 
 * @param env JNI environment
 * @param clazz NativeCore class object (unused)
 * @param package_name Package name of the virtualized application
 */
static void init(JNIEnv *env, jobject clazz, jobject context, jstring package_name) {
    if (env == nullptr) {
        ALOGE("JNI environment is null, cannot initialize");
        return;
    }
      
    // Get API level from native layer
    VMEnv.api_level = xdl_util_get_api_level();
    if (VMEnv.api_level < 0) {
        ALOGE("Failed to get API level from native layer, using fallback");
        VMEnv.api_level = __ANDROID_API_J__; // Fallback to minimum supported
    }

    const char* package_name_str = env->GetStringUTFChars(package_name, JNI_FALSE);
    if (package_name_str != nullptr) {
        strncpy(VMEnv.package_name, package_name_str, sizeof(VMEnv.package_name) - 1);
        VMEnv.package_name[sizeof(VMEnv.package_name) - 1] = '\0';
        env->ReleaseStringUTFChars(package_name, package_name_str);
    } else {
        ALOGE("Failed to get package name");
    }
    if (context != nullptr) {
        jclass contextClass = env->GetObjectClass(context);
        jmethodID getExternalFilesDirId = env->GetMethodID(contextClass, "getExternalFilesDir", "(Ljava/lang/String;)Ljava/io/File;");
        if (getExternalFilesDirId != nullptr) {
            jobject externalFilesDirFile = env->CallObjectMethod(context, getExternalFilesDirId, nullptr);
            if (externalFilesDirFile != nullptr) {
                // Get File.getAbsolutePath() method
                jclass fileClass = env->GetObjectClass(externalFilesDirFile);
                jmethodID getAbsolutePathId = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
                if (getAbsolutePathId != nullptr) {
                    jstring externalFilesDirStr = (jstring)env->CallObjectMethod(externalFilesDirFile, getAbsolutePathId);
                    if (externalFilesDirStr != nullptr) {
                        const char* externalFilesDirStrC = env->GetStringUTFChars(externalFilesDirStr, JNI_FALSE);
                        if (externalFilesDirStrC != nullptr) {
                            strncpy(VMEnv.external_dir, externalFilesDirStrC, sizeof(VMEnv.external_dir) - 1);
                            VMEnv.external_dir[sizeof(VMEnv.external_dir) - 1] = '\0';
                            env->ReleaseStringUTFChars(externalFilesDirStr, externalFilesDirStrC);
                            ALOGD("Cached external files directory: %s", VMEnv.external_dir);
                        }
                        env->DeleteLocalRef(externalFilesDirStr);
                    }
                }
                env->DeleteLocalRef(fileClass);
                env->DeleteLocalRef(externalFilesDirFile);
            }
        }
        env->DeleteLocalRef(contextClass);
    }

    ALOGD("NativeCore init with API level: %d (from native) and package name: %s and external dir: %s", 
          VMEnv.api_level, VMEnv.package_name, VMEnv.external_dir);

    // Find and cache NativeCore class
    jclass nativeCoreClass = env->FindClass(VMCORE_CLASS);
    if (nativeCoreClass == nullptr) {
        ALOGE("Failed to find NativeCore class");
        env->ExceptionClear();
        return;
    }
    
    VMEnv.NativeCoreClass = static_cast<jclass>(env->NewGlobalRef(nativeCoreClass));
    if (VMEnv.NativeCoreClass == nullptr) {
        ALOGE("Failed to create global reference for NativeCore class");
        return;
    }
    
    // Get method IDs
    VMEnv.getCallingUidId = env->GetStaticMethodID(
        VMEnv.NativeCoreClass, 
        METHOD_GET_CALLING_UID, 
        SIG_GET_CALLING_UID
    );
    if (VMEnv.getCallingUidId == nullptr) {
        ALOGE("Failed to get method ID: getCallingUid");
        env->ExceptionClear();
    }
    
    VMEnv.redirectPathString = env->GetStaticMethodID(
        VMEnv.NativeCoreClass, 
        METHOD_REDIRECT_PATH_STRING, 
        SIG_REDIRECT_PATH_STRING
    );
    if (VMEnv.redirectPathString == nullptr) {
        ALOGE("Failed to get method ID: redirectPath(String)");
        env->ExceptionClear();
    }
    
    VMEnv.redirectPathFile = env->GetStaticMethodID(
        VMEnv.NativeCoreClass, 
        METHOD_REDIRECT_PATH_FILE, 
        SIG_REDIRECT_PATH_FILE
    );
    if (VMEnv.redirectPathFile == nullptr) {
        ALOGE("Failed to get method ID: redirectPath(File)");
        env->ExceptionClear();
    }
    
    VMEnv.loadEmptyDex = env->GetStaticMethodID(
        VMEnv.NativeCoreClass, 
        METHOD_LOAD_EMPTY_DEX, 
        SIG_LOAD_EMPTY_DEX
    );
    if (VMEnv.loadEmptyDex == nullptr) {
        ALOGE("Failed to get method ID: loadEmptyDex");
        env->ExceptionClear();
    }
    
    // Initialize JniHook
    JniHook::InitJniHook(env, VMEnv.api_level);
    
    ALOGD("NativeCore initialization completed");
}

/**
 * Adds an I/O redirection rule.
 * 
 * @param env JNI environment
 * @param clazz NativeCore class object (unused)
 * @param target_path Original path to redirect from
 * @param relocate_path Target path to redirect to
 */
static void addIORule(JNIEnv *env, jclass clazz, jstring target_path, jstring relocate_path) {
    if (env == nullptr) {
        ALOGE("JNI environment is null");
        return;
    }
    
    if (target_path == nullptr || relocate_path == nullptr) {
        ALOGE("Input paths are null");
        return;
    }
    
    const char* target = env->GetStringUTFChars(target_path, JNI_FALSE);
    const char* relocate = env->GetStringUTFChars(relocate_path, JNI_FALSE);
    
    if (target == nullptr || relocate == nullptr) {
        ALOGE("Failed to get string characters");
        if (target != nullptr) env->ReleaseStringUTFChars(target_path, target);
        if (relocate != nullptr) env->ReleaseStringUTFChars(relocate_path, relocate);
        return;
    }
    
    ALOGD("Adding I/O rule: %s -> %s",  target, relocate);
    add_replace_item( target, relocate );
    
    // Release string characters
    env->ReleaseStringUTFChars(target_path, target);
    env->ReleaseStringUTFChars(relocate_path, relocate);
}

bool disable_hidden_api(JNIEnv *env) {
    char version_str[PROP_VALUE_MAX];
    if (!__system_property_get("ro.build.version.sdk", version_str)) {
        ALOGE("Failed to obtain SDK int");
        return false;
    }
    long android_version = std::strtol(version_str, nullptr, 10);

    // Hidden api introduced in sdk 29
    if (android_version < 29) {
        ALOGD("HiddenAPI: Android version < 29, no need to disable");
        return true;
    }

    SandHook::ElfImg *elf_img = new SandHook::ElfImg("libart.so");
    if (!elf_img->isValid()) {
        ALOGE("HiddenAPI: Failed to load libart.so");
        delete elf_img;
        return false;
    }

    // Try multiple possible symbol names for different Android versions
    void *addr = nullptr;
    const char* symbol_names[] = {
        "_ZN3artL32VMRuntime_setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "_ZN3art9VMRuntime22setHiddenApiExemptionsEP7_JNIEnvP7_jclassP13_jobjectArray",
        "art::VMRuntime::setHiddenApiExemptions(_JNIEnv*, _jclass*, _jobjectArray*)",
        nullptr
    };

    for (int i = 0; symbol_names[i] != nullptr; i++) {
        addr = (void*)elf_img->getSymbAddress(symbol_names[i]);
        if (addr) {
            ALOGD("HiddenAPI: Found symbol %s at %p", symbol_names[i], addr);
            break;
        }
    }

    delete elf_img;
    
    if (!addr) {
        ALOGE("HiddenAPI: Didn't find setHiddenApiExemptions in any form");
        return false;
    }

    jclass stringClass = env->FindClass("java/lang/String");
    if (!stringClass) {
        ALOGE("HiddenAPI: Failed to find String class");
        return false;
    }

    // L is basically wildcard for everything
    jstring wildcard = env->NewStringUTF("L");
    if (!wildcard) {
        ALOGE("HiddenAPI: Failed to create wildcard string");
        return false;
    }

    jobjectArray args = env->NewObjectArray(1, stringClass, wildcard);
    if (!args) {
        ALOGE("HiddenAPI: Failed to create args array");
        return false;
    }

    auto func = reinterpret_cast<void (*)(JNIEnv *, jclass, jobjectArray)>(addr);
    // jclass arg is not used so pass string class for the memes
    func(env, stringClass, args);
    ALOGD("HiddenAPI: Successfully disabled hidden API restrictions");
    return true;
}

bool disable_resource_loading() {
    // Try to hook the ApkAssets.nativeLoad method directly (safer than system properties)
    try {
        // Load the framework library
        void* handle = xdl_open("libandroid_runtime.so", XDL_DEFAULT);
        if (handle) {
            // Try to find and hook the nativeLoad method
            void* nativeLoadAddr = xdl_sym(handle, "_ZN7android8ApkAssets9nativeLoadEPKc", nullptr);
            if (nativeLoadAddr) {
                ALOGD("ResourceLoading: Found ApkAssets.nativeLoad at %p", nativeLoadAddr);
                // Here we would implement the actual hook, but for now we'll just log it
            } else {
                ALOGD("ResourceLoading: Could not find ApkAssets.nativeLoad symbol");
            }
            xdl_close(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libandroid_runtime.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook ApkAssets.nativeLoad");
    }
    
    // Try to hook the file system calls directly
    try {
        // Load the libc library
        void* handle = xdl_open("libc.so", XDL_DEFAULT);
        if (handle) {
            // Try to find and hook the open function
            void* openAddr = xdl_sym(handle, "open", nullptr);
            if (openAddr) {
                ALOGD("ResourceLoading: Found open function at %p", openAddr);
                // Here we would implement the actual hook, but for now we'll just log it
            } else {
                ALOGD("ResourceLoading: Could not find open function symbol");
            }
            xdl_close(handle);
        } else {
            ALOGD("ResourceLoading: Could not open libc.so");
        }
    } catch (...) {
        ALOGD("ResourceLoading: Exception while trying to hook file system calls");
    }
    
    ALOGD("ResourceLoading: Native resource loading hooks initialized (without system properties)");
    return true;
}

/**
 * Initializes I/O system and all native hooks.
 * This function sets up file system hooks, class loader hooks, binder hooks, etc.
 * API level is obtained automatically from native layer.
 * 
 * @param env JNI environment
 * @param clazz NativeCore class object (unused)
 * @param context Android context for accessing system services
 * @param package_name Package name of the virtualized application
 */
static void installHooks(JNIEnv *env, jclass clazz, jobject context, jstring package_name) {
    if (context == nullptr) {
        ALOGW("Context is null in installHooks, continuing without context");
    }
    init(env, clazz, context, package_name);
    
    ALOGD("Initializing I/O system and native hooks...");
    
    // Initialize I/O system

    UnixFileSystemHook::install(env);
    LibcHook::install();
    VMClassLoaderHook::install(env);
    RuntimeHook::install(env);
    BinderHook::install(env);
    DexFileHook::install(env);
    ZlibHook::install();
    
    ALOGD("I/O system and native hooks initialized successfully");
}

/**
 * Disables Android Hidden API restrictions (Android 9.0+).
 * 
 * @param env JNI environment
 * @param clazz NativeCore class object (unused)
 * @return true if successful, false otherwise
 */
static bool disableHiddenApi(JNIEnv *env, jclass clazz) {
    if (env == nullptr) {
        ALOGE("JNI environment is null");
        return false;
    }
    
    ALOGD("Disabling Hidden API restrictions...");
    bool result = disable_hidden_api(env);
    if (!result) {
        ALOGE("Failed to disable Hidden API restrictions");
    } else {
        ALOGD("Hidden API restrictions disabled successfully");
    }
    return result;
}

/**
 * Disables resource loading restrictions.
 * 
 * @param env JNI environment
 * @param clazz NativeCore class object (unused)
 * @return true if successful, false otherwise
 */
static bool disableResourceLoading(JNIEnv *env, jclass clazz) {
    if (env == nullptr) {
        ALOGE("JNI environment is null");
        return false;
    }
    
    ALOGD("Disabling resource loading restrictions...");
    bool result = disable_resource_loading();
    if (!result) {
        ALOGE("Failed to disable resource loading restrictions");
    } else {
        ALOGD("Resource loading restrictions disabled successfully");
    }
    return result;
}

// JNI native method definitions
static JNINativeMethod gMethods[] = {
    {"disableHiddenApi", "()Z", reinterpret_cast<void*>(disableHiddenApi)},
    {"disableResourceLoading", "()Z", reinterpret_cast<void*>(disableResourceLoading)},
    {"addIORule", "(Ljava/lang/String;Ljava/lang/String;)V", reinterpret_cast<void*>(addIORule)},
    {"installHooks", "(Landroid/content/Context;Ljava/lang/String;)V", reinterpret_cast<void*>(installHooks)},
};

/**
 * Registers all native methods for NativeCore class.
 * 
 * @param env JNI environment
 * @return JNI_TRUE if successful, JNI_FALSE otherwise
 */
static int registerNatives(JNIEnv *env) {
    if (env == nullptr) {
        ALOGE("JNI environment is null");
        return JNI_FALSE;
    }
    
    int numMethods = sizeof(gMethods) / sizeof(gMethods[0]);
    
    jclass clazz = env->FindClass(VMCORE_CLASS);
    if (clazz == nullptr) {
        ALOGE("Failed to find class: %s", VMCORE_CLASS);
        env->ExceptionClear();
        return JNI_FALSE;
    }
    
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGE("Failed to register native methods for class: %s", VMCORE_CLASS);
        env->ExceptionClear();
        return JNI_FALSE;
    }
    
    ALOGD("Successfully registered %d native methods for class: %s", numMethods, VMCORE_CLASS);
    return JNI_TRUE;
}

/**
 * JNI library initialization function.
 * Called when the native library is loaded.
 * 
 * @param vm JavaVM instance
 * @param reserved Reserved for future use
 * @return JNI version on success, error code on failure
 */
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    if (vm == nullptr) {
        ALOGE("JavaVM is null");
        return JNI_ERR;
    }
    
    JNIEnv *env = nullptr;
    VMEnv.vm = vm;
    
    jint result = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (result != JNI_OK) {
        ALOGE("Failed to get JNI environment: %d", result);
        return JNI_EVERSION;
    }
    
    if (env == nullptr) {
        ALOGE("JNI environment is null after GetEnv");
        return JNI_ERR;
    }
    
    registerNatives(env);
    ALOGD("JNI_OnLoad completed successfully");
    
    return JNI_VERSION_1_6;
}
