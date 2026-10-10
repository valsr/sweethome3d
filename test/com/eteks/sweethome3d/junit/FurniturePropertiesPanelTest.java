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

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.undo.AbstractUndoableEdit;
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
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;

import junit.framework.TestCase;

/**
 * Tests the {@link com.eteks.sweethome3d.swing.FurniturePropertiesPanel panel}
 * which edits the properties of the selected furniture without dialog box,
 * and applies each edit immediately.
 */
public class FurniturePropertiesPanelTest extends TestCase {
  private Home                     home;
  private HomePieceOfFurniture     piece1;
  private HomePieceOfFurniture     piece2;
  private UserPreferences          preferences;
  private UndoableEditSupport      undoSupport;
  private UndoManager              undoManager;
  private FurniturePropertiesPanel panel;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    final UserPreferences preferences = this.preferences = new DefaultUserPreferences();
    this.home = new Home();
    PieceOfFurniture catalogPiece = preferences.getFurnitureCatalog().
        getCategories().get(0).getFurniture().get(0);
    this.piece1 = new HomePieceOfFurniture(catalogPiece);
    this.piece1.setName("Piece 1");
    this.home.addPieceOfFurniture(this.piece1);
    this.piece2 = new HomePieceOfFurniture(catalogPiece);
    this.piece2.setName("Piece 2");
    this.home.addPieceOfFurniture(this.piece2);

    UndoableEditSupport undoSupport = this.undoSupport = new UndoableEditSupport();
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

  /**
   * Runs the given edit in the Event Dispatch Thread, as the components of the panel do,
   * and waits for it to be applied.
   */
  private void edit(Runnable edit) throws Exception {
    EventQueue.invokeAndWait(edit);
    waitForUpdate();
  }

  private void editName(final String name) throws Exception {
    edit(new Runnable() {
        public void run() {
          panel.getFurnitureController().setName(name);
        }
      });
  }

  private void editX(final float x) throws Exception {
    edit(new Runnable() {
        public void run() {
          panel.getFurnitureController().setX(x);
        }
      });
  }

  private void waitForUpdate() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
        }
      });
  }

  public void testNothingIsEditedWithoutSelectedFurniture() throws Exception {
    assertNull(this.panel.getFurnitureController());
    assertTrue(SwingTools.findChildren(this.panel, HomeFurniturePanel.class).isEmpty());

    select(this.piece1);
    select();
    assertNull(this.panel.getFurnitureController());
    assertTrue(SwingTools.findChildren(this.panel, HomeFurniturePanel.class).isEmpty());
  }

  public void testSelectedFurnitureIsEditedWithoutApplyButton() throws Exception {
    select(this.piece1);

    assertEquals("Piece 1", this.panel.getFurnitureController().getName());
    assertEquals(1, SwingTools.findChildren(this.panel, HomeFurniturePanel.class).size());
    for (JButton button : SwingTools.findChildren(this.panel, JButton.class)) {
      assertFalse("Edits are applied without button", "Apply".equals(button.getText()));
    }
    assertFalse("Selecting furniture shouldn't be undoable", this.undoManager.canUndo());
  }

  public void testEditIsAppliedImmediatelyAndCanBeUndone() throws Exception {
    select(this.piece1);
    HomeFurnitureController controller = this.panel.getFurnitureController();

    editName("Renamed");
    assertEquals("Renamed", this.piece1.getName());
    assertSame("Panel shouldn't be rebuilt by its own edits", controller, this.panel.getFurnitureController());

    this.undoManager.undo();
    waitForUpdate();
    assertEquals("Piece 1", this.piece1.getName());
    assertSame("Panel should be updated in place", controller, this.panel.getFurnitureController());
    assertEquals("Piece 1", controller.getName());
    assertFalse(this.undoManager.canUndo());
  }

  public void testSuccessiveEditsOfSamePropertyAreUndoneAtOnce() throws Exception {
    select(this.piece1);
    // Simulate a name typed character by character
    for (String name : new String [] {"R", "Re", "Ren"}) {
      editName(name);
      assertEquals(name, this.piece1.getName());
    }

    this.undoManager.undo();
    assertEquals("Piece 1", this.piece1.getName());
    assertFalse(this.undoManager.canUndo());

    this.undoManager.redo();
    assertEquals("Ren", this.piece1.getName());
    assertFalse(this.undoManager.canRedo());
  }

  public void testEditsOfDifferentPropertiesAreUndoneSeparately() throws Exception {
    select(this.piece1);
    float x = this.piece1.getX();
    editName("Renamed");
    editX(x + 50);
    editName("Renamed again");

    this.undoManager.undo();
    assertEquals("Renamed", this.piece1.getName());
    assertEquals(x + 50, this.piece1.getX());
    this.undoManager.undo();
    assertEquals("Renamed", this.piece1.getName());
    assertEquals(x, this.piece1.getX());
    this.undoManager.undo();
    assertEquals("Piece 1", this.piece1.getName());
    assertFalse(this.undoManager.canUndo());
  }

  public void testEditsOfSamePropertyOnDifferentSelectionsAreUndoneSeparately() throws Exception {
    select(this.piece1);
    editName("Renamed 1");
    select(this.piece2);
    editName("Renamed 2");

    this.undoManager.undo();
    assertEquals("Renamed 1", this.piece1.getName());
    assertEquals("Piece 2", this.piece2.getName());
    this.undoManager.undo();
    assertEquals("Piece 1", this.piece1.getName());
  }

  public void testEditsOfSamePropertyAroundAnotherUndoableEditAreUndoneSeparately() throws Exception {
    select(this.piece1);
    editName("Renamed");
    // Simulate an edit made elsewhere, like a piece added to home
    this.undoSupport.postEdit(new AbstractUndoableEdit());
    editName("Renamed again");

    this.undoManager.undo();
    assertEquals("Renamed", this.piece1.getName());
  }

  public void testPropertiesChangedTogetherAreAppliedInOneEdit() throws Exception {
    select(this.piece1);
    float width = this.piece1.getWidth();
    float depth = this.piece1.getDepth();
    final HomeFurnitureController controller = this.panel.getFurnitureController();
    edit(new Runnable() {
        public void run() {
          controller.setProportional(true);
        }
      });
    assertFalse("Proportions choice isn't a furniture edit", this.undoManager.canUndo());

    // Depth is updated with width when proportions are kept
    final float newWidth = width * 2;
    edit(new Runnable() {
        public void run() {
          controller.setWidth(newWidth);
        }
      });
    assertEquals(width * 2, this.piece1.getWidth(), 0.001f);
    assertEquals(depth * 2, this.piece1.getDepth(), 0.001f);

    this.undoManager.undo();
    assertEquals(width, this.piece1.getWidth(), 0.001f);
    assertEquals(depth, this.piece1.getDepth(), 0.001f);
    assertFalse(this.undoManager.canUndo());
  }

  public void testEditKeepsDifferentValuesOfSelectedFurniture() throws Exception {
    select(this.piece1, this.piece2);
    assertNull(this.panel.getFurnitureController().getName());

    edit(new Runnable() {
        public void run() {
          panel.getFurnitureController().setElevation(30f);
        }
      });

    assertEquals(30f, this.piece1.getElevation());
    assertEquals(30f, this.piece2.getElevation());
    assertEquals("Piece 1", this.piece1.getName());
    assertEquals("Piece 2", this.piece2.getName());
  }

  public void testFurnitureChangedElsewhereIsUpdatedInPlaceWithoutEdit() throws Exception {
    select(this.piece1);
    HomeFurnitureController controller = this.panel.getFurnitureController();

    // Simulate a piece moved in the plan
    this.piece1.setX(this.piece1.getX() + 100);
    waitForUpdate();
    waitForUpdate();

    assertSame(controller, this.panel.getFurnitureController());
    assertEquals(this.piece1.getX(), controller.getX());
    assertFalse("Displaying a change made elsewhere shouldn't be undoable", this.undoManager.canUndo());
  }

  public void testTypedNamesAreNotProposedForAutoCompletion() throws Exception {
    select(this.piece1);
    editName("Ren");

    assertFalse(this.preferences.getAutoCompletionStrings("HomePieceOfFurnitureName").contains("Ren"));
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

  public void testTypingInNameFieldAppliesNameAndKeepsCaret() throws Exception {
    select(this.piece1);
    final JTextField nameTextField = getNameTextField();
    assertEquals("Piece 1", nameTextField.getText());

    // Type a character in the middle of the name
    edit(new Runnable() {
        public void run() {
          try {
            nameTextField.setCaretPosition(5);
            nameTextField.getDocument().insertString(5, "s", null);
          } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
          }
        }
      });
    waitForUpdate();

    assertEquals("Pieces 1", this.piece1.getName());
    assertSame("Edited component shouldn't be replaced", nameTextField, getNameTextField());
    assertEquals("Pieces 1", nameTextField.getText());
    assertEquals("Caret should stay after typed character", 6, nameTextField.getCaretPosition());
  }

  private JTextField getNameTextField() {
    for (JTextField textField : SwingTools.findChildren(this.panel, JTextField.class)) {
      if (this.piece1.getName().equals(textField.getText())) {
        return textField;
      }
    }
    fail("No text field displaying the name of the piece");
    return null;
  }
}
