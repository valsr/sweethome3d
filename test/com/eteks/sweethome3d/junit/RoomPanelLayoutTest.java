/*
 * RoomPanelLayoutTest.java
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.RoomPanel;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.RoomController;

import junit.framework.TestCase;

/**
 * Tests the layout of {@link com.eteks.sweethome3d.swing.RoomPanel room panel},
 * which should fit in a narrow side bar.
 */
public class RoomPanelLayoutTest extends TestCase {
  private RoomPanel panel;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    Home home = new Home();
    Room room = new Room(new float [][] {{0, 0}, {400, 0}, {400, 300}, {0, 300}});
    home.addRoom(room);
    // Add a wall along the room to edit its sides
    home.addWall(new Wall(0, 0, 400, 0, 10, 250));
    home.setSelectedItems(Arrays.asList(new Selectable [] {room}));
    RoomController controller = new RoomController(home, preferences,
        new SwingViewFactory(), new FileContentManager(preferences), null);
    this.panel = (RoomPanel)controller.getView();
    this.panel.setSize(this.panel.getPreferredSize());
    layout(this.panel);
  }

  private void layout(Container container) {
    container.doLayout();
    for (Component child : container.getComponents()) {
      if (child instanceof Container) {
        layout((Container)child);
      }
    }
  }

  /**
   * Returns the bounds in the panel of the buttons with the given text, in the order they are found.
   */
  private List<java.awt.Rectangle> getBounds(String text) {
    List<java.awt.Rectangle> bounds = new ArrayList<java.awt.Rectangle>();
    for (AbstractButton button : SwingTools.findChildren(this.panel, AbstractButton.class)) {
      if (text.equals(button.getText())) {
        bounds.add(SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), this.panel));
      }
    }
    assertFalse("No button " + text, bounds.isEmpty());
    return bounds;
  }

  public void testPanelIsNarrowEnoughForSideBar() {
    int maxWidth = Math.round(400 * SwingTools.getResolutionScale());
    assertTrue("Panel too wide: " + this.panel.getPreferredSize().width,
        this.panel.getPreferredSize().width <= maxWidth);
  }

  public void testFloorCeilingWallSidesAndBaseboardAreDisplayedInRows() {
    int floorY = getBounds("Display floor").get(0).y;
    int ceilingY = getBounds("Display ceiling").get(0).y;
    int wallSidesY = getBounds("Recompute walls").get(0).y;
    int baseboardY = getBounds("Add baseboard").get(0).y;
    assertTrue(floorY < ceilingY);
    assertTrue(ceilingY < wallSidesY);
    assertTrue(wallSidesY < baseboardY);
  }

  public void testColorAndTextureAreDisplayedInTheSameRow() {
    // Floor, ceiling, wall sides and baseboard have each a color and a texture
    List<java.awt.Rectangle> colorBounds = getBounds("Color:");
    List<java.awt.Rectangle> textureBounds = getBounds("Texture:");
    assertEquals(4, colorBounds.size());
    for (int i = 0; i < 4; i++) {
      assertEquals(colorBounds.get(i).y, textureBounds.get(i).y);
      assertTrue(colorBounds.get(i).x < textureBounds.get(i).x);
    }
  }

  public void testCeilingOptionsAreDisplayedInTheSameRow() {
    assertEquals(getBounds("Display ceiling").get(0).y, getBounds("Flat ceiling only").get(0).y);
  }

  public void testShininessIsDisplayedInOneRow() {
    List<java.awt.Rectangle> mattBounds = getBounds("Matt");
    List<java.awt.Rectangle> shinyBounds = getBounds("Shiny");
    assertEquals(3, mattBounds.size());
    for (int i = 0; i < 3; i++) {
      assertEquals(mattBounds.get(i).y, shinyBounds.get(i).y);
    }
  }

  public void testBaseboardIsDisplayedInThreeRows() {
    assertEquals(getBounds("Add baseboard").get(0).y, getBounds("Wall side color/texture").get(0).y);
    List<JSpinner> spinners = SwingTools.findChildren(this.panel, JSpinner.class);
    assertEquals(2, spinners.size());
    assertEquals("Height and thickness should be in the same row", spinners.get(0).getY(), spinners.get(1).getY());
    assertTrue(spinners.get(0).getX() < spinners.get(1).getX());
  }
}
