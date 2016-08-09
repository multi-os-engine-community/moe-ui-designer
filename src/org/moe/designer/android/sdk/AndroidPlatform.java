/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.moe.designer.android.sdk;

import com.android.SdkConstants;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import org.moe.designer.android.AndroidFacet;
import org.moe.designer.utils.IOSPsiUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.HashSet;
//import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

//import static org.jetbrains.android.sdk.AndroidSdkUtils.isAndroidSdk;

/**
 * Created by IntelliJ IDEA.
 * User: Eugene.Kudelevsky
 * Date: Aug 15, 2009
 * Time: 6:54:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AndroidPlatform {
  public static AndroidSdkData mySdkData;
  private static Module myModule = null;
  private final IAndroidTarget myTarget;
  private static AndroidPlatform myPlatform = null;

  public AndroidPlatform(@NotNull AndroidSdkData sdkData, @NotNull IAndroidTarget target) {
    mySdkData = sdkData;
    myTarget = target;
    myPlatform = this;
  }

  @Nullable
  public static AndroidPlatform getInstance(@NotNull Module module) {
    if (myPlatform == null) {
      myPlatform = getPlatform(module);
//      final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
//      myPlatform = sdk != null ? getInstance(sdk) : null;
//      myPlatform = sdk != null ? parse(sdk) : null;
    }
    return myPlatform;
  }

  @Nullable
  public static AndroidPlatform getInstance(@NotNull Sdk sdk) {
//    if (!(sdk.getSdkType() instanceof AndroidSdkType)) {
//      return null;
//    }

//    if (!(sdk.getSdkType().equals("Android SDK"))) {
//      return null;
//    }
//
//    final AndroidSdkAdditionalData sdkAdditionalData = (AndroidSdkAdditionalData)sdk.getSdkAdditionalData();
//    if (sdkAdditionalData == null) {
//      return null;
//    }
//    return sdkAdditionalData.getAndroidPlatform();
    return  AndroidPlatform.parse(sdk);
  }

  @NotNull
  public static AndroidSdkData getSdkData() {
    return mySdkData;
  }

  @NotNull
  public IAndroidTarget getTarget() {
    return myTarget;
  }

  @Nullable
  public static AndroidPlatform parse(Sdk sdk) {
//    if (!isAndroidSdk(sdk)) {
//      return null;
//    }
    String sdkPath = IOSPsiUtils.getAndroidSDKPath(myModule.getProject());
    if (sdkPath != null) {
      mySdkData = AndroidSdkData.getSdkData(sdkPath);
      if (mySdkData != null) {
        IAndroidTarget target = IOSPsiUtils.getAndroidTarget(myModule.getProject());
        if (target != null) {
          return new AndroidPlatform(mySdkData, target);
        }
      }
    }
    return null;
  }

  // deprecated, use only for converting
  @Nullable
  @Deprecated
  public static AndroidPlatform parse(@NotNull Library library,
                                      @Nullable Library.ModifiableModel model,
                                      @Nullable Map<String, AndroidSdkData> parsedSdks) {
    VirtualFile[] files = model != null ? model.getFiles(OrderRootType.CLASSES) : library.getFiles(OrderRootType.CLASSES);
    Set<String> jarPaths = new HashSet<String>();
    VirtualFile frameworkLibrary = null;
    for (VirtualFile file : files) {
      VirtualFile vFile = JarFileSystem.getInstance().getVirtualFileForJar(file);
      if (vFile != null) {
        if (vFile.getName().equals(SdkConstants.FN_FRAMEWORK_LIBRARY)) {
          frameworkLibrary = vFile;
        }
        jarPaths.add(vFile.getPath());
      }
    }
    if (frameworkLibrary != null) {
      VirtualFile sdkDir = frameworkLibrary.getParent();
      if (sdkDir != null) {
        VirtualFile platformsDir = sdkDir.getParent();
        if (platformsDir != null && platformsDir.getName().equals(SdkConstants.FD_PLATFORMS)) {
          sdkDir = platformsDir.getParent();
          if (sdkDir == null) return null;
        }
        String sdkPath = sdkDir.getPath();
        AndroidSdkData sdkData = parsedSdks != null ? parsedSdks.get(sdkPath) : null;
        if (sdkData == null) {
          sdkData = AndroidSdkData.getSdkData(sdkPath);
          if (sdkData == null) return null;
          if (parsedSdks != null) {
            parsedSdks.put(sdkPath, sdkData);
          }
        }
        IAndroidTarget resultTarget = null;
        for (IAndroidTarget target : sdkData.getTargets()) {
          String targetsFrameworkLibPath = PathUtil.getCanonicalPath(target.getPath(IAndroidTarget.ANDROID_JAR));
          if (frameworkLibrary.getPath().equals(targetsFrameworkLibPath)) {
            if (target.isPlatform()) {
              if (resultTarget == null) resultTarget = target;
            }
            else {
              boolean ok = true;
              IAndroidTarget.IOptionalLibrary[] libraries = target.getOptionalLibraries();
              if (libraries == null) {
                // we cannot identify add-on target without optional libraries by classpath
                ok = false;
              }
              else {
                for (IAndroidTarget.IOptionalLibrary optionalLibrary : libraries) {
                  if (!jarPaths.contains(PathUtil.getCanonicalPath(optionalLibrary.getJarPath()))) {
                    ok = false;
                  }
                }
              }
              if (ok) resultTarget = target;
            }
          }
        }
        if (resultTarget != null) {
          return new AndroidPlatform(sdkData, resultTarget);
        }
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AndroidPlatform platform = (AndroidPlatform)o;

    if (!mySdkData.equals(platform.mySdkData)) return false;
    if (!myTarget.equals(platform.myTarget)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mySdkData.hashCode();
    result = 31 * result + myTarget.hashCode();
    return result;
  }

  public boolean needToAddAnnotationsJarToClasspath() {
    // todo: check if we really don't need to add this
    return AndroidSdkUtils.needsAnnotationsJarInClasspath(myTarget);
  }

  public int getApiLevel() {
    return myTarget.getVersion().getApiLevel();
  }

  public AndroidVersion getApiVersion() {
    return myTarget.getVersion();
  }

  /** Looks up the platform for a given module */
  @Nullable
  public static AndroidPlatform getPlatform(Module module) {
    myModule = module;
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    return AndroidPlatform.parse(sdk);
  }
}
