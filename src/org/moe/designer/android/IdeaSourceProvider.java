/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.moe.designer.android;

import com.android.builder.model.*;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Like {@link SourceProvider}, but for IntelliJ, which means it provides
 * {@link VirtualFile} references rather than {@link File} references.
 *
 * @see org.jetbrains.android.facet.AndroidSourceType
 */
public abstract class IdeaSourceProvider {
  private IdeaSourceProvider() {
  }

  @NotNull
  public static IdeaSourceProvider create(@NotNull SourceProvider provider) {
    return new IdeaSourceProvider.Gradle(provider);
  }

  @NotNull
  public static IdeaSourceProvider create(@NotNull final AndroidFacet facet) {
    return new IdeaSourceProvider.Legacy(facet);
  }

  @NotNull
  public abstract String getName();

  @Nullable
  public abstract VirtualFile getManifestFile();

  @NotNull
  public abstract Collection<VirtualFile> getJavaDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getResourcesDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getAidlDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getRenderscriptDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getCDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getCppDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getJniLibsDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getResDirectories();

  @NotNull
  public abstract Collection<VirtualFile> getAssetsDirectories();

  /** {@linkplain IdeaSourceProvider} for a Gradle projects */
  private static class Gradle extends IdeaSourceProvider {
    private final SourceProvider myProvider;
    private VirtualFile myManifestFile;
    private File myManifestIoFile;

    private Gradle(@NotNull SourceProvider provider) {
      myProvider = provider;
    }

    @NotNull
    @Override
    public String getName() {
      return myProvider.getName();
    }

    @Nullable
    @Override
    public VirtualFile getManifestFile() {
      File manifestFile = myProvider.getManifestFile();
      if (myManifestFile == null || !FileUtil.filesEqual(manifestFile, myManifestIoFile)) {
        myManifestIoFile = manifestFile;
        myManifestFile = LocalFileSystem.getInstance().findFileByIoFile(manifestFile);
      }

      return myManifestFile;
    }

    /** Convert a set of IO files into a set of equivalent virtual files */
    private static Collection<VirtualFile> convertFileSet(@NotNull Collection<File> fileSet) {
      Collection<VirtualFile> result = Sets.newHashSetWithExpectedSize(fileSet.size());
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      for (File file : fileSet) {
        VirtualFile virtualFile = fileSystem.findFileByIoFile(file);
        if (virtualFile != null) {
          result.add(virtualFile);
        }
      }
      return result;
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getJavaDirectories() {
      return convertFileSet(myProvider.getJavaDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getResourcesDirectories() {
      return convertFileSet(myProvider.getResourcesDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getAidlDirectories() {
      return convertFileSet(myProvider.getAidlDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getRenderscriptDirectories() {
      return convertFileSet(myProvider.getRenderscriptDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getCDirectories() {
      return convertFileSet(myProvider.getCDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getCppDirectories() {
      return convertFileSet(myProvider.getCppDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getJniLibsDirectories() {
      return convertFileSet(myProvider.getJniLibsDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getResDirectories() {
      // TODO: Perform some caching; this method gets called a lot!
      return convertFileSet(myProvider.getResDirectories());
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getAssetsDirectories() {
      return convertFileSet(myProvider.getAssetsDirectories());
    }

    /**
     * Compares another source provider with this for equality. Returns true if the specified object is also a Gradle source provider,
     * has the same name, and the same set of source locations.
     */
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Gradle that = (Gradle)o;
      if (!myProvider.getName().equals(that.getName())) return false;
      if (!myProvider.getManifestFile().getPath().equals(that.myProvider.getManifestFile().getPath())) return false;

      return true;
    }

    /**
     * Returns the hash code for this source provider. The hash code simply provides the hash of the manifest file's location,
     * but this follows the required contract that if two source providers are equal, their hash codes will be the same.
     */
    @Override
    public int hashCode() {
      return myProvider.getManifestFile().getPath().hashCode();
    }
  }

  /** {@linkplain IdeaSourceProvider} for a legacy (non-Gradle) Android project */
  private static class Legacy extends IdeaSourceProvider {
    @NotNull private final AndroidFacet myFacet;

    private Legacy(@NotNull AndroidFacet facet) {
      myFacet = facet;
    }

    @NotNull
    @Override
    public String getName() {
      return "";
    }

    @Nullable
    @Override
    public VirtualFile getManifestFile() {
      return AndroidRootUtil.getFileByRelativeModulePath(myFacet.getModule(), myFacet.getProperties().MANIFEST_FILE_RELATIVE_PATH, true);
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getJavaDirectories() {
      Module module = myFacet.getModule();
      Collection<VirtualFile> dirs = new HashSet<VirtualFile>();
      Collections.addAll(dirs, ModuleRootManager.getInstance(module).getContentRoots());
      return dirs;
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getResourcesDirectories() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getAidlDirectories() {
      final VirtualFile dir = AndroidRootUtil.getAidlGenDir(myFacet);
      assert dir != null;
      return Collections.singleton(dir);
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getRenderscriptDirectories() {
      final VirtualFile dir = AndroidRootUtil.getRenderscriptGenDir(myFacet);
      assert dir != null;
      return Collections.singleton(dir);
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getCDirectories() {
     return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getCppDirectories() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getJniLibsDirectories() {
      return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getResDirectories() {
      String resRelPath = myFacet.getProperties().RES_FOLDER_RELATIVE_PATH;
      final VirtualFile dir =  AndroidRootUtil.getFileByRelativeModulePath(myFacet.getModule(), resRelPath, true);
      if (dir != null) {
        return Collections.singleton(dir);
      } else {
        return Collections.emptySet();
      }
    }

    @NotNull
    @Override
    public Collection<VirtualFile> getAssetsDirectories() {
      final VirtualFile dir = AndroidRootUtil.getAssetsDir(myFacet);
      assert dir != null;
      return Collections.singleton(dir);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Legacy that = (Legacy)o;
      return myFacet.equals(that.myFacet);
    }

    @Override
    public int hashCode() {
      return myFacet.hashCode();
    }

  }

  /**
   * Returns a list of source providers, in the overlay order (meaning that later providers
   * override earlier providers when they redefine resources) for the currently selected variant.
   * <p>
   * Note that the list will never be empty; there is always at least one source provider.
   * <p>
   * The overlay source order is defined by the Android Gradle plugin.
   */
  @NotNull
  public static List<IdeaSourceProvider> getCurrentSourceProviders(@NotNull AndroidFacet facet) {
    if (!facet.isGradleProject()) {
      return Collections.singletonList(facet.getMainIdeaSourceProvider());
    }

    List<IdeaSourceProvider> providers = Lists.newArrayList();

    providers.add(facet.getMainIdeaSourceProvider());
    List<IdeaSourceProvider> flavorSourceProviders = facet.getIdeaFlavorSourceProviders();
    if (flavorSourceProviders != null) {
      for (IdeaSourceProvider provider : flavorSourceProviders) {
        providers.add(provider);
      }
    }

    IdeaSourceProvider multiProvider = facet.getIdeaMultiFlavorSourceProvider();
    if (multiProvider != null) {
      providers.add(multiProvider);
    }

    IdeaSourceProvider buildTypeSourceProvider = facet.getIdeaBuildTypeSourceProvider();
    if (buildTypeSourceProvider != null) {
      providers.add(buildTypeSourceProvider);
    }

    IdeaSourceProvider variantProvider = facet.getIdeaVariantSourceProvider();
    if (variantProvider != null) {
      providers.add(variantProvider);
    }

    return providers;
  }

//  @NotNull
//  public static List<IdeaSourceProvider> getCurrentTestSourceProviders(@NotNull AndroidFacet facet) {
//    if (!facet.isGradleProject()) {
//      return Collections.emptyList();
//    }
//
//    List<IdeaSourceProvider> providers = Lists.newArrayList();
//
//    providers.addAll(facet.getMainIdeaTestSourceProviders());
//    providers.addAll(facet.getIdeaFlavorTestSourceProviders());
//
//    //TODO: Does this make sense?
//    //providers.addAll(facet.getIdeaMultiFlavorTestSourceProviders());
//
//    providers.addAll(facet.getIdeaBuildTypeTestSourceProvider());
//
//    //TODO: Does this make sense?
//    //providers.addAll(facet.getIdeaVariantTestSourceProvider());
//
//    return providers;
//  }

  private Collection<VirtualFile> getAllSourceFolders() {
    List<VirtualFile> srcDirectories = Lists.newArrayList();
    srcDirectories.addAll(getJavaDirectories());
    srcDirectories.addAll(getResDirectories());
    srcDirectories.addAll(getAidlDirectories());
    srcDirectories.addAll(getRenderscriptDirectories());
    srcDirectories.addAll(getAssetsDirectories());
    srcDirectories.addAll(getCDirectories());
    srcDirectories.addAll(getCppDirectories());
    srcDirectories.addAll(getJniLibsDirectories());
    return srcDirectories;
  }

  private static Collection<File> getAllSourceFolders(SourceProvider provider) {
    List<File> srcDirectories = Lists.newArrayList();
    srcDirectories.addAll(provider.getJavaDirectories());
    srcDirectories.addAll(provider.getResDirectories());
    srcDirectories.addAll(provider.getAidlDirectories());
    srcDirectories.addAll(provider.getRenderscriptDirectories());
    srcDirectories.addAll(provider.getAssetsDirectories());
    srcDirectories.addAll(provider.getCDirectories());
    srcDirectories.addAll(provider.getCppDirectories());
    srcDirectories.addAll(provider.getJniLibsDirectories());
    return srcDirectories;
  }

  /**
   * Returns true iff this SourceProvider provides the source folder that contains the given file.
   */
  public boolean containsFile(@NotNull VirtualFile file) {
    Collection<VirtualFile> srcDirectories = getAllSourceFolders();
    if (file.equals(getManifestFile())) {
      return true;
    }

    for (VirtualFile container : srcDirectories) {
      if (!container.exists()) {
        continue;
      }

      if (VfsUtilCore.isAncestor(container, file, false /* allow them to be the same */)) {
        return true;
      }

      // Check the flavor root directories
      if (file.equals(container.getParent())) {
        return true;
      }
    }
    return false;
  }


  /**
   * Returns true if this SourceProvider has one or more source folders contained by (or equal to)
   * the given folder.
   */
  public static boolean isContainedBy(@NotNull SourceProvider provider, @NotNull File targetFolder) {
    Collection<File> srcDirectories = getAllSourceFolders(provider);
    for (File container : srcDirectories) {
      if (FileUtil.isAncestor(targetFolder, container, false)) {
        return true;
      }

      if (!container.exists()) {
        continue;
      }

      if (VfsUtilCore.isAncestor(targetFolder, container, false /* allow them to be the same */)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true iff this SourceProvider provides the source folder that contains the given file.
   */
  public static boolean containsFile(@NotNull SourceProvider provider, @NotNull File file) {
    Collection<File> srcDirectories = getAllSourceFolders(provider);
    if (FileUtil.filesEqual(provider.getManifestFile(), file)) {
      return true;
    }

    for (File container : srcDirectories) {
      // Check the flavor root directories
      File parent = container.getParentFile();
      if (parent != null && parent.isDirectory() && FileUtil.filesEqual(parent, file)) {
        return true;
      }

      // Don't do ancestry checking if this file doesn't exist
      if (!container.exists()) {
        continue;
      }

      if (VfsUtilCore.isAncestor(container, file, false /* allow them to be the same */)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Returns true if this SourceProvider has one or more source folders contained by (or equal to)
   * the given folder.
   */
  public boolean isContainedBy(@NotNull VirtualFile targetFolder) {
    Collection<VirtualFile> srcDirectories = getAllSourceFolders();
    for (VirtualFile container : srcDirectories) {
      if (!container.exists()) {
        continue;
      }

      if (VfsUtilCore.isAncestor(targetFolder, container, false /* allow them to be the same */)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns an iterable of all source providers, for the given facet,
   * in the overlay order (meaning that later providers
   * override earlier providers when they redefine resources.)
   * <p>
   * Note that the list will never be empty; there is always at least one source provider.
   * <p>
   * The overlay source order is defined by the Android Gradle plugin.
   */
  @NotNull
  public static List<SourceProvider> getAllSourceProviders(@NotNull AndroidFacet facet) {
    if (!facet.isGradleProject() || facet.getIdeaAndroidProject() == null) {
      return Collections.singletonList(facet.getMainSourceProvider());
    }

    AndroidProject androidProject = facet.getIdeaAndroidProject().getDelegate();
    Collection<Variant> variants = androidProject.getVariants();
    List<SourceProvider> providers = Lists.newArrayList();

    // Add main source set
    providers.add(facet.getMainSourceProvider());

    // Add all flavors
    Collection<ProductFlavorContainer> flavors = androidProject.getProductFlavors();
    for (ProductFlavorContainer pfc : flavors) {
      providers.add(pfc.getSourceProvider());
    }

    // Add the multi-flavor source providers
    for (Variant v : variants) {
      SourceProvider provider = v.getMainArtifact().getMultiFlavorSourceProvider();
      if (provider != null) {
        providers.add(provider);
      }
    }

    // Add all the build types
    Collection<BuildTypeContainer> buildTypes = androidProject.getBuildTypes();
    for (BuildTypeContainer btc : buildTypes) {
      providers.add(btc.getSourceProvider());
    }

    // Add all the variant source providers
    for (Variant v : variants) {
      SourceProvider provider = v.getMainArtifact().getVariantSourceProvider();
      if (provider != null) {
        providers.add(provider);
      }
    }

    return providers;
  }

  /**
   * Returns a list of all IDEA source providers, for the given facet, in the overlay order
   * (meaning that later providers override earlier providers when they redefine resources.)
   * <p>
   * Note that the list will never be empty; there is always at least one source provider.
   * <p>
   * The overlay source order is defined by the Android Gradle plugin.
   *
   * This method should be used when only on-disk source sets are required. It will return
   * empty source sets for all other source providers (since VirtualFiles MUST exist on disk).
   */
  @NotNull
  public static List<IdeaSourceProvider> getAllIdeaSourceProviders(@NotNull AndroidFacet facet) {
    List<IdeaSourceProvider> ideaSourceProviders = Lists.newArrayList();
    for (SourceProvider sourceProvider : getAllSourceProviders(facet)) {
      ideaSourceProviders.add(create(sourceProvider));
    }
    return ideaSourceProviders;
  }

  /**
   * Returns a list of all IDEA source providers that contain, or are contained by, the given file.
   * For example, with the file structure:
   * <pre>
   * src
   *   main
   *     aidl
   *       myfile.aidl
   *   free
   *     aidl
   *       myoverlay.aidl
   * </pre>
   *
   * With target file == "myoverlay.aidl" the returned list would be ['free'], but if target file == "src",
   * the returned list would be ['main', 'free'] since both of those source providers have source folders which
   * are descendants of "src."
   */
  @NotNull
  public static List<IdeaSourceProvider> getIdeaSourceProvidersForFile(@NotNull AndroidFacet facet,
                                                                       @Nullable VirtualFile targetFolder,
                                                                       @Nullable IdeaSourceProvider defaultIdeaSourceProvider) {
    List<IdeaSourceProvider> sourceProviderList = Lists.newArrayList();


    if (targetFolder != null) {
      // Add source providers that contain the file (if any) and any that have files under the given folder
      for (IdeaSourceProvider provider : getAllIdeaSourceProviders(facet)) {
        if (provider.containsFile(targetFolder) || provider.isContainedBy(targetFolder)) {
          sourceProviderList.add(provider);
        }
      }
    }

    if (sourceProviderList.isEmpty() && defaultIdeaSourceProvider != null) {
      sourceProviderList.add(defaultIdeaSourceProvider);
    }
    return sourceProviderList;
  }

  /**
   * Returns a list of all source providers that contain, or are contained by, the given file.
   * For example, with the file structure:
   * <pre>
   * src
   *   main
   *     aidl
   *       myfile.aidl
   *   free
   *     aidl
   *       myoverlay.aidl
   * </pre>
   *
   * With target file == "myoverlay.aidl" the returned list would be ['free'], but if target file == "src",
   * the returned list would be ['main', 'free'] since both of those source providers have source folders which
   * are descendants of "src."
   */
  @NotNull
  public static List<SourceProvider> getSourceProvidersForFile(@NotNull AndroidFacet facet, @Nullable VirtualFile targetFolder,
                                                               @Nullable SourceProvider defaultSourceProvider) {
    List<SourceProvider> sourceProviderList = Lists.newArrayList();


    if (targetFolder != null) {
      File targetIoFolder = VfsUtilCore.virtualToIoFile(targetFolder);
      // Add source providers that contain the file (if any) and any that have files under the given folder
      for (SourceProvider provider : getAllSourceProviders(facet)) {
        if (containsFile(provider, targetIoFolder) || isContainedBy(provider, targetIoFolder)) {
          sourceProviderList.add(provider);
        }
      }
    }

    if (sourceProviderList.isEmpty() && defaultSourceProvider != null) {
      sourceProviderList.add(defaultSourceProvider);
    }
    return sourceProviderList;
  }

  /** Returns true if the given candidate file is a manifest file in the given module */
  public static boolean isManifestFile(@NotNull AndroidFacet facet, @Nullable VirtualFile candidate) {
    if (candidate == null) {
      return false;
    }

    if (facet.isGradleProject()) {
      for (IdeaSourceProvider provider : getCurrentSourceProviders(facet)) {
        if (candidate.equals(provider.getManifestFile())) {
          return true;
        }
      }
      return false;
    } else {
      return candidate.equals(facet.getMainIdeaSourceProvider().getManifestFile());
    }
  }

  /** Returns the manifest files in the given module */
  @NotNull
  public static List<VirtualFile> getManifestFiles(@NotNull AndroidFacet facet) {
    VirtualFile main = facet.getMainIdeaSourceProvider().getManifestFile();
    if (!facet.isGradleProject()) {
      return main != null ? Collections.singletonList(main) : Collections.<VirtualFile>emptyList();
    }
    List<VirtualFile> files = Lists.newArrayList();
    if (main != null) {
      files.add(main);
    }

    for (IdeaSourceProvider provider : getCurrentSourceProviders(facet)) {
      VirtualFile manifest = provider.getManifestFile();
      if (manifest != null) {
        files.add(manifest);
      }
    }

    return files;
  }

  public static Function<IdeaSourceProvider, List<VirtualFile>> MANIFEST_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      VirtualFile manifestFile = provider.getManifestFile();
      return manifestFile == null ? Collections.<VirtualFile>emptyList() : Collections.singletonList(manifestFile);
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> RES_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getResDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> JAVA_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getJavaDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> RESOURCES_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getResourcesDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> AIDL_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getAidlDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> C_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getCDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> CPP_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getCppDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> JNI_LIBS_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getJniLibsDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> ASSETS_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getAssetsDirectories());
    }
  };

  public static Function<IdeaSourceProvider, List<VirtualFile>> RS_PROVIDER = new Function<IdeaSourceProvider, List<VirtualFile>>() {
    @Override
    public List<VirtualFile> apply(IdeaSourceProvider provider) {
      return Lists.newArrayList(provider.getRenderscriptDirectories());
    }
  };
}
