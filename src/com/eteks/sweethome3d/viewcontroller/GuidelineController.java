/*
 * GuidelineController.java
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
package com.eteks.sweethome3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.model.Guideline;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A MVC controller for the view which edits the position and the color of guidelines.
 */
public class GuidelineController implements Controller {
  /**
   * The properties that may be edited by the view associated to this controller.
   */
  public enum Property {ORIENTATION, POSITION, COLOR}

  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  guidelineView;

  private Guideline.Orientation orientation;
  private Float                 position;
  private Integer               color;

  /**
   * Creates the controller of guideline view with undo support.
   */
  public GuidelineController(Home home,
                             UserPreferences preferences,
                             ViewFactory viewFactory,
                             UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);

    updateProperties();
  }

  /**
   * Returns the view associated with this controller.
   */
  public DialogView getView() {
    // Create view lazily only once it's needed
    if (this.guidelineView == null) {
      this.guidelineView = this.viewFactory.createGuidelineView(this.preferences, this);
    }
    return this.guidelineView;
  }

  /**
   * Displays the view controlled by this controller.
   */
  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  /**
   * Adds the property change <code>listener</code> in parameter to this controller.
   */
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  /**
   * Removes the property change <code>listener</code> in parameter from this controller.
   */
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  /**
   * Updates edited properties from selected guidelines in the home edited by this controller.
   */
  public void updateProperties() {
    List<Guideline> selectedGuidelines = Home.getGuidelinesSubList(this.home.getSelectedItems());
    if (selectedGuidelines.isEmpty()) {
      setOrientation(null);
      setPosition(null);
      setColor(null);
    } else {
      // Search the common properties among selected guidelines
      Guideline firstGuideline = selectedGuidelines.get(0);
      Guideline.Orientation orientation = firstGuideline.getOrientation();
      Float position = firstGuideline.getPosition();
      Integer color = firstGuideline.getColor();
      for (int i = 1; i < selectedGuidelines.size(); i++) {
        Guideline guideline = selectedGuidelines.get(i);
        if (orientation != guideline.getOrientation()) {
          orientation = null;
        }
        if (position != null
            && position != guideline.getPosition()) {
          position = null;
        }
        if (color != null
            && color != guideline.getColor()) {
          color = null;
        }
      }
      setOrientation(orientation);
      setPosition(position);
      setColor(color);
    }
  }

  /**
   * Sets the orientation of the edited guidelines, or <code>null</code> if they have different orientations.
   */
  private void setOrientation(Guideline.Orientation orientation) {
    if (orientation != this.orientation) {
      Guideline.Orientation oldOrientation = this.orientation;
      this.orientation = orientation;
      this.propertyChangeSupport.firePropertyChange(Property.ORIENTATION.name(), oldOrientation, orientation);
    }
  }

  /**
   * Returns the orientation of the edited guidelines, or <code>null</code> if they have different orientations.
   */
  public Guideline.Orientation getOrientation() {
    return this.orientation;
  }

  /**
   * Sets the edited position.
   */
  public void setPosition(Float position) {
    if (position != this.position
        && (position == null || !position.equals(this.position))) {
      Float oldPosition = this.position;
      this.position = position;
      this.propertyChangeSupport.firePropertyChange(Property.POSITION.name(), oldPosition, position);
    }
  }

  /**
   * Returns the edited position.
   */
  public Float getPosition() {
    return this.position;
  }

  /**
   * Sets the edited color.
   */
  public void setColor(Integer color) {
    if (color != this.color
        && (color == null || !color.equals(this.color))) {
      Integer oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }

  /**
   * Returns the edited color.
   */
  public Integer getColor() {
    return this.color;
  }

  /**
   * Controls the modification of selected guidelines.
   */
  public void modifyGuidelines() {
    modifyGuidelines(null);
  }

  /**
   * Controls the modification of selected guidelines, with an undoable edit
   * which will be merged with the previous one if that one was posted with an equal key.
   * @param mergeKey an object identifying the kind of modification,
   *            or <code>null</code> if the edit shouldn't be merged with an other one
   */
  public void modifyGuidelines(Object mergeKey) {
    List<Selectable> oldSelection = this.home.getSelectedItems();
    List<Guideline> selectedGuidelines = Home.getGuidelinesSubList(oldSelection);
    if (!selectedGuidelines.isEmpty()) {
      Float position = getPosition();
      Integer color = getColor();

      // Create an array of modified guidelines with their current properties values
      ModifiedGuideline [] modifiedGuidelines = new ModifiedGuideline [selectedGuidelines.size()];
      for (int i = 0; i < modifiedGuidelines.length; i++) {
        modifiedGuidelines [i] = new ModifiedGuideline(selectedGuidelines.get(i));
      }
      // Apply modification
      doModifyGuidelines(modifiedGuidelines, position, color);
      if (this.undoSupport != null) {
        GuidelinesModificationUndoableEdit undoableEdit = new GuidelinesModificationUndoableEdit(this.home,
            this.preferences, oldSelection.toArray(new Selectable [oldSelection.size()]),
            modifiedGuidelines, position, color);
        undoableEdit.setMergeKey(mergeKey);
        this.undoSupport.postEdit(undoableEdit);
      }
    }
  }

  /**
   * Undoable edit for guidelines modification. This class isn't anonymous to avoid
   * being bound to controller and its view.
   */
  private static class GuidelinesModificationUndoableEdit extends MergeableUndoableEdit {
    private final Home                 home;
    private final Selectable []        oldSelection;
    private final ModifiedGuideline [] modifiedGuidelines;
    private final Float                position;
    private final Integer              color;

    private GuidelinesModificationUndoableEdit(Home home,
                                               UserPreferences preferences,
                                               Selectable [] oldSelection,
                                               ModifiedGuideline [] modifiedGuidelines,
                                               Float position, Integer color) {
      super(preferences, GuidelineController.class, "undoModifyGuidelinesName");
      this.home = home;
      this.oldSelection = oldSelection;
      this.modifiedGuidelines = modifiedGuidelines;
      this.position = position;
      this.color = color;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      undoMergedModifications();
      undoModification();
      this.home.setSelectedItems(Arrays.asList(this.oldSelection));
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      redoModification();
      redoMergedModifications();
      this.home.setSelectedItems(Arrays.asList(this.oldSelection));
    }

    @Override
    protected void undoModification() {
      for (ModifiedGuideline modifiedGuideline : this.modifiedGuidelines) {
        modifiedGuideline.reset();
      }
    }

    @Override
    protected void redoModification() {
      doModifyGuidelines(this.modifiedGuidelines, this.position, this.color);
    }
  }

  /**
   * Modifies guidelines properties with the values in parameter.
   */
  private static void doModifyGuidelines(ModifiedGuideline [] modifiedGuidelines,
                                         Float position, Integer color) {
    for (ModifiedGuideline modifiedGuideline : modifiedGuidelines) {
      Guideline guideline = modifiedGuideline.getGuideline();
      if (position != null) {
        guideline.setPosition(position);
      }
      if (color != null) {
        guideline.setColor(color);
      }
    }
  }

  /**
   * Stores the current properties values of a modified guideline.
   */
  private static final class ModifiedGuideline {
    private final Guideline guideline;
    private final float     position;
    private final int       color;

    public ModifiedGuideline(Guideline guideline) {
      this.guideline = guideline;
      this.position = guideline.getPosition();
      this.color = guideline.getColor();
    }

    public Guideline getGuideline() {
      return this.guideline;
    }

    public void reset() {
      this.guideline.setPosition(this.position);
      this.guideline.setColor(this.color);
    }
  }
}
