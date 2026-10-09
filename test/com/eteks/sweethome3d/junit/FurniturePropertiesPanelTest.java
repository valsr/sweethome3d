/*
 * FurniturePropertiesPanelTest.java
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
package com.eteks.sweethome3d.junit;

import java.awt.EventQueue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.PieceOfFurniture;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.FurniturePropertiesPanel;
import com.eteks.sweethome3d.swing.HomeFurniturePanel;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;

import junit.framework.TestCase;

/**
 * Tests the {@link com.eteks.sweethome3d.swing.FurniturePropertiesPanel panel}
 * which edits the properties of the selected furniture without dialog box.
 */
public class FurniturePropertiesPanelTest extends TestCase {
  private Home                     home;
  private HomePieceOfFurniture     piece1;
  private HomePieceOfFurniture     piece2;
  private UndoManager              undoManager;
  private FurniturePropertiesPanel panel;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    this.home = new Home();
    PieceOfFurniture catalogPiece = preferences.getFurnitureCatalog().
        getCategories().get(0).getFurniture().get(0);
    this.piece1 = new HomePieceOfFurniture(catalogPiece);
    this.piece1.setName("Piece 1");
    this.home.addPieceOfFurniture(this.piece1);
    this.piece2 = new HomePieceOfFurniture(catalogPiece);
    this.piece2.setName("Piece 2");
    this.home.addPieceOfFurniture(this.piece2);

    UndoableEditSupport undoSupport = new UndoableEditSupport();
    this.undoManager = new UndoManager();
    undoSupport.addUndoableEditListener(this.undoManager);
    FurnitureController furnitureController = new FurnitureController(this.home, preferences,
        new SwingViewFactory(), new FileContentManager(preferences), undoSupport);
    this.panel = new FurniturePropertiesPanel(this.home, preferences, furnitureController);
  }

  /**
   * Selects the given items and waits for the panel to be updated.
   */
  private void select(Selectable... items) throws Exception {
    this.home.setSelectedItems(Arrays.asList(items));
    waitForUpdate();
  }

  private void waitForUpdate() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
        }
      });
  }

  public void testNothingIsEditedWithoutSelectedFurniture() throws Exception {
    assertNull(this.panel.getFurnitureController());
    assertFalse(this.panel.getApplyButton().isEnabled());
    assertFalse(this.panel.getResetButton().isEnabled());
    assertTrue(SwingTools.findChildren(this.panel, HomeFurniturePanel.class).isEmpty());

    select(this.piece1);
    select();
    assertNull(this.panel.getFurnitureController());
    assertFalse(this.panel.getApplyButton().isEnabled());
    assertTrue(SwingTools.findChildren(this.panel, HomeFurniturePanel.class).isEmpty());
  }

  public void testSelectedFurnitureIsEdited() throws Exception {
    select(this.piece1);

    assertEquals("Piece 1", this.panel.getFurnitureController().getName());
    assertTrue(this.panel.getApplyButton().isEnabled());
    assertTrue(this.panel.getResetButton().isEnabled());
    assertEquals(1, SwingTools.findChildren(this.panel, HomeFurniturePanel.class).size());
  }

  public void testApplyButtonModifiesSelectedFurnitureInOneUndoableEdit() throws Exception {
    select(this.piece1);
    float x = this.piece1.getX();
    this.panel.getFurnitureController().setName("Renamed");
    this.panel.getFurnitureController().setX(x + 50);
    assertEquals("Furniture shouldn't change before apply", "Piece 1", this.piece1.getName());

    this.panel.getApplyButton().doClick();
    waitForUpdate();

    assertEquals("Renamed", this.piece1.getName());
    assertEquals(x + 50, this.piece1.getX());
    assertEquals("Renamed", this.panel.getFurnitureController().getName());
    assertFalse(this.panel.isModified());

    this.undoManager.undo();
    waitForUpdate();
    assertEquals("Piece 1", this.piece1.getName());
    assertEquals(x, this.piece1.getX());
    assertFalse(this.undoManager.canUndo());
    assertEquals("Panel should show undone values", "Piece 1", this.panel.getFurnitureController().getName());
  }

  public void testResetButtonDiscardsEdits() throws Exception {
    select(this.piece1);
    this.panel.getFurnitureController().setName("Renamed");
    assertTrue(this.panel.isModified());

    this.panel.getResetButton().doClick();
    waitForUpdate();

    assertEquals("Piece 1", this.panel.getFurnitureController().getName());
    assertEquals("Piece 1", this.piece1.getName());
    assertFalse(this.panel.isModified());
    assertFalse(this.undoManager.canUndo());
  }

  public void testSelectionChangeDiscardsEditsAndEditsNewSelection() throws Exception {
    select(this.piece1);
    this.panel.getFurnitureController().setName("Renamed");

    select(this.piece2);

    assertEquals("Piece 2", this.panel.getFurnitureController().getName());
    assertFalse(this.panel.isModified());
    this.panel.getApplyButton().doClick();
    assertEquals("Piece 1", this.piece1.getName());
    assertEquals("Piece 2", this.piece2.getName());
  }

  public void testFurnitureChangedElsewhereIsUpdatedWhenNoEditIsPending() throws Exception {
    select(this.piece1);

    // Simulate a piece moved in the plan
    this.piece1.setX(this.piece1.getX() + 100);
    waitForUpdate();

    assertEquals(this.piece1.getX(), this.panel.getFurnitureController().getX());
  }

  public void testPendingEditsAreKeptWhenFurnitureChangesElsewhere() throws Exception {
    select(this.piece1);
    this.panel.getFurnitureController().setName("Renamed");

    this.piece1.setX(this.piece1.getX() + 100);
    waitForUpdate();

    assertEquals("Renamed", this.panel.getFurnitureController().getName());
    assertTrue(this.panel.isModified());
  }

  public void testInactivePanelIsUpdatedOnlyOnceActive() throws Exception {
    this.panel.setActive(false);
    select(this.piece1);
    assertNull(this.panel.getFurnitureController());
    assertTrue(SwingTools.findChildren(this.panel, HomeFurniturePanel.class).isEmpty());

    this.panel.setActive(true);
    waitForUpdate();
    assertEquals("Piece 1", this.panel.getFurnitureController().getName());
  }

  public void testSuccessiveSelectionChangesUpdatePanelOnce() throws Exception {
    select(this.piece1);
    Object controller = this.panel.getFurnitureController();

    // Change selection twice before the panel had a chance to be updated
    this.home.setSelectedItems(Collections.<Selectable>emptyList());
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {this.piece2}));
    assertSame("Panel should be updated later", controller, this.panel.getFurnitureController());
    waitForUpdate();

    assertEquals("Piece 2", this.panel.getFurnitureController().getName());
  }
}
