/*
 * MergeableUndoableEdit.java
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.UndoableEdit;

import com.eteks.sweethome3d.model.UserPreferences;

/**
 * An undoable edit which merges the edits of the same class posted after it with an equal key.
 * This lets a view apply each change of the user immediately, and the user undo the successive
 * changes of the same kind at once.
 */
abstract class MergeableUndoableEdit extends LocalizedUndoableEdit {
  private Object                      mergeKey;
  private List<MergeableUndoableEdit> mergedEdits;

  public MergeableUndoableEdit(UserPreferences preferences,
                               Class<? extends Controller>  controllerClass,
                               String presentationNameKey) {
    super(preferences, controllerClass, presentationNameKey);
  }

  /**
   * Sets the key which identifies the edits this edit may be merged with,
   * or <code>null</code> if it shouldn't be merged.
   */
  public void setMergeKey(Object mergeKey) {
    this.mergeKey = mergeKey;
  }

  /**
   * Merges the given edit with this one if they were posted with the same key.
   */
  @Override
  public boolean addEdit(UndoableEdit edit) {
    if (this.mergeKey != null
        && edit.getClass() == getClass()
        && this.mergeKey.equals(((MergeableUndoableEdit)edit).mergeKey)) {
      if (this.mergedEdits == null) {
        this.mergedEdits = new ArrayList<MergeableUndoableEdit>();
      }
      this.mergedEdits.add((MergeableUndoableEdit)edit);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Undoes the modifications of the merged edits from the last to the first.
   * Should be called before undoing the modification of this edit.
   */
  protected void undoMergedModifications() {
    if (this.mergedEdits != null) {
      for (int i = this.mergedEdits.size() - 1; i >= 0; i--) {
        this.mergedEdits.get(i).undoModification();
      }
    }
  }

  /**
   * Redoes the modifications of the merged edits from the first to the last.
   * Should be called after redoing the modification of this edit.
   */
  protected void redoMergedModifications() {
    if (this.mergedEdits != null) {
      for (MergeableUndoableEdit mergedEdit : this.mergedEdits) {
        mergedEdit.redoModification();
      }
    }
  }

  /**
   * Returns the last edit merged with this edit, or this edit if none was merged.
   */
  protected MergeableUndoableEdit getLastEdit() {
    return this.mergedEdits != null
        ? this.mergedEdits.get(this.mergedEdits.size() - 1)
        : this;
  }

  /**
   * Undoes the modification of this edit without taking into account merged edits and selection.
   */
  protected abstract void undoModification();

  /**
   * Redoes the modification of this edit without taking into account merged edits and selection.
   */
  protected abstract void redoModification();
}
