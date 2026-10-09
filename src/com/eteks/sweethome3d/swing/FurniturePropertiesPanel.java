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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
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
 * Edited values are set on furniture with its Apply button and discarded with its Reset button
 * or when the selection changes.
 */
public class FurniturePropertiesPanel extends JPanel {
  private final Home                 home;
  private final FurnitureController  furnitureController;
  private final JLabel               noSelectionLabel;
  private final JButton              applyButton;
  private final JButton              resetButton;
  private final PropertyChangeListener furnitureChangeListener;
  private List<HomePieceOfFurniture> editedFurniture = Collections.emptyList();
  private HomeFurnitureController    homeFurnitureController;
  private JComponent                 editedView;
  private boolean                    modified;
  private boolean                    applying;
  private boolean                    active = true;
  private boolean                    updateScheduled;
  private boolean                    updateRequired;

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
    this.applyButton = new JButton(preferences.getLocalizedString(
        FurniturePropertiesPanel.class, "applyButton.text"));
    this.applyButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          apply();
        }
      });
    this.resetButton = new JButton(preferences.getLocalizedString(
        FurniturePropertiesPanel.class, "resetButton.text"));
    this.resetButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          reset();
        }
      });
    int gap = Math.round(5 * SwingTools.getResolutionScale());
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, gap, gap));
    buttonsPanel.add(this.applyButton);
    buttonsPanel.add(this.resetButton);
    add(buttonsPanel, BorderLayout.SOUTH);
    // Nothing can be applied until this panel is updated with the selected furniture
    this.applyButton.setEnabled(false);
    this.resetButton.setEnabled(false);
    showEditedView(null);

    this.furnitureChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          // Keep the edits of the user if furniture is modified elsewhere in the meantime
          if (!applying && !modified) {
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
   * Returns the button which sets the edited values on the selected furniture.
   */
  public JButton getApplyButton() {
    return this.applyButton;
  }

  /**
   * Returns the button which discards the edited values.
   */
  public JButton getResetButton() {
    return this.resetButton;
  }

  /**
   * Returns <code>true</code> if the user edited some values which weren't applied yet.
   */
  public boolean isModified() {
    return this.modified;
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
   * Sets the edited values on the selected furniture.
   */
  public void apply() {
    if (this.homeFurnitureController != null) {
      this.applying = true;
      try {
        this.homeFurnitureController.modifyFurniture();
      } finally {
        this.applying = false;
      }
      update();
    }
  }

  /**
   * Discards the edited values and displays the ones of the selected furniture.
   */
  public void reset() {
    update();
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
   * Updates immediately the edited furniture from the furniture selected in home.
   */
  public void update() {
    if (!this.active) {
      this.updateRequired = true;
      return;
    }
    this.updateRequired = false;
    for (HomePieceOfFurniture piece : this.editedFurniture) {
      piece.removePropertyChangeListener(this.furnitureChangeListener);
    }
    this.editedFurniture = Home.getFurnitureSubList(this.home.getSelectedItems());
    this.modified = false;
    if (this.editedFurniture.isEmpty()) {
      this.homeFurnitureController = null;
      showEditedView(null);
    } else {
      this.homeFurnitureController = this.furnitureController.createHomeFurnitureController();
      Object view = this.homeFurnitureController.getView();
      showEditedView(view instanceof JComponent ? (JComponent)view : null);
      addModificationListeners(this.homeFurnitureController);
      for (HomePieceOfFurniture piece : this.editedFurniture) {
        piece.addPropertyChangeListener(this.furnitureChangeListener);
      }
    }
    this.applyButton.setEnabled(this.homeFurnitureController != null);
    this.resetButton.setEnabled(this.homeFurnitureController != null);
  }

  /**
   * Adds to the given controller the listeners which track the edits of the user.
   */
  private void addModificationListeners(HomeFurnitureController controller) {
    PropertyChangeListener modificationListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          modified = true;
        }
      };
    for (HomeFurnitureController.Property property : HomeFurnitureController.Property.values()) {
      controller.addPropertyChangeListener(property, modificationListener);
    }
    TextureChoiceController textureController = controller.getTextureController();
    if (textureController != null) {
      textureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE, modificationListener);
    }
    ModelMaterialsController modelMaterialsController = controller.getModelMaterialsController();
    if (modelMaterialsController != null) {
      modelMaterialsController.addPropertyChangeListener(ModelMaterialsController.Property.MATERIALS, modificationListener);
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
