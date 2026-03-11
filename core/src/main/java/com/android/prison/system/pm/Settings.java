package com.android.prison.system.pm;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.os.Parcel;
import android.os.Process;
import android.util.ArrayMap;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.android.prison.core.PrisonCore;
import com.android.prison.base.PEnvironment;
import com.android.prison.system.ProcessManagerService;
import com.android.prison.system.user.BUserHandle;
import com.android.prison.entity.InstallOption;
import com.android.prison.utils.FileUtils;
import com.android.prison.utils.PackageParserCompat;
import com.android.prison.utils.Logger;

/*public*/ class Settings {
    public static final String TAG = Settings.class.getSimpleName();

    final ArrayMap<String, PPackageSettings> mPackages = new ArrayMap<>();
    private final Map<String, Integer> mAppIds = new HashMap<>();
    private final Map<String, SharedUserSetting> mSharedUsers = SharedUserSetting.sSharedUsers;
    private int mCurrUid = 0;

    public Settings() {
        synchronized (mPackages) {
            loadUidLP();
            SharedUserSetting.loadSharedUsers();
        }
    }

    PPackageSettings getPackageLPw(String name, PackageParser.Package aPackage, InstallOption installOption) {
        PPackageSettings pkgSettings;
        PPackageSettings origSettings = new PPackageSettings();
        origSettings.pkg = new BPackage(aPackage);
        origSettings.pkg.installOption = installOption;
        origSettings.installOption = installOption;
        origSettings.pkg.mExtras = origSettings;
        origSettings.pkg.applicationInfo = PackageManagerCompat.generateApplicationInfo(origSettings.pkg, 0, BPackageUserState.create(), 0);
        synchronized (mPackages) {
            pkgSettings = mPackages.get(name);
            long now = System.currentTimeMillis();
            if (pkgSettings != null) {
                origSettings.appId = pkgSettings.appId;
                origSettings.userState = pkgSettings.userState;
                // 保留首次安装时间，只更新最后更新时间
                origSettings.firstInstallTime = pkgSettings.firstInstallTime;
                origSettings.lastUpdateTime = now;
            } else {
                // 首次安装：两个时间都为当前时间
                origSettings.firstInstallTime = now;
                origSettings.lastUpdateTime = now;
                boolean b = registerAppIdLPw(origSettings);
                if (!b) {
                    throw new RuntimeException("registerAppIdLPw err.");
                }
            }
        }
        return origSettings;
    }

    boolean registerAppIdLPw(PPackageSettings p) {
        boolean createdNew = false;
        String sharedUserId = p.pkg.mSharedUserId;
        SharedUserSetting sharedUserSetting = null;
        if (sharedUserId != null) {
            sharedUserSetting = mSharedUsers.get(sharedUserId);
            if (sharedUserSetting == null) {
                sharedUserSetting = new SharedUserSetting(sharedUserId);
                sharedUserSetting.userId = acquireAndRegisterNewAppIdLPw(p);
                mSharedUsers.put(sharedUserId, sharedUserSetting);
            }
        }
        if (sharedUserSetting != null) {
            p.appId = sharedUserSetting.userId;
            Logger.d(TAG, p.pkg.packageName + " sharedUserId = " + sharedUserId + ", setAppId = " + p.appId);
        }
        if (p.appId == 0) {
            // Assign new user ID
            p.appId = acquireAndRegisterNewAppIdLPw(p);
        }
        if (p.appId < 0) {
            createdNew = false;
//            PackageManagerService.reportSettingsProblem(Log.WARN,
//                    "Package " + p.name + " could not be assigned a valid UID");
//            throw new PackageManagerException(INSTALL_FAILED_INSUFFICIENT_STORAGE,
//                    "Package " + p.name + " could not be assigned a valid UID");
        } else {
            createdNew = true;
        }
        saveUidLP();
        SharedUserSetting.saveSharedUsers();
        return createdNew;
    }

    private int acquireAndRegisterNewAppIdLPw(PPackageSettings obj) {
        // Let's be stupidly inefficient for now...
        Integer integer = mAppIds.get(obj.pkg.packageName);
        if (integer != null)
            return integer;

        if (mCurrUid >= Process.LAST_APPLICATION_UID) {
            return -1;
        }
        mCurrUid++;
        mAppIds.put(obj.pkg.packageName, mCurrUid);
        return Process.FIRST_APPLICATION_UID + mCurrUid;
    }

    private void saveUidLP() {
        Parcel parcel = Parcel.obtain();
        FileOutputStream fileOutputStream = null;
        AtomicFile atomicFile = new AtomicFile(PEnvironment.getUidConf());
        try {
            Set<String> pkgName = mPackages.keySet();
            for (String s : new HashSet<>(mAppIds.keySet())) {
                if (!pkgName.contains(s)) {
                    mAppIds.remove(s);
                }
            }
            parcel.writeInt(mCurrUid);
            parcel.writeMap(mAppIds);

            fileOutputStream = atomicFile.startWrite();
            FileUtils.writeParcelToOutput(parcel, fileOutputStream);
            atomicFile.finishWrite(fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            atomicFile.failWrite(fileOutputStream);
        } finally {
            parcel.recycle();
        }
    }

    private void loadUidLP() {
        Parcel parcel = Parcel.obtain();
        try {
            byte[] uidBytes = FileUtils.toByteArray(PEnvironment.getUidConf());
            if (uidBytes == null || uidBytes.length == 0) {
                // No data to load, start fresh
                return;
            }
            
            parcel.unmarshall(uidBytes, 0, uidBytes.length);
            parcel.setDataPosition(0);

            mCurrUid = parcel.readInt();
            HashMap hashMap = parcel.readHashMap(HashMap.class.getClassLoader());
            synchronized (mAppIds) {
                mAppIds.clear();
                mAppIds.putAll(hashMap);
            }
        } catch (Exception e) {
            // If loading fails, clear the corrupted data and start fresh
            try {
                // Delete the corrupted file
                PEnvironment.getUidConf().delete();
            } catch (Exception deleteException) {
                // Ignore delete errors
            }
            
            // Reset to default values
            mCurrUid = 0;
            synchronized (mAppIds) {
                mAppIds.clear();
            }
        } finally {
            parcel.recycle();
        }
    }

    public void scanPackage() {
        synchronized (mPackages) {
            File appRootDir = PEnvironment.getAppRootDir();
            FileUtils.mkdirs(appRootDir);
            File[] apps = appRootDir.listFiles();
            for (File app : apps) {
                if (!app.isDirectory()) {
                    continue;
                }
                scanPackage(app.getName());
            }
        }
    }

    public void scanPackage(String packageName) {
        synchronized (mPackages) {
            updatePackageLP(PEnvironment.getAppDir(packageName));
        }
    }

    private void updatePackageLP(File app) {
        String packageName = app.getName();
        Parcel packageSettingsIn = Parcel.obtain();
        File packageConf = PEnvironment.getPackageConf(packageName);
        try {
            byte[] bPackageSettingsBytes = FileUtils.toByteArray(packageConf);

            packageSettingsIn.unmarshall(bPackageSettingsBytes, 0, bPackageSettingsBytes.length);
            packageSettingsIn.setDataPosition(0);

            PPackageSettings bPackageSettings = new PPackageSettings(packageSettingsIn);
            bPackageSettings.pkg.mExtras = bPackageSettings;
            if (bPackageSettings.installOption.isFlag(InstallOption.FLAG_SYSTEM)) {
                PackageInfo packageInfo = PrisonCore.getContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
                String currPackageSourcePath = packageInfo.applicationInfo.sourceDir;
                if (!currPackageSourcePath.equals(bPackageSettings.pkg.baseCodePath)) {
                    // update baseCodePath And Re install
                    ProcessManagerService.get().killAllByPackageName(bPackageSettings.pkg.packageName);
                    PPackageSettings newPkg = reInstallBySystem(packageInfo, bPackageSettings.installOption);
                    bPackageSettings.pkg = newPkg.pkg;
                }
            } else {
                bPackageSettings.pkg.applicationInfo = PackageManagerCompat.generateApplicationInfo(bPackageSettings.pkg, 0, BPackageUserState.create(), 0);
            }
            bPackageSettings.save();
            mPackages.put(bPackageSettings.pkg.packageName, bPackageSettings);
            Logger.d(TAG, "loaded Package: " + packageName);
        } catch (Throwable e) {
            e.printStackTrace();
            // bad package
            FileUtils.deleteDir(app);
            removePackage(packageName);
            ProcessManagerService.get().killAllByPackageName(packageName);
            PackageManagerService.get().onPackageUninstalled(packageName, true, BUserHandle.USER_ALL);
            Logger.d(TAG, "bad Package: " + packageName);
        } finally {
            packageSettingsIn.recycle();
        }
    }

    private PPackageSettings reInstallBySystem(PackageInfo systemPackageInfo, InstallOption option) throws Exception {
        Logger.d(TAG, "reInstallBySystem: " + systemPackageInfo.packageName);
        PackageParser.Package aPackage = parserApk(systemPackageInfo.applicationInfo.sourceDir);
        if (aPackage == null) {
            throw new RuntimeException("parser apk error.");
        }
        aPackage.applicationInfo = PrisonCore.getContext().getPackageManager().getPackageInfo(aPackage.packageName, 0).applicationInfo;
        return getPackageLPw(aPackage.packageName, aPackage, option);
    }

    public void removePackage(String packageName) {
        mPackages.remove(packageName);
    }

    private PackageParser.Package parserApk(String file) {
        try {
            PackageParser parser = PackageParserCompat.createParser(new File(file));
            PackageParser.Package aPackage = PackageParserCompat.parsePackage(parser, new File(file), 0);
            PackageParserCompat.collectCertificates(parser, aPackage, 0);
            return aPackage;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }
}
