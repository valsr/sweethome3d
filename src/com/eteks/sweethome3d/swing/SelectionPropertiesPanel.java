/*
 * SelectionPropertiesPanel.java
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeObject;
import com.eteks.sweethome3d.model.ObserverCamera;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.viewcontroller.BaseboardChoiceController;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;
import com.eteks.sweethome3d.viewcontroller.ModelMaterialsController;
import com.eteks.sweethome3d.viewcontroller.ObserverCameraController;
import com.eteks.sweethome3d.viewcontroller.PlanController;
import com.eteks.sweethome3d.viewcontroller.RoomController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;
import com.eteks.sweethome3d.viewcontroller.WallController;

/**
 * A panel which edits the properties of the furniture, the rooms, the walls or the observer camera
 * selected in a home without dialog box. Each change of the user is applied immediately to the selected objects, and
 * successive changes of the same property may be undone at once. Nothing is edited when the
 * selection is empty or contains objects of different kinds.
 */
public class SelectionPropertiesPanel extends JPanel {
  private final Home                   home;
  private final FurnitureController    furnitureController;
  private final PlanController         planController;
  private final JLabel                 noSelectionLabel;
  private final PropertyChangeListener objectChangeListener;
  private List<HomeObject>             editedObjects = Collections.emptyList();
  private Editor                       editor;
  private JComponent                   editedView;
  private Object                       mergedEditsKey;
  private Object                       modifiedProperty;
  private boolean                      modifying;
  private boolean                      refreshing;
  private boolean                      active = true;
  private boolean                      updateScheduled;
  private boolean                      updateRequired;

  /**
   * Creates a panel editing the furniture selected in <code>home</code>
   * with the controllers created by the given <code>furnitureController</code>.
   */
  public SelectionPropertiesPanel(Home home,
                                  UserPreferences preferences,
                                  FurnitureController furnitureController) {
    this(home, preferences, furnitureController, null);
  }

  /**
   * Creates a panel editing the furniture, the rooms, the walls or the observer camera selected
   * in <code>home</code> with the controllers created by the given controllers.
   * @param planController the controller which creates the controllers of rooms, walls and observer camera,
   *            or <code>null</code> if they shouldn't be edited
   */
  public SelectionPropertiesPanel(Home home,
                                  UserPreferences preferences,
                                  FurnitureController furnitureController,
                                  PlanController planController) {
    super(new BorderLayout());
    this.home = home;
    this.furnitureController = furnitureController;
    this.planController = planController;
    setMinimumSize(new Dimension());

    this.noSelectionLabel = new JLabel(preferences.getLocalizedString(
        SelectionPropertiesPanel.class, "noSelectionLabel.text"), JLabel.CENTER);
    this.noSelectionLabel.setEnabled(false);
    showEditedView(null);

    this.objectChangeListener = new PropertyChangeListener() {
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
    return this.editor instanceof FurnitureEditor
        ? ((FurnitureEditor)this.editor).controller
        : null;
  }

  /**
   * Returns the controller of the rooms currently edited by this panel,
   * or <code>null</code> if no room is edited.
   */
  public RoomController getRoomController() {
    return this.editor instanceof RoomsEditor
        ? ((RoomsEditor)this.editor).controller
        : null;
  }

  /**
   * Returns the controller of the walls currently edited by this panel,
   * or <code>null</code> if no wall is edited.
   */
  public WallController getWallController() {
    return this.editor instanceof WallsEditor
        ? ((WallsEditor)this.editor).controller
        : null;
  }

  /**
   * Returns the controller of the observer camera currently edited by this panel,
   * or <code>null</code> if it isn't edited.
   */
  public ObserverCameraController getObserverCameraController() {
    return this.editor instanceof ObserverCameraEditor
        ? ((ObserverCameraEditor)this.editor).controller
        : null;
  }

  /**
   * Returns <code>true</code> if this panel is kept up to date with the selected objects.
   */
  public boolean isActive() {
    return this.active;
  }

  /**
   * Sets whether this panel should be kept up to date with the selected objects.
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
   * Gives the focus to the first text field which edits the selected objects.
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
   * Returns the selected objects this panel is able to edit together,
   * or an empty list if objects of different kinds are selected.
   */
  private List<HomeObject> getEditableSelectedObjects() {
    List<Selectable> selectedItems = this.home.getSelectedItems();
    List<HomeObject> furniture = new ArrayList<HomeObject>(Home.getFurnitureSubList(selectedItems));
    List<HomeObject> rooms = new ArrayList<HomeObject>();
    List<HomeObject> walls = new ArrayList<HomeObject>();
    List<HomeObject> observerCamera = new ArrayList<HomeObject>();
    if (this.planController != null) {
      rooms.addAll(Home.getRoomsSubList(selectedItems));
      walls.addAll(Home.getWallsSubList(selectedItems));
      if (selectedItems.contains(this.home.getObserverCamera())) {
        observerCamera.add(this.home.getObserverCamera());
      }
    }
    int kindsCount = (furniture.isEmpty() ? 0 : 1) + (rooms.isEmpty() ? 0 : 1)
        + (walls.isEmpty() ? 0 : 1) + (observerCamera.isEmpty() ? 0 : 1);
    if (kindsCount != 1) {
      return Collections.emptyList();
    } else if (!furniture.isEmpty()) {
      return furniture;
    } else if (!rooms.isEmpty()) {
      return rooms;
    } else if (!walls.isEmpty()) {
      return walls;
    } else {
      return observerCamera;
    }
  }

  /**
   * Updates immediately this panel from the objects selected in home. The edited values
   * are updated in place if the selected objects didn't change, to keep the focus where it is.
   */
  public void update() {
    if (!this.active) {
      this.updateRequired = true;
      return;
    }
    this.updateRequired = false;
    List<HomeObject> selectedObjects = getEditableSelectedObjects();
    if (this.editor != null
        && selectedObjects.equals(this.editedObjects)) {
      this.refreshing = true;
      try {
        this.editor.refresh();
      } finally {
        this.refreshing = false;
      }
    } else {
      for (HomeObject object : this.editedObjects) {
        object.removePropertyChangeListener(this.objectChangeListener);
      }
      this.editedObjects = selectedObjects;
      this.modifiedProperty = null;
      if (selectedObjects.isEmpty()) {
        this.editor = null;
        showEditedView(null);
      } else {
        if (selectedObjects.get(0) instanceof Room) {
          this.editor = new RoomsEditor();
        } else if (selectedObjects.get(0) instanceof Wall) {
          this.editor = new WallsEditor();
        } else if (selectedObjects.get(0) instanceof ObserverCamera) {
          this.editor = new ObserverCameraEditor();
        } else {
          this.editor = new FurnitureEditor();
        }
        // Edits are merged only if they are made on the same selection
        this.mergedEditsKey = new Object();
        Object view = this.editor.getView();
        showEditedView(view instanceof JComponent ? (JComponent)view : null);
        this.editor.addModificationListeners();
        for (HomeObject object : selectedObjects) {
          object.addPropertyChangeListener(this.objectChangeListener);
        }
      }
    }
  }

  /**
   * Modifies the selected objects later, to apply at once the properties which change together.
   */
  private void scheduleModification(final Editor editor, Object property) {
    // Ignore the changes which display the current state of the selected objects
    if (!this.refreshing
        && editor == this.editor
        && this.modifiedProperty == null) {
      this.modifiedProperty = property;
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            modifySelectedObjects(editor);
          }
        });
    }
  }

  /**
   * Sets the values edited with the given editor on the selected objects.
   */
  private void modifySelectedObjects(Editor editor) {
    if (editor == this.editor
        && this.modifiedProperty != null) {
      Object property = this.modifiedProperty;
      this.modifiedProperty = null;
      if (getEditableSelectedObjects().equals(this.editedObjects)) {
        this.modifying = true;
        try {
          // Merge the successive edits of the same property to undo them at once
          editor.modify(Arrays.asList(this.mergedEditsKey, property));
        } finally {
          this.modifying = false;
        }
      }
    }
  }

  /**
   * The editor of the selected objects of a given kind.
   */
  private abstract class Editor {
    /**
     * Returns the view which displays the edited values.
     */
    public abstract Object getView();

    /**
     * Updates the edited values from the selected objects.
     */
    public abstract void refresh();

    /**
     * Sets the edited values on the selected objects with an undoable edit using the given key.
     */
    public abstract void modify(Object mergeKey);

    /**
     * Adds the listeners which will apply the edits of the user.
     */
    public abstract void addModificationListeners();

    /**
     * Returns a listener which applies the edited values when it's notified,
     * as a change of the given <code>property</code>.
     */
    protected PropertyChangeListener createModificationListener(final Object property) {
      return new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            scheduleModification(Editor.this, property);
          }
        };
    }

    protected void addModificationListeners(BaseboardChoiceController baseboardController, Object property) {
      PropertyChangeListener listener = createModificationListener(property);
      for (BaseboardChoiceController.Property baseboardProperty : BaseboardChoiceController.Property.values()) {
        if (baseboardProperty != BaseboardChoiceController.Property.MAX_HEIGHT) {
          baseboardController.addPropertyChangeListener(baseboardProperty, listener);
        }
      }
      baseboardController.getTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE, listener);
    }
  }

  /**
   * The editor of selected furniture.
   */
  private class FurnitureEditor extends Editor {
    private final HomeFurnitureController controller = furnitureController.createHomeFurnitureController();

    @Override
    public Object getView() {
      return this.controller.getView();
    }

    @Override
    public void refresh() {
      this.controller.updateProperties();
    }

    @Override
    public void modify(Object mergeKey) {
      this.controller.modifyFurniture(mergeKey);
    }

    @Override
    public void addModificationListeners() {
      for (HomeFurnitureController.Property property : HomeFurnitureController.Property.values()) {
        // Ignore the properties which don't describe a modification of furniture
        if (property != HomeFurnitureController.Property.ICON
            && property != HomeFurnitureController.Property.PROPORTIONAL
            && property != HomeFurnitureController.Property.RESIZABLE
            && property != HomeFurnitureController.Property.DEFORMABLE
            && property != HomeFurnitureController.Property.TEXTURABLE) {
          this.controller.addPropertyChangeListener(property, createModificationListener(property));
        }
      }
      PropertyChangeListener paintModificationListener =
          createModificationListener(HomeFurnitureController.Property.PAINT);
      TextureChoiceController textureController = this.controller.getTextureController();
      if (textureController != null) {
        textureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE, paintModificationListener);
      }
      ModelMaterialsController modelMaterialsController = this.controller.getModelMaterialsController();
      if (modelMaterialsController != null) {
        modelMaterialsController.addPropertyChangeListener(ModelMaterialsController.Property.MATERIALS, paintModificationListener);
      }
    }
  }

  /**
   * The editor of selected rooms.
   */
  private class RoomsEditor extends Editor {
    private final RoomController controller = planController.createRoomController();

    @Override
    public Object getView() {
      return this.controller.getView();
    }

    @Override
    public void refresh() {
      this.controller.updateProperties();
    }

    @Override
    public void modify(Object mergeKey) {
      this.controller.modifyRooms(mergeKey);
    }

    @Override
    public void addModificationListeners() {
      for (RoomController.Property property : RoomController.Property.values()) {
        this.controller.addPropertyChangeListener(property, createModificationListener(property));
      }
      this.controller.getFloorTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          createModificationListener(RoomController.Property.FLOOR_PAINT));
      this.controller.getCeilingTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          createModificationListener(RoomController.Property.CEILING_PAINT));
      this.controller.getWallSidesTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          createModificationListener(RoomController.Property.WALL_SIDES_PAINT));
      addModificationListeners(this.controller.getWallSidesBaseboardController(),
          RoomController.Property.WALL_SIDES_BASEBOARD);
    }
  }

  /**
   * The editor of selected walls.
   */
  private class WallsEditor extends Editor {
    private final WallController controller = planController.createWallController();

    @Override
    public Object getView() {
      return this.controller.getView();
    }

    @Override
    public void refresh() {
      this.controller.updateProperties();
    }

    @Override
    public void modify(Object mergeKey) {
      this.controller.modifyWalls(mergeKey);
    }

    @Override
    public void addModificationListeners() {
      for (WallController.Property property : WallController.Property.values()) {
        // Ignore the property which doesn't describe a modification of walls
        if (property != WallController.Property.EDITABLE_POINTS) {
          this.controller.addPropertyChangeListener(property, createModificationListener(property));
        }
      }
      this.controller.getLeftSideTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          createModificationListener(WallController.Property.LEFT_SIDE_PAINT));
      this.controller.getRightSideTextureController().addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          createModificationListener(WallController.Property.RIGHT_SIDE_PAINT));
      addModificationListeners(this.controller.getLeftSideBaseboardController(), "leftSideBaseboard");
      addModificationListeners(this.controller.getRightSideBaseboardController(), "rightSideBaseboard");
    }
  }

  /**
   * The editor of the observer camera, which is modified without undoable edit.
   */
  private class ObserverCameraEditor extends Editor {
    private final ObserverCameraController controller = planController.createObserverCameraController();

    @Override
    public Object getView() {
      return this.controller.getView();
    }

    @Override
    public void refresh() {
      this.controller.updateProperties();
    }

    @Override
    public void modify(Object mergeKey) {
      this.controller.modifyObserverCamera();
    }

    @Override
    public void addModificationListeners() {
      for (ObserverCameraController.Property property : ObserverCameraController.Property.values()) {
        // Ignore the property which doesn't describe a modification of the camera
        if (property != ObserverCameraController.Property.MINIMUM_ELEVATION) {
          this.controller.addPropertyChangeListener(property, createModificationListener(property));
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
