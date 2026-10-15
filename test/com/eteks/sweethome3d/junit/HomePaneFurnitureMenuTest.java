/*
 * HomePaneFurnitureMenuTest.java 15 oct. 2026
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
import java.util.Locale;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.swing.FurnitureTable;
import com.eteks.sweethome3d.swing.HomePane;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import junit.framework.TestCase;

/**
 * Tests the furniture grouping menu items displayed by {@link HomePane home pane}.
 */
public class HomePaneFurnitureMenuTest extends TestCase {
  private Home           home;
  private HomeController homeController;
  private HomePane       homePane;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    DefaultUserPreferences preferences = new DefaultUserPreferences() {
        @Override
        public void write() throws RecorderException {
          // Ignore the requests to save preferences
        }
      };
    this.home = new Home();
    this.homeController = new HomeController(this.home, preferences, new SwingViewFactory());
    this.homePane = (HomePane)this.homeController.getView();
  }

  private JCheckBoxMenuItem getMenuBarGroupingItem() {
    JMenuBar menuBar = this.homePane.getJMenuBar();
    for (int i = 0; i < menuBar.getMenuCount(); i++) {
      JMenu menu = menuBar.getMenu(i);
      if (menu != null) {
        for (Component item : menu.getMenuComponents()) {
          if (item instanceof JCheckBoxMenuItem
              && "Group by level and room".equals(((JCheckBoxMenuItem)item).getText())) {
            return (JCheckBoxMenuItem)item;
          }
        }
      }
    }
    fail("No grouping item in menu bar");
    return null;
  }

  private JCheckBoxMenuItem getPopupGroupingItem() {
    FurnitureTable table = SwingTools.findChildren(
        (JComponent)this.homeController.getFurnitureController().getView(), FurnitureTable.class).get(0);
    for (Component item : table.getComponentPopupMenu().getComponents()) {
      if (item instanceof JCheckBoxMenuItem
          && "Group by level and room".equals(((JCheckBoxMenuItem)item).getText())) {
        return (JCheckBoxMenuItem)item;
      }
    }
    fail("No grouping item in furniture popup menu");
    return null;
  }

  public void testGroupingMenuItemsToggleGroupingOnce() {
    JCheckBoxMenuItem menuBarItem = getMenuBarGroupingItem();
    JCheckBoxMenuItem popupItem = getPopupGroupingItem();
    assertTrue(menuBarItem.isSelected());

    menuBarItem.doClick();
    assertEquals("false", this.home.getProperty(FurnitureController.FURNITURE_GROUPED_PROPERTY));
    assertFalse(menuBarItem.isSelected());
    assertFalse(popupItem.isSelected());

    popupItem.doClick();
    assertEquals("true", this.home.getProperty(FurnitureController.FURNITURE_GROUPED_PROPERTY));
    assertTrue(menuBarItem.isSelected());
  }
}
