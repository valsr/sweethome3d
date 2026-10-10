/*
 * WallPanelLayoutTest.java
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
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.swing.WallPanel;
import com.eteks.sweethome3d.viewcontroller.WallController;

import junit.framework.TestCase;

/**
 * Tests the layout of {@link com.eteks.sweethome3d.swing.RoomPanel wall panel},
 * which should fit in a narrow side bar.
 */
public class WallPanelLayoutTest extends TestCase {
  private WallPanel panel;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    Home home = new Home();
    Wall wall = new Wall(0, 0, 400, 0, 10, 250);
    home.addWall(wall);
    home.setSelectedItems(Arrays.asList(new Selectable [] {wall}));
    WallController controller = new WallController(home, preferences,
        new SwingViewFactory(), new FileContentManager(preferences), null);
    this.panel = (WallPanel)controller.getView();
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

  public void testLeftAndRightSidesAreDisplayedOneUnderTheOther() {
    // Left side, right side and top have each a color
    List<java.awt.Rectangle> colorBounds = getBounds("Color:");
    List<java.awt.Rectangle> textureBounds = getBounds("Texture:");
    assertEquals(3, colorBounds.size());
    assertEquals(2, textureBounds.size());
    assertTrue("Right side should be under left side", colorBounds.get(0).y < colorBounds.get(1).y);
    assertEquals(colorBounds.get(0).x, colorBounds.get(1).x);
    for (int i = 0; i < 2; i++) {
      assertEquals(colorBounds.get(i).y, textureBounds.get(i).y);
      assertTrue(colorBounds.get(i).x < textureBounds.get(i).x);
    }
  }

  public void testShininessAndBaseboardAreDisplayedInTheSameRow() {
    List<java.awt.Rectangle> mattBounds = getBounds("Matt");
    List<java.awt.Rectangle> shinyBounds = getBounds("Shiny");
    List<java.awt.Rectangle> baseboardBounds = getBounds("Modify baseboard...");
    assertEquals(2, baseboardBounds.size());
    for (int i = 0; i < 2; i++) {
      assertEquals(mattBounds.get(i).y, shinyBounds.get(i).y);
      // Button is higher than radio buttons
      assertTrue(baseboardBounds.get(i).y <= mattBounds.get(i).y);
      assertTrue(baseboardBounds.get(i).y + baseboardBounds.get(i).height >= mattBounds.get(i).y + mattBounds.get(i).height);
      assertTrue(shinyBounds.get(i).x < baseboardBounds.get(i).x);
    }
  }

  public void testWallShapesAreDisplayedOneUnderTheOther() {
    java.awt.Rectangle rectangularWallBounds = getBounds("Rectangular wall").get(0);
    java.awt.Rectangle slopingWallBounds = getBounds("Sloping wall").get(0);
    assertTrue(rectangularWallBounds.y < slopingWallBounds.y);
    assertEquals(rectangularWallBounds.x, slopingWallBounds.x);
  }
}
