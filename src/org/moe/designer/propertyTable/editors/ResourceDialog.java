/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.moe.designer.propertyTable.editors;

import com.android.resources.ResourceType;
//import com.intellij.android.designer.model.RadViewComponent;
import org.moe.designer.model.RadViewComponent;
import org.moe.designer.uipreview.ChooseResourceDialog;
import com.intellij.openapi.module.Module;
//import org.jetbrains.android.uipreview.ChooseResourceDialog;

/**
 * @author Alexander Lobas
 */
public class ResourceDialog extends ChooseResourceDialog {
  public ResourceDialog(Module module, ResourceType[] types, String value, RadViewComponent component) {
    super(module, types, value, component != null ? component.getTag() : null);
  }
}