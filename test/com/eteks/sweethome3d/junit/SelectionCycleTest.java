/*
 * SelectionCycleTest.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.undo.UndoableEditSupport;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.PlanController;

import junit.framework.TestCase;

/**
 * Tests the selection cycle among the items under cursor
 * when the user clicks again on a selected item in the plan.
 */
public class SelectionCycleTest extends TestCase {
  private Home                 home;
  private PlanController       planController;
  private HomePieceOfFurniture piece;
  private Wall                 wall;
  private Room                 room;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    this.home = new Home();
    this.room = new Room(new float [][] {{0, 0}, {400, 0}, {400, 300}, {0, 300}});
    this.home.addRoom(this.room);
    this.wall = new Wall(0, 0, 400, 0, 20, 250);
    this.home.addWall(this.wall);
    this.piece = new HomePieceOfFurniture(
        preferences.getFurnitureCatalog().getCategories().get(0).getFurniture().get(0));
    this.piece.setX(100);
    this.piece.setY(5);
    this.home.addPieceOfFurniture(this.piece);
    this.planController = new PlanController(this.home, preferences,
        new SwingViewFactory(), new FileContentManager(preferences), new UndoableEditSupport());
  }

  private void click(float x, float y) {
    this.planController.moveMouse(x, y);
    this.planController.pressMouse(x, y, 1, false, false);
    this.planController.releaseMouse(x, y);
  }

  private void assertSelection(Selectable item) {
    assertEquals(Arrays.asList(new Selectable [] {item}), this.home.getSelectedItems());
  }

  public void testItemsUnderCursorAreCycledAtEachClick() {
    click(100, 5);
    assertSelection(this.piece);
    click(100, 5);
    assertSelection(this.wall);
    click(100, 5);
    assertSelection(this.room);
    click(100, 5);
    assertSelection(this.piece);
  }

  public void testFirstClickSelectsTopItem() {
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {this.wall}));
    // Click where the wall isn't under cursor
    click(100, 200);
    assertSelection(this.room);
    click(100, 5);
    assertSelection(this.piece);
  }

  public void testClickOnMultipleSelectionSelectsTopItem() {
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {this.wall, this.room}));
    click(100, 5);
    assertSelection(this.piece);
  }

  public void testDoubleClickModifiesItemSelectedBeforeCycle() {
    final List<List<Selectable>> modifiedSelections = new ArrayList<List<Selectable>>();
    Runnable modifier = new Runnable() {
        public void run() {
          modifiedSelections.add(home.getSelectedItems());
        }
      };
    this.planController.setSelectedWallsModifier(modifier);
    this.planController.setSelectedRoomsModifier(modifier);
    click(100, 5);
    click(100, 5);
    assertSelection(this.wall);

    // Double click on the selected wall
    click(100, 5);
    this.planController.pressMouse(100, 5, 2, false, false);
    this.planController.releaseMouse(100, 5);
    assertSelection(this.wall);
    assertEquals(Arrays.asList(Arrays.asList(new Selectable [] {this.wall})), modifiedSelections);

    // Next click cycles again
    click(100, 5);
    assertSelection(this.room);
  }
}
