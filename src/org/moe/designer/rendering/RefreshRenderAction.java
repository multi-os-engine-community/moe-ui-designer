/*
 * Copyright (C) 2014 The Android Open Source Project
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
package org.moe.designer.rendering;

import com.android.sdklib.IAndroidTarget;
import org.moe.designer.android.AndroidFacet;
import org.moe.designer.android.sdk.AndroidTargetData;
import org.moe.designer.configurations.Configuration;
import org.moe.designer.configurations.ConfigurationListener;
import org.moe.designer.configurations.RenderContext;
import org.moe.designer.uipreview.ProjectClassLoader;
import org.moe.designer.utils.AndroidBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;

public class RefreshRenderAction extends AnAction {
  private final RenderContext myContext;

  public RefreshRenderAction(RenderContext context) {
//    super(AndroidBundle.message("android.layout.preview.refresh.action.text"), null, AllIcons.Actions.Refresh);
    super("ios.layout.preview.refresh.action.text", null, AllIcons.Actions.Refresh);
    myContext = context;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ProjectClassLoader.clearCache();
    Configuration configuration = myContext.getConfiguration();

    if (configuration != null) {
      // Clear layoutlib bitmap cache (in case files have been modified externally)
      IAndroidTarget target = configuration.getTarget();
      Module module = configuration.getModule();
      if (target != null && module != null) {
        AndroidTargetData targetData = AndroidTargetData.getTargetData(target, module);
        if (targetData != null) {
          targetData.clearLayoutBitmapCache(module);
        }
      }

      AndroidFacet facet = AndroidFacet.getInstance(configuration.getModule());
      if (facet != null) {
        facet.refreshResources();
      }

      configuration.updated(ConfigurationListener.MASK_RENDERING);
    }

    myContext.requestRender();
  }
}
