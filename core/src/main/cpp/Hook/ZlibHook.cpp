// Hook for libz deflate function

#include "Hooks.h"
#include "Foundation/NativeCore.h"
#include "Foundation/Logger.h"
#include "Foundation/xdl/xdl.h"
#include "Dobby/dobby.h"
#include <zlib.h>
#include <cstring>
#include <cstdio>
#include <ctime>
#include <unistd.h>
#include <sys/stat.h>

// Original function pointer
static int (*orig_deflate)(z_streamp strm, int flush) = nullptr;

// Helper function to check if data contains 0x98 byte or "x98" string
static __always_inline char* strnstr(const char* s1, const char* s2, size_t len)
{
	size_t l2;

	l2 = strlen(s2);
	if (!l2)
		return (char *)s1;
	while (len >= l2) {
		len--;
		if (!memcmp(s1, s2, l2))
			return (char *)s1;
		s1++;
	}
	return NULL;
}


// Save data to file
static void save_data_to_file(const Bytef* data, uInt len) {
    // Generate filename with timestamp
    char filename[256];
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    const char* external_dir = NativeCore::getExternalFilesDir();
    if (!external_dir || external_dir[0] == '\0') {
        ALOGE("ZlibHook: Failed to get external files directory, cannot save file");
        return;
    }
    snprintf(filename, sizeof(filename), "%s/ddd%ld-%ld", external_dir, ts.tv_sec, ts.tv_nsec);
    // Write data to file
    FILE* fp = fopen(filename, "wb");
    if (fp) {
        fwrite(data, 1, len, fp);
        fclose(fp);
        ALOGD("ZlibHook: Saved %u bytes to %s", len, filename);
    } else {
        ALOGE("ZlibHook: Failed to open file %s for writing", filename);
    }
}

int new_deflate(z_streamp strm, int flush) {
    // 检查压缩前的输入数据
    if (strm && strm->next_in && strm->avail_in > 0) {
        if (strnstr((char *)strm->next_in, "x98", strm->avail_in) &&  strcmp(NativeCore::getPackageName() ,"com.xingin.xhs") == 0) {
            save_data_to_file(strm->next_in, strm->avail_in);
        }
    }
    // 调用原始函数
    int result = orig_deflate(strm, flush);
    return result;
}

void ZlibHook::install() {
    void* handle = xdl_open("libz.so", XDL_DEFAULT);
    if (!handle) {
        ALOGE("ZlibHook: Failed to open libz.so");
        return;
    }
    
    // Hook deflate function
    void* deflate_addr = xdl_sym(handle, "deflate", nullptr);
    if (!deflate_addr) {
        ALOGE("ZlibHook: Failed to find deflate function");
        xdl_close(handle);
        return;
    }

    if (DobbyHook(deflate_addr, (void*)new_deflate, (void**)&orig_deflate)) {
        ALOGE("ZlibHook: Failed to hook deflate function");
    } 
    xdl_close(handle);
    return;
}
