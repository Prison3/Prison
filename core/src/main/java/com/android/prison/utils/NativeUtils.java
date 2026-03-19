package com.android.prison.utils;

import android.os.Build;
import com.android.prison.utils.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class NativeUtils {
    public static final String TAG = NativeUtils.class.getSimpleName();

    public static void copyNativeLib(File apk, File nativeLibDir) throws Exception {
        long startTime = System.currentTimeMillis();
        if (!nativeLibDir.exists()) {
            nativeLibDir.mkdirs();
        }
        try (ZipFile zipfile = new ZipFile(apk.getAbsolutePath())) {
            if (findAndCopyNativeLib(zipfile, Build.CPU_ABI, nativeLibDir)) {
                return;
            }

            findAndCopyNativeLib(zipfile, "armeabi", nativeLibDir);
        } finally {
            Logger.d(TAG, "Done! +" + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    private static boolean findAndCopyNativeLib(ZipFile zipfile, String cpuArch, File nativeLibDir) throws Exception {
        Logger.d(TAG, "Try to copy plugin's cup arch: " + cpuArch);
        boolean findLib = false;
        boolean findSo = false;
        byte buffer[] = null;
        String libPrefix = "lib/" + cpuArch + "/";
        ZipEntry entry;
        Enumeration e = zipfile.entries();

        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();
            if (!findLib && !entryName.startsWith("lib/")) {
                continue;
            }
            findLib = true;
            if (!entryName.endsWith(".so") || !entryName.startsWith(libPrefix)) {
                continue;
            }

            if (buffer == null) {
                findSo = true;
                Logger.d(TAG, "Found plugin's cup arch dir: " + cpuArch);
                buffer = new byte[8192];
            }

            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            Logger.d(TAG, "verify so " + libName);
//            File abiDir = new File(nativeLibDir, cpuArch);
//            if (!abiDir.exists()) {
//                abiDir.mkdirs();
//            }

            File libFile = new File(nativeLibDir, libName);
            if (libFile.exists() && libFile.length() == entry.getSize()) {
                Logger.d(TAG, libName + " skip copy");
                continue;
            }
            FileOutputStream fos = new FileOutputStream(libFile);
            Logger.d(TAG, "copy so " + entry.getName() + " of " + cpuArch);
            copySo(buffer, zipfile.getInputStream(entry), fos);
        }

        if (!findLib) {
            Logger.d(TAG, "Fast skip all!");
            return true;
        }

        return findSo;
    }

    private static void copySo(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        int count;

        while ((count = bufferedInput.read(buffer)) > 0) {
            bufferedOutput.write(buffer, 0, count);
        }
        bufferedOutput.flush();
        bufferedOutput.close();
        output.close();
        bufferedInput.close();
        input.close();
    }
}
