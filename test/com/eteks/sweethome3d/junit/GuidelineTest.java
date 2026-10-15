/*
 * GuidelineTest.java
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

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.model.Guideline;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.GuidelinePanel;
import com.eteks.sweethome3d.swing.SelectionPropertiesPanel;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.GuidelineController;
import com.eteks.sweethome3d.viewcontroller.PlanController;

import junit.framework.TestCase;

/**
 * Tests {@link Guideline guidelines} displayed across the plan at all levels.
 */
public class GuidelineTest extends TestCase {
  private UserPreferences preferences;
  private Home            home;
  private UndoManager     undoManager;
  private PlanController  planController;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    this.preferences = new DefaultUserPreferences();
    this.preferences.setMagnetismEnabled(false);
    this.home = new Home();
    UndoableEditSupport undoSupport = new UndoableEditSupport();
    this.undoManager = new UndoManager();
    undoSupport.addUndoableEditListener(this.undoManager);
    this.planController = new PlanController(this.home, this.preferences,
        new SwingViewFactory(), new FileContentManager(this.preferences), undoSupport);
    // Lay out plan view to give it a visible area
    JComponent planView = (JComponent)this.planController.getView();
    planView.setSize(400, 300);
    layout(planView);
  }

  private void layout(Container container) {
    container.doLayout();
    for (Component child : container.getComponents()) {
      if (child instanceof Container) {
        layout((Container)child);
      }
    }
  }

  private Guideline addGuideline(Guideline.Orientation orientation, float position) {
    Guideline guideline = new Guideline(orientation, position);
    this.home.addGuideline(guideline);
    return guideline;
  }

  /**
   * Returns a point displayed in the plan.
   */
  private float [] getVisiblePoint() {
    return new float [] {this.planController.getView().convertXPixelToModel(200),
                         this.planController.getView().convertYPixelToModel(150)};
  }

  public void testGuidelineGeometry() {
    Guideline horizontal = new Guideline(Guideline.Orientation.HORIZONTAL, 100);
    assertTrue(horizontal.containsPoint(-5000, 102, 3));
    assertFalse(horizontal.containsPoint(100, 110, 3));
    assertTrue(horizontal.intersectsRectangle(0, 50, 10, 150));
    assertFalse(horizontal.intersectsRectangle(0, 150, 10, 200));
    horizontal.move(30, 20);
    assertEquals(120f, horizontal.getPosition());

    Guideline vertical = new Guideline(Guideline.Orientation.VERTICAL, 50);
    vertical.move(30, 20);
    assertEquals(80f, vertical.getPosition());
    assertEquals(Guideline.DEFAULT_COLOR, vertical.getColor());
    Guideline clone = vertical.clone();
    assertNotSame(vertical, clone);
    assertEquals(80f, clone.getPosition());
  }

  public void testGuidelinesAreRecorded() throws Exception {
    Guideline horizontal = addGuideline(Guideline.Orientation.HORIZONTAL, 123.5f);
    horizontal.setColor(0xFF0000);
    addGuideline(Guideline.Orientation.VERTICAL, -40);
    for (boolean preferXmlEntry : new boolean [] {false, true}) {
      File homeFile = File.createTempFile("guidelines", ".sh3d");
      try {
        HomeFileRecorder recorder = new HomeFileRecorder(0, false, this.preferences, false, preferXmlEntry);
        recorder.writeHome(this.home, homeFile.getAbsolutePath());
        List<Guideline> readGuidelines = recorder.readHome(homeFile.getAbsolutePath()).getGuidelines();
        assertEquals("Entry XML " + preferXmlEntry, 2, readGuidelines.size());
        assertEquals(Guideline.Orientation.HORIZONTAL, readGuidelines.get(0).getOrientation());
        assertEquals(123.5f, readGuidelines.get(0).getPosition());
        assertEquals(0xFF0000, readGuidelines.get(0).getColor() & 0xFFFFFF);
        assertEquals(Guideline.Orientation.VERTICAL, readGuidelines.get(1).getOrientation());
        assertEquals(-40f, readGuidelines.get(1).getPosition());
        assertEquals(Guideline.DEFAULT_COLOR, readGuidelines.get(1).getColor() & 0xFFFFFF);
      } finally {
        homeFile.delete();
      }
    }
  }

  public void testGuidelineDraggedFromRulerIsCreatedAndUndone() {
    float [] point = getVisiblePoint();
    this.planController.startGuidelineCreation(Guideline.Orientation.HORIZONTAL);
    assertTrue(this.planController.isModificationState());
    this.planController.moveMouse(point [0], point [1]);
    this.planController.releaseMouse(point [0], point [1]);

    assertEquals(1, this.home.getGuidelines().size());
    Guideline guideline = this.home.getGuidelines().get(0);
    assertEquals(point [1], guideline.getPosition());
    assertEquals(Arrays.asList(new Selectable [] {guideline}), this.home.getSelectedItems());
    assertEquals(PlanController.Mode.SELECTION, this.planController.getMode());
    assertFalse(this.planController.isModificationState());

    this.undoManager.undo();
    assertTrue(this.home.getGuidelines().isEmpty());
    this.undoManager.redo();
    assertEquals(Arrays.asList(guideline), this.home.getGuidelines());
  }

  public void testGuidelineDroppedOnRulerIsntCreated() {
    float [] point = getVisiblePoint();
    this.planController.startGuidelineCreation(Guideline.Orientation.VERTICAL);
    this.planController.moveMouse(point [0], point [1]);
    assertEquals("Guideline should be displayed while dragged", 1, this.home.getGuidelines().size());
    // Move back to the ruler out of the plan
    this.planController.moveMouse(-100000, -100000);
    assertTrue(this.home.getGuidelines().isEmpty());
    this.planController.releaseMouse(-100000, -100000);
    assertTrue(this.home.getGuidelines().isEmpty());
    assertFalse(this.undoManager.canUndo());
  }

  public void testGuidelineCreationIsIgnoredWhileDrawing() {
    this.planController.setMode(PlanController.Mode.WALL_CREATION);
    this.planController.moveMouse(0, 0);
    this.planController.pressMouse(0, 0, 1, false, false);
    this.planController.releaseMouse(0, 0);
    this.planController.moveMouse(100, 0);
    // A wall is being drawn
    this.planController.startGuidelineCreation(Guideline.Orientation.HORIZONTAL);
    this.planController.moveMouse(200, 0);
    assertTrue(this.home.getGuidelines().isEmpty());
    this.planController.escape();
  }

  public void testGuidelineCreatedInWallCreationModeKeepsMode() {
    this.planController.setMode(PlanController.Mode.WALL_CREATION);
    float [] point = getVisiblePoint();
    this.planController.startGuidelineCreation(Guideline.Orientation.HORIZONTAL);
    assertEquals(PlanController.Mode.WALL_CREATION, this.planController.getMode());
    this.planController.moveMouse(point [0], point [1]);
    this.planController.releaseMouse(point [0], point [1]);
    assertEquals(1, this.home.getGuidelines().size());
    assertEquals(PlanController.Mode.WALL_CREATION, this.planController.getMode());
    assertTrue(this.home.getWalls().isEmpty());
  }

  public void testGuidelineIsMovedWithMouseAndUndone() {
    Guideline guideline = addGuideline(Guideline.Orientation.VERTICAL, 100);
    this.planController.moveMouse(101, 50);
    this.planController.pressMouse(101, 50, 1, false, false);
    assertEquals(Arrays.asList(new Selectable [] {guideline}), this.home.getSelectedItems());
    this.planController.moveMouse(151, 80);
    this.planController.releaseMouse(151, 80);
    assertEquals(151f, guideline.getPosition());

    this.undoManager.undo();
    assertEquals(100f, guideline.getPosition());
    this.undoManager.redo();
    assertEquals(151f, guideline.getPosition());
  }

  public void testGuidelineClickedWithoutMoveIsOnlySelected() {
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.planController.pressMouse(50, 101, 1, false, false);
    this.planController.releaseMouse(50, 101);
    assertEquals(Arrays.asList(new Selectable [] {guideline}), this.home.getSelectedItems());
    assertEquals(100f, guideline.getPosition());
    assertFalse(this.undoManager.canUndo());
  }

  public void testGuidelineDroppedOnRulerIsDeletedAndUndone() {
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.planController.pressMouse(50, 100, 1, false, false);
    this.planController.moveMouse(50, 120);
    this.planController.moveMouse(-100000, -100000);
    this.planController.releaseMouse(-100000, -100000);
    assertTrue(this.home.getGuidelines().isEmpty());

    this.undoManager.undo();
    assertEquals(Arrays.asList(guideline), this.home.getGuidelines());
    assertEquals("Position before move should be restored", 100f, guideline.getPosition());
    this.undoManager.redo();
    assertTrue(this.home.getGuidelines().isEmpty());
  }

  public void testEscapeRestoresDraggedGuideline() {
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.planController.pressMouse(50, 100, 1, false, false);
    this.planController.moveMouse(50, -100000);
    assertTrue(this.home.getGuidelines().isEmpty());
    this.planController.escape();
    assertEquals(Arrays.asList(guideline), this.home.getGuidelines());
    assertEquals(100f, guideline.getPosition());
    assertFalse(this.undoManager.canUndo());
  }

  public void testItemsHavePriorityOverGuidelines() {
    Wall wall = new Wall(0, 100, 400, 100, 10, 250);
    this.home.addWall(wall);
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    assertSame(wall, this.planController.getSelectableItemAt(200, 100));
    assertSame(guideline, this.planController.getSelectableItemAt(600, 100));
    assertTrue(this.planController.getSelectableItemsAt(200, 100).contains(guideline));
  }

  public void testGuidelinesAreSelectableAtAllLevels() {
    Level level0 = new Level("0", 0, 12, 250);
    Level level1 = new Level("1", 250, 12, 250);
    this.home.addLevel(level0);
    this.home.addLevel(level1);
    this.home.setSelectedLevel(level0);
    Guideline guideline = addGuideline(Guideline.Orientation.VERTICAL, 100);
    this.home.setSelectedLevel(level1);
    assertSame(guideline, this.planController.getSelectableItemAt(100, 500));
  }

  public void testGuidelinesArentSelectedWithOtherItems() {
    this.home.addWall(new Wall(0, 0, 400, 0, 10, 250));
    addGuideline(Guideline.Orientation.HORIZONTAL, 0);
    this.planController.selectAll();
    assertTrue(Home.getGuidelinesSubList(this.home.getSelectedItems()).isEmpty());
  }

  public void testSelectedGuidelineIsDeletedAndUndone() {
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {guideline}));
    this.planController.deleteSelection();
    assertTrue(this.home.getGuidelines().isEmpty());
    this.undoManager.undo();
    assertEquals(Arrays.asList(guideline), this.home.getGuidelines());
  }

  public void testGuidelinesArentPartOfBasePlan() {
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.home.setBasePlanLocked(true);
    assertSame(guideline, this.planController.getSelectableItemAt(50, 100));
  }

  public void testMovedGuidelineIsMagnetizedToItemPoints() {
    this.preferences.setMagnetismEnabled(true);
    this.home.addWall(new Wall(0, 0, 400, 0, 10, 250));
    Guideline guideline = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    this.planController.pressMouse(50, 100, 1, false, false);
    // Move close to the bottom side of the wall at y = 5
    this.planController.moveMouse(50, 6);
    this.planController.releaseMouse(50, 6);
    assertEquals(5f, guideline.getPosition());
  }

  public void testDrawnWallIsMagnetizedToGuideline() {
    this.preferences.setMagnetismEnabled(true);
    addGuideline(Guideline.Orientation.VERTICAL, 300);
    this.planController.setMode(PlanController.Mode.WALL_CREATION);
    this.planController.moveMouse(0, 0);
    this.planController.pressMouse(0, 0, 1, false, false);
    this.planController.releaseMouse(0, 0);
    this.planController.moveMouse(304, 2);
    this.planController.pressMouse(304, 2, 2, false, false);
    this.planController.releaseMouse(304, 2);
    Wall wall = this.home.getWalls().iterator().next();
    assertEquals(300f, wall.getXEnd());
    assertEquals(0f, wall.getYEnd());
  }

  public void testMovedItemsAreMagnetizedToGuideline() {
    this.preferences.setMagnetismEnabled(true);
    Wall wall = new Wall(0, 0, 0, 400, 10, 250);
    this.home.addWall(wall);
    addGuideline(Guideline.Orientation.VERTICAL, 200);
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {wall}));
    // Press out of the arc extent indicator in the middle of the wall
    this.planController.pressMouse(0, 100, 1, false, false);
    // Move wall left side close to the guideline
    this.planController.moveMouse(207, 100);
    this.planController.releaseMouse(207, 100);
    assertEquals("Wall left side should be on guideline", 205f, wall.getXStart());

    this.planController.pressMouse(205, 100, 1, false, false);
    // Move far from the guideline
    this.planController.moveMouse(255, 100);
    this.planController.releaseMouse(255, 100);
    assertEquals(255f, wall.getXStart());
  }

  public void testRulersArePaintedWithGuidelinesOfBothOrientations() {
    Guideline horizontal = addGuideline(Guideline.Orientation.HORIZONTAL, 100);
    horizontal.setColor(0xFF0000);
    Guideline vertical = addGuideline(Guideline.Orientation.VERTICAL, 100);
    vertical.setColor(0x0000FF);
    for (JComponent ruler : new JComponent [] {(JComponent)this.planController.getHorizontalRulerView(),
                                               (JComponent)this.planController.getVerticalRulerView()}) {
      ruler.setSize(ruler.getPreferredSize());
      BufferedImage image = new BufferedImage(ruler.getWidth(), ruler.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g2D = image.createGraphics();
      ruler.paint(g2D);
      g2D.dispose();
    }
  }

  public void testGuidelineIsEditedInPropertiesPanel() throws Exception {
    final Guideline guideline = addGuideline(Guideline.Orientation.VERTICAL, 100);
    final SelectionPropertiesPanel panel = new SelectionPropertiesPanel(
        this.home, this.preferences, this.planController, this.planController);
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {guideline}));
    waitForUpdate();
    GuidelineController controller = panel.getGuidelineController();
    assertNotNull(controller);
    assertEquals(Guideline.Orientation.VERTICAL, controller.getOrientation());
    assertEquals(1, SwingTools.findChildren(panel, GuidelinePanel.class).size());

    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          panel.getGuidelineController().setPosition(150f);
          panel.getGuidelineController().setColor(0x00FF00);
        }
      });
    waitForUpdate();
    assertEquals(150f, guideline.getPosition());
    assertEquals(0x00FF00, guideline.getColor());

    this.undoManager.undo();
    assertEquals(100f, guideline.getPosition());
    assertEquals(Guideline.DEFAULT_COLOR, guideline.getColor());
  }

  public void testModifyGuidelineRevealsPropertiesPanelModifier() {
    final boolean [] modifierRun = {false};
    this.planController.setSelectedGuidelinesModifier(new Runnable() {
        public void run() {
          modifierRun [0] = true;
        }
      });
    Guideline guideline = addGuideline(Guideline.Orientation.VERTICAL, 100);
    this.planController.pressMouse(100, 50, 1, false, false);
    this.planController.releaseMouse(100, 50);
    this.planController.pressMouse(100, 50, 2, false, false);
    this.planController.releaseMouse(100, 50);
    assertEquals(Collections.singletonList(guideline), this.home.getSelectedItems());
    assertTrue(modifierRun [0]);
  }

  private void waitForUpdate() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
        }
      });
  }
}
