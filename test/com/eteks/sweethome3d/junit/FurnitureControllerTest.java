/*
 * FurnitureControllerTest.java 15 oct. 2026
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;

import junit.framework.TestCase;

/**
 * Tests furniture controller.
 */
public class FurnitureControllerTest extends TestCase {
  public void testToggleFurnitureGrouping() {
    Home home = new Home();
    FurnitureController controller = new FurnitureController(home, new DefaultUserPreferences(), null);
    final int [] changeCount = {0};
    home.addPropertyChangeListener(FurnitureController.FURNITURE_GROUPED_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          changeCount [0]++;
        }
      });

    assertTrue("Furniture not grouped by default", controller.isFurnitureGroupedByLevelAndRoom());
    assertNull(home.getProperty(FurnitureController.FURNITURE_GROUPED_PROPERTY));

    controller.toggleFurnitureGrouping();
    assertFalse("Furniture still grouped", controller.isFurnitureGroupedByLevelAndRoom());
    assertEquals("false", home.getProperty(FurnitureController.FURNITURE_GROUPED_PROPERTY));

    controller.toggleFurnitureGrouping();
    assertTrue("Furniture not grouped", controller.isFurnitureGroupedByLevelAndRoom());
    assertEquals(2, changeCount [0]);
  }

  public void testSetFurnitureGroupedByLevelAndRoom() {
    Home home = new Home();
    FurnitureController controller = new FurnitureController(home, new DefaultUserPreferences(), null);
    controller.setFurnitureGroupedByLevelAndRoom(true);
    assertNull("Property set without change", home.getProperty(FurnitureController.FURNITURE_GROUPED_PROPERTY));
    controller.setFurnitureGroupedByLevelAndRoom(false);
    assertFalse(controller.isFurnitureGroupedByLevelAndRoom());
    controller.setFurnitureGroupedByLevelAndRoom(false);
    assertFalse(controller.isFurnitureGroupedByLevelAndRoom());
    controller.setFurnitureGroupedByLevelAndRoom(true);
    assertTrue(controller.isFurnitureGroupedByLevelAndRoom());
  }
}
