/*
 * PlanItemPanelsLayoutTest.java
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
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.DimensionLine;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Label;
import com.eteks.sweethome3d.model.Polyline;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.PlanController;

import junit.framework.TestCase;

/**
 * Tests the layout of the panels which edit texts, polylines and dimension lines,
 * which should fit in a narrow side bar.
 */
public class PlanItemPanelsLayoutTest extends TestCase {
  private Home           home;
  private PlanController planController;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    this.home = new Home();
    this.planController = new PlanController(this.home, preferences,
        new SwingViewFactory(), new FileContentManager(preferences), null);
  }

  private void select(Selectable item) {
    this.home.setSelectedItems(Arrays.asList(item));
  }

  private void assertNarrowEnoughForSideBar(JComponent panel) {
    int maxWidth = Math.round(400 * SwingTools.getResolutionScale());
    assertTrue("Panel too wide: " + panel.getPreferredSize().width,
        panel.getPreferredSize().width <= maxWidth);
  }

  /**
   * Asserts that the components of <code>panel</code> stay at its top and fill its width
   * when it's displayed in a larger area.
   */
  private void assertComponentsFillWidthAtTop(JComponent panel) {
    Dimension preferredSize = panel.getPreferredSize();
    panel.setSize(preferredSize.width + 200, preferredSize.height + 200);
    layout(panel);
    int bottom = 0;
    int right = 0;
    for (Component child : panel.getComponents()) {
      // Ignore the empty label which fills the remaining space
      if (child.isVisible()
          && !(child instanceof JLabel && ((JLabel)child).getText().length() == 0)) {
        bottom = Math.max(bottom, child.getY() + child.getHeight());
        right = Math.max(right, child.getX() + child.getWidth());
      }
    }
    assertTrue("Components not at top", bottom < panel.getHeight() - 100);
    assertTrue("Components don't fill width", right > preferredSize.width + 100);
  }

  private void layout(Container container) {
    container.doLayout();
    for (Component child : container.getComponents()) {
      if (child instanceof Container) {
        layout((Container)child);
      }
    }
  }

  public void testLabelPanelFitsInSideBar() {
    Label label = new Label("Hello", 0, 0);
    this.home.addLabel(label);
    select(label);
    JComponent panel = (JComponent)this.planController.createLabelController().getView();
    assertNarrowEnoughForSideBar(panel);
    assertComponentsFillWidthAtTop(panel);
  }

  public void testPolylinePanelFitsInSideBar() {
    Polyline polyline = new Polyline(new float [][] {{0, 0}, {100, 0}});
    this.home.addPolyline(polyline);
    select(polyline);
    JComponent panel = (JComponent)this.planController.createPolylineController().getView();
    assertNarrowEnoughForSideBar(panel);
    assertComponentsFillWidthAtTop(panel);
  }

  public void testDimensionLinePanelFitsInSideBar() {
    DimensionLine dimensionLine = new DimensionLine(0, 0, 100, 0, 20);
    this.home.addDimensionLine(dimensionLine);
    select(dimensionLine);
    JComponent panel = (JComponent)this.planController.createDimensionLineController().getView();
    assertNarrowEnoughForSideBar(panel);
    assertComponentsFillWidthAtTop(panel);
  }
}
