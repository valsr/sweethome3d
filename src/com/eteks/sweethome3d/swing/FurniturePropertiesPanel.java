/*
 * FurniturePropertiesPanel.java
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;
import com.eteks.sweethome3d.viewcontroller.ModelMaterialsController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;

/**
 * A panel which edits the properties of the furniture selected in a home without dialog box.
 * Each change of the user is applied immediately to the selected furniture, and successive
 * changes of the same property may be undone at once.
 */
public class FurniturePropertiesPanel extends JPanel {
  /**
   * The properties of furniture controller which don't describe a modification of furniture.
   */
  private static final EnumSet<HomeFurnitureController.Property> UNEDITED_PROPERTIES =
      EnumSet.of(HomeFurnitureController.Property.ICON,
                 HomeFurnitureController.Property.PROPORTIONAL,
                 HomeFurnitureController.Property.RESIZABLE,
                 HomeFurnitureController.Property.DEFORMABLE,
                 HomeFurnitureController.Property.TEXTURABLE);

  private final Home                   home;
  private final FurnitureController    furnitureController;
  private final JLabel                 noSelectionLabel;
  private final PropertyChangeListener furnitureChangeListener;
  private List<HomePieceOfFurniture>   editedFurniture = Collections.emptyList();
  private HomeFurnitureController      homeFurnitureController;
  private JComponent                   editedView;
  private Object                       mergedEditsKey;
  private HomeFurnitureController.Property modifiedProperty;
  private boolean                      modifying;
  private boolean                      refreshing;
  private boolean                      active = true;
  private boolean                      updateScheduled;
  private boolean                      updateRequired;

  /**
   * Creates a panel editing the furniture selected in <code>home</code>
   * with the controllers created by the given <code>furnitureController</code>.
   */
  public FurniturePropertiesPanel(Home home,
                                  UserPreferences preferences,
                                  FurnitureController furnitureController) {
    super(new BorderLayout());
    this.home = home;
    this.furnitureController = furnitureController;
    setMinimumSize(new Dimension());

    this.noSelectionLabel = new JLabel(preferences.getLocalizedString(
        FurniturePropertiesPanel.class, "noSelectionLabel.text"), JLabel.CENTER);
    this.noSelectionLabel.setEnabled(false);
    showEditedView(null);

    this.furnitureChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          // Display the changes made elsewhere, like in the plan or by an undo
          if (!modifying) {
            scheduleUpdate();
          }
        }
      };
    home.addSelectionListener(new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          scheduleUpdate();
        }
      });
    scheduleUpdate();
  }

  /**
   * Returns the controller of the furniture currently edited by this panel,
   * or <code>null</code> if no furniture is edited.
   */
  public HomeFurnitureController getFurnitureController() {
    return this.homeFurnitureController;
  }

  /**
   * Returns <code>true</code> if this panel is kept up to date with the selected furniture.
   */
  public boolean isActive() {
    return this.active;
  }

  /**
   * Sets whether this panel should be kept up to date with the selected furniture.
   * An inactive panel avoids the cost of creating editing components which aren't visible.
   */
  public void setActive(boolean active) {
    if (active != this.active) {
      this.active = active;
      if (active && this.updateRequired) {
        scheduleUpdate();
      }
    }
  }

  /**
   * Gives the focus to the first text field which edits the selected furniture.
   */
  public void requestFocusInEditedView() {
    if (this.editedView != null) {
      List<JTextComponent> textComponents = SwingTools.findChildren(this.editedView, JTextComponent.class);
      for (JTextComponent textComponent : textComponents) {
        if (textComponent.isEnabled() && textComponent.isEditable()) {
          textComponent.requestFocusInWindow();
          return;
        }
      }
      this.editedView.requestFocusInWindow();
    }
  }

  /**
   * Updates this panel later, to update it only once when various changes happen in a row.
   */
  private void scheduleUpdate() {
    if (!this.updateScheduled) {
      this.updateScheduled = true;
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            updateScheduled = false;
            update();
          }
        });
    }
  }

  /**
   * Updates immediately this panel from the furniture selected in home. The edited values
   * are updated in place if the selected furniture didn't change, to keep the focus where it is.
   */
  public void update() {
    if (!this.active) {
      this.updateRequired = true;
      return;
    }
    this.updateRequired = false;
    List<HomePieceOfFurniture> selectedFurniture = Home.getFurnitureSubList(this.home.getSelectedItems());
    if (this.homeFurnitureController != null
        && selectedFurniture.equals(this.editedFurniture)) {
      this.refreshing = true;
      try {
        this.homeFurnitureController.updateProperties();
      } finally {
        this.refreshing = false;
      }
    } else {
      for (HomePieceOfFurniture piece : this.editedFurniture) {
        piece.removePropertyChangeListener(this.furnitureChangeListener);
      }
      this.editedFurniture = selectedFurniture;
      this.modifiedProperty = null;
      if (selectedFurniture.isEmpty()) {
        this.homeFurnitureController = null;
        showEditedView(null);
      } else {
        this.homeFurnitureController = this.furnitureController.createHomeFurnitureController();
        // Edits are merged only if they are made on the same selection
        this.mergedEditsKey = new Object();
        Object view = this.homeFurnitureController.getView();
        showEditedView(view instanceof JComponent ? (JComponent)view : null);
        addModificationListeners(this.homeFurnitureController);
        for (HomePieceOfFurniture piece : selectedFurniture) {
          piece.addPropertyChangeListener(this.furnitureChangeListener);
        }
      }
    }
  }

  /**
   * Adds to the given controller the listeners which apply the edits of the user.
   */
  private void addModificationListeners(final HomeFurnitureController controller) {
    for (final HomeFurnitureController.Property property : HomeFurnitureController.Property.values()) {
      if (!UNEDITED_PROPERTIES.contains(property)) {
        controller.addPropertyChangeListener(property, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              scheduleModification(controller, property);
            }
          });
      }
    }
    PropertyChangeListener paintModificationListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          scheduleModification(controller, HomeFurnitureController.Property.PAINT);
        }
      };
    TextureChoiceController textureController = controller.getTextureController();
    if (textureController != null) {
      textureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE, paintModificationListener);
    }
    ModelMaterialsController modelMaterialsController = controller.getModelMaterialsController();
    if (modelMaterialsController != null) {
      modelMaterialsController.addPropertyChangeListener(ModelMaterialsController.Property.MATERIALS, paintModificationListener);
    }
  }

  /**
   * Modifies the selected furniture later, to apply at once the properties which change together.
   */
  private void scheduleModification(final HomeFurnitureController controller,
                                    HomeFurnitureController.Property property) {
    // Ignore the changes which display the current state of the selected furniture
    if (!this.refreshing
        && controller == this.homeFurnitureController
        && this.modifiedProperty == null) {
      this.modifiedProperty = property;
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            modifyFurniture(controller);
          }
        });
    }
  }

  /**
   * Sets the values edited with the given controller on the selected furniture.
   */
  private void modifyFurniture(HomeFurnitureController controller) {
    if (controller == this.homeFurnitureController
        && this.modifiedProperty != null) {
      HomeFurnitureController.Property property = this.modifiedProperty;
      this.modifiedProperty = null;
      if (Home.getFurnitureSubList(this.home.getSelectedItems()).equals(this.editedFurniture)) {
        this.modifying = true;
        try {
          // Merge the successive edits of the same property to undo them at once
          controller.modifyFurniture(Arrays.asList(this.mergedEditsKey, property));
        } finally {
          this.modifying = false;
        }
      }
    }
  }

  /**
   * Displays the given view in this panel, or a label if <code>view</code> is <code>null</code>.
   */
  private void showEditedView(JComponent view) {
    // Keep the tab selected in the previous view
    int selectedTabIndex = getSelectedTabIndex(this.editedView);
    BorderLayout layout = (BorderLayout)getLayout();
    if (layout.getLayoutComponent(BorderLayout.CENTER) != null) {
      remove(layout.getLayoutComponent(BorderLayout.CENTER));
    }
    this.editedView = view;
    if (view == null) {
      add(this.noSelectionLabel, BorderLayout.CENTER);
    } else {
      List<JTabbedPane> tabbedPanes = SwingTools.findChildren(view, JTabbedPane.class);
      if (!tabbedPanes.isEmpty()
          && selectedTabIndex > 0
          && selectedTabIndex < tabbedPanes.get(0).getTabCount()
          && tabbedPanes.get(0).isEnabledAt(selectedTabIndex)) {
        tabbedPanes.get(0).setSelectedIndex(selectedTabIndex);
      }
      JScrollPane scrollPane = new JScrollPane(view);
      scrollPane.setBorder(null);
      scrollPane.setMinimumSize(new Dimension());
      add(scrollPane, BorderLayout.CENTER);
    }
    revalidate();
    repaint();
  }

  private int getSelectedTabIndex(JComponent view) {
    if (view != null) {
      List<JTabbedPane> tabbedPanes = SwingTools.findChildren(view, JTabbedPane.class);
      if (!tabbedPanes.isEmpty()) {
        return tabbedPanes.get(0).getSelectedIndex();
      }
    }
    return -1;
  }
}
