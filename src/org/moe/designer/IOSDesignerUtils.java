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
package org.moe.designer;

import com.android.ide.common.rendering.api.ViewInfo;
import com.android.utils.XmlUtils;
import org.moe.designer.android.AndroidFacet;
import org.moe.designer.configurations.Configuration;
import org.moe.designer.configurations.RenderContext;
import org.moe.designer.designSurface.RootView;
import org.moe.designer.ixml.IXmlFile;
import org.moe.designer.model.RadViewComponent;
import org.moe.designer.rendering.RenderLogger;
import org.moe.designer.rendering.RenderService;
import com.intellij.designer.designSurface.EditableArea;
import com.intellij.designer.designSurface.FeedbackLayer;
import com.intellij.designer.model.RadComponent;
import com.intellij.designer.model.RadVisualComponent;
import com.intellij.designer.palette.PaletteItem;
import com.intellij.openapi.module.Module;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.awt.*;
import java.util.List;
import java.util.Locale;

import static com.android.SdkConstants.*;
import static com.android.resources.Density.DEFAULT_DENSITY;

public class IOSDesignerUtils {
  private IOSDesignerUtils() {
  }

  @Nullable
  public static AndroidFacet getFacet(@NotNull EditableArea area) {
    IOSDesignerEditorPanel panel = getPanel(area);
    if (panel != null) {
      Configuration configuration = panel.getConfiguration();
      assert configuration != null;
      Module module = panel.getModule();
      return AndroidFacet.getInstance(module);
    }

    return null;
  }

  @Nullable
  public static RenderService getRenderService(@NotNull EditableArea area) {
    IOSDesignerEditorPanel panel = getPanel(area);
    if (panel != null) {
      Configuration configuration = panel.getConfiguration();
      assert configuration != null;
      IXmlFile xmlFile = panel.getXmlFile();
      Module module = panel.getModule();
      AndroidFacet facet = AndroidFacet.getInstance(module);
      assert facet != null;
      RenderLogger logger = new RenderLogger(xmlFile.getName(), module);
      @SuppressWarnings("UnnecessaryLocalVariable")
      RenderContext renderContext = panel;
      RenderService service = RenderService.create(facet, module, xmlFile, configuration, logger, renderContext);
      assert service != null;
      return service;
    }

    return null;
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // Pixel (px) to Device Independent Pixel (dp) conversion
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  @Nullable
  public static IOSDesignerEditorPanel getPanel(@NotNull EditableArea area) {
    RadComponent root = area.getRootComponent();
    if (root instanceof RadVisualComponent) {
      Component nativeComponent = ((RadVisualComponent)root).getNativeComponent();
      if (nativeComponent instanceof RootView) {
        return ((RootView)nativeComponent).getPanel();
      }
    }

    return null;
  }

  /** Returns the dpi used to render in the designer corresponding to the given {@link EditableArea} */
  public static int getDpi(@NotNull EditableArea area) {
   IOSDesignerEditorPanel panel = getPanel(area);
    if (panel != null) {
      return panel.getDpi();
    }

    return DEFAULT_DENSITY;
  }

  /**
   * Converts a pixel to a dp (device independent pixel) for the current screen density
   *
   * @param area the associated {@link EditableArea}
   * @param px the pixel dimension
   * @return the corresponding dp dimension
   */
  public static int pxToDp(@NotNull EditableArea area, int px) {
    int dpi = getDpi(area);
    return px * DEFAULT_DENSITY / dpi;
  }

  /**
   * Converts a pixel to a dp (device independent pixel) for the current screen density,
   * and returns it as a dimension string.
   *
   * @param area the associated {@link EditableArea}
   * @param px the pixel dimension
   * @return the corresponding dp dimension string
   */
  @NotNull
  public static String pxToDpWithUnits(@NotNull EditableArea area, int px) {
    return String.format(Locale.US, VALUE_N_DP, pxToDp(area, px));
  }

  /**
   * Converts a device independent pixel to a screen pixel for the current screen density
   *
   * @param dp the device independent pixel dimension
   * @return the corresponding pixel dimension
   */
  public static int dpToPx(@NotNull EditableArea area, int dp) {
    int dpi = getDpi(area);
    return dp * dpi / DEFAULT_DENSITY;
  }

  /**
   * Returns the preferred size of the given component in the given editable area
   *
   * @param area the associated {@link EditableArea}
   * @param component the component to measure
   * @return the bounds, if they can be determined
   */
  public static Dimension computePreferredSize(@NotNull EditableArea area, @NotNull RadViewComponent component,
                                               @Nullable RadComponent targetParent) {
    FeedbackLayer layer = area.getFeedbackLayer();
    Dimension size = component.getBounds(layer).getSize();
    if (size.width == 0 && size.height == 0) {

      // TODO: If it's a layout, do something smarter. I really only have to worry about this
      // if the creation XML calls for wrap_content!
      RenderService service = getRenderService(area);
      if (service != null) {
        List<ViewInfo> roots = measureComponent(area, component, targetParent);
        if (roots != null && !roots.isEmpty()) {
          ViewInfo root = roots.get(0);
          size.width = root.getRight() - root.getLeft();
          size.height = root.getBottom() - root.getTop();
          if (size.width != 0 && size.height != 0) {
            // Apply canvas scale!
            if (targetParent != null) {
              return targetParent.fromModel(layer, size);
            } else {
              IOSDesignerEditorPanel panel = getPanel(area);
              if (panel != null) {
                RadComponent rootComponent = panel.getRootComponent();
                if (rootComponent != null) {
                  return rootComponent.fromModel(layer, size);
                }
              }
              return size;
            }
          }
        }
      }

      // Default size when we can't compute it
      size.width = 100;
      size.height = 30;
    }

    return size;
  }

  /**
   * Measures the given component in the given editable area
   *
   * @param area the associated {@link EditableArea}
   * @param component the component to measure
   * @param targetParent if supplied, the target parent intended for the component
   *                     (used to find namespace declarations and size for fill_parent sizes)
   * @return the measured view info objects at the root level
   */
  @Nullable
  public static List<ViewInfo> measureComponent(@NotNull EditableArea area, @NotNull RadViewComponent component,
                                                @Nullable RadComponent targetParent) {
    XmlTag tag = component.getTag();
    if (tag == null) {
      String xml = null;
      PaletteItem initialPaletteItem = component.getInitialPaletteItem();
      if (initialPaletteItem != null) {
        xml = initialPaletteItem.getCreation();
      }
      if (xml == null) {
        xml = component.getCreationXml();
      }
      if (xml != null) {
        String widthValue = VALUE_FILL_PARENT;
        String heightValue = VALUE_FILL_PARENT;

        // Look up the exact size of the parent, if any, such that fill_parent settings inherit the expected size
        if (targetParent instanceof RadViewComponent && (xml.contains(VALUE_FILL_PARENT) || xml.contains(VALUE_MATCH_PARENT))) {
          RadViewComponent parent = (RadViewComponent)targetParent;
          Rectangle paddedBounds = parent.getPaddedBounds();
          if (paddedBounds.width > 0 && paddedBounds.height > 0) {
            widthValue = Integer.toString(paddedBounds.width) + UNIT_PX;
            heightValue = Integer.toString(paddedBounds.height) + UNIT_PX;
          }
        }

        StringBuilder sb = new StringBuilder(xml.length() + 200);
        sb.append('<').append(FRAME_LAYOUT).append(' ').append(XMLNS_ANDROID).append("=\"").append(ANDROID_URI).append('"');
        sb.append(' ').append(ANDROID_NS_NAME).append(':').append(ATTR_LAYOUT_WIDTH).append("=\"").append(widthValue).append('"');
        sb.append(' ').append(ANDROID_NS_NAME).append(':').append(ATTR_LAYOUT_HEIGHT).append("=\"").append(heightValue).append('"');

        // Copy in any other namespaces we may need from the root, such that custom views etc (if any) can refer to these
        if (targetParent != null) {
          RadComponent root = targetParent.getRoot();
          if (root instanceof RadViewComponent) {
            RadViewComponent viewRoot = (RadViewComponent)root;
            XmlTag rootTag = viewRoot.getTag();
            if (rootTag != null) {
              for (XmlAttribute attribute : rootTag.getAttributes()) {
                String name = attribute.getName();
                if (name.startsWith(XMLNS_PREFIX) && !XMLNS_ANDROID.equals(name)) {
                  String value = attribute.getValue();
                  if (value != null && !value.isEmpty()) {
                    sb.append(' ').append(name).append("=\"");
                    XmlUtils.appendXmlAttributeValue(sb, value);
                    sb.append('"');
                  }
                }
              }
            }
          }
        }
        sb.append('>');
        sb.append(xml);
        sb.append('<').append('/').append(FRAME_LAYOUT).append('>');
        Document document = XmlUtils.parseDocumentSilently(sb.toString(), true);
        if (document != null && document.getDocumentElement() != null) {
          RenderService service = getRenderService(area);
          if (service != null) {
            List<ViewInfo> roots = service.measure(document.getDocumentElement());
            if (roots != null && !roots.isEmpty()) {
              ViewInfo root = roots.get(0);
              // Skip the outer layout we added to hold the XML
              if (root.getClassName().equals(FQCN_FRAME_LAYOUT) && !root.getChildren().isEmpty()) {
                return root.getChildren();
              }
            }
          }
        }
      }
    }

    return null;
  }
}
