/*
 * Copyright (c) 2024 Prison Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * GitHub: https://github.com/Prison3/prison
 * 996.ICU: https://996.icu | https://github.com/996icu/996.ICU
 */

// Created for blocking problematic resource files via libc hooks


#include <sys/stat.h>
#include <fcntl.h>
#include <stdarg.h>
#include <cstring>
#include <errno.h>
#include <limits.h>
#include <syscall.h>
#include <cstdio>
#include "Dobby/dobby.h"
#include "Foundation/SandboxFs.h"
#include "Foundation/Logger.h"
#include "Hooks.h"
#include "Foundation/xdl/xdl.h"
#include <unistd.h>



// int openat(int fd, const char *pathname, int flags, int mode);
HOOK_DEF(int, openat, int fd, const char *pathname, int flags, int mode) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path) {
        if ((flags & O_ACCMODE) == O_WRONLY) {
            flags &= ~O_ACCMODE;
            flags |= O_RDWR;
        }
        int ret = syscall(__NR_openat, fd, relocated_path, flags, mode);
        return ret;
    }
    errno = EACCES;
    return -1;
}


HOOK_DEF(int, faccessat, int dirfd, const char *pathname, int mode, int flags) {
    char temp[PATH_MAX];
    //ALOGD("LibcHook faccessat pathname %s dirfd %d mode %d flags %d" , pathname, dirfd, mode, flags);
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path && !(mode & W_OK && isReadOnly(relocated_path))) {
        //ALOGD("LibcHook faccessat pathname %s -> %s dirfd %d mode %d flags %d" , pathname, relocated_path, dirfd, mode, flags);
        return syscall(__NR_faccessat, dirfd, relocated_path, mode, flags);
    }
    errno = EACCES;
    return -1;
}

// int open(const char *pathname, int flags, ...);
HOOK_DEF(int, open, const char *pathname, int flags, ...) {
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path) {
        mode_t mode = 0;
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode = va_arg(args, mode_t);
            va_end(args);
        }
        
        if ((flags & O_ACCMODE) == O_WRONLY) {
            flags &= ~O_ACCMODE;
            flags |= O_RDWR;
        }
        int ret = syscall(__NR_openat, AT_FDCWD, relocated_path, flags, mode);
        return ret;
    }
    errno = EACCES;
    return -1;
}

// FILE* fopen(const char *pathname, const char *mode);
HOOK_DEF(FILE*, fopen, const char *pathname, const char *mode) {
    //ALOGD("LibcHook fopen CALLED pathname %s mode %s", pathname ? pathname : "(null)", mode ? mode : "(null)");
    char temp[PATH_MAX];
    const char *relocated_path = relocate_path(pathname, temp, sizeof(temp));
    if (relocated_path) {
        FILE* ret = orig_fopen(relocated_path, mode);
        return ret;
    }
    errno = EACCES;
    return nullptr;
}

void LibcHook::install() {
    ALOGD("LibcHook: Initializing file system hooks");
    
    // Load libc.so
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    if (handle) {
        // HOOK_SYMBOL(handle, fchownat);
        // HOOK_SYMBOL(handle, renameat);
        // HOOK_SYMBOL(handle, mkdirat);
        // HOOK_SYMBOL(handle, mknodat);
        // HOOK_SYMBOL(handle, truncate);
        // HOOK_SYMBOL(handle, linkat);
        // HOOK_SYMBOL(handle, readlinkat);
        // HOOK_SYMBOL(handle, unlinkat);
        // HOOK_SYMBOL(handle, symlinkat);
        // HOOK_SYMBOL(handle, utimensat);
        // HOOK_SYMBOL(handle, chdir);
        // HOOK_SYMBOL(handle, execve);
        // HOOK_SYMBOL(handle, statfs64);
        // HOOK_SYMBOL(handle, kill);
        // HOOK_SYMBOL(handle, vfork);
         HOOK_SYMBOL(handle, faccessat);
         HOOK_SYMBOL(handle, openat);
         HOOK_SYMBOL(handle, open);
         HOOK_SYMBOL(handle, fopen);
        // HOOK_SYMBOL(handle, fchmodat);
        // HOOK_SYMBOL(handle, fstatat64);
        // HOOK_SYMBOL(handle, statfs);
        // HOOK_SYMBOL(handle, __statfs);
        // HOOK_SYMBOL(handle, __statfs64);
        // HOOK_SYMBOL(handle, getcwd);
        // HOOK_SYMBOL(handle, stat);
        // HOOK_SYMBOL(handle, lstat);
        // HOOK_SYMBOL(handle, fstatat);
        // HOOK_SYMBOL(handle, close);
        // HOOK_SYMBOL(handle, read);
        // HOOK_SYMBOL(handle, write);
        // HOOK_SYMBOL(handle, mmap);
        // HOOK_SYMBOL(handle, munmap);
        // HOOK_SYMBOL(handle, pread64);
        // HOOK_SYMBOL(handle, pwrite64);
        // HOOK_SYMBOL(handle, fstat);
        // HOOK_SYMBOL(handle, __llseek);
        // HOOK_SYMBOL(handle, lseek);
        // HOOK_SYMBOL(handle, ftruncate64);
        // HOOK_SYMBOL(handle, sendfile);
        // HOOK_SYMBOL(handle, sendfile64);
        // HOOK_SYMBOL(handle, dup);
        // HOOK_SYMBOL(handle, dup3);
        // HOOK_SYMBOL(handle, fcntl);
        // HOOK_SYMBOL(handle, getaddrinfo);
        // HOOK_SYMBOL(handle, sendto);
        // HOOK_SYMBOL(handle, msync);
    }
    
    xdl_close(handle);
}

