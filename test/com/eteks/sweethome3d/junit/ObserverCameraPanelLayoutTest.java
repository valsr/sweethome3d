/*
 * ObserverCameraPanelLayoutTest.java
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
import java.awt.Rectangle;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.ObserverCameraPanel;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.ObserverCameraController;

import junit.framework.TestCase;

/**
 * Tests the layout of {@link com.eteks.sweethome3d.swing.ObserverCameraPanel observer camera panel},
 * which should fit in a narrow side bar.
 */
public class ObserverCameraPanelLayoutTest extends TestCase {
  private ObserverCameraPanel panel;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences();
    ObserverCameraController controller = new ObserverCameraController(new Home(), preferences, new SwingViewFactory());
    this.panel = (ObserverCameraPanel)controller.getView();
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
   * Returns the bounds in the panel of the label which text starts with the given prefix.
   */
  private Rectangle getLabelBounds(String textPrefix) {
    for (JLabel label : SwingTools.findChildren(this.panel, JLabel.class)) {
      if (label.getText() != null && label.getText().startsWith(textPrefix)) {
        return SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), this.panel);
      }
    }
    fail("No label " + textPrefix);
    return null;
  }

  public void testPanelIsNarrowEnoughForSideBar() {
    int maxWidth = Math.round(400 * SwingTools.getResolutionScale());
    assertTrue("Panel too wide: " + this.panel.getPreferredSize().width,
        this.panel.getPreferredSize().width <= maxWidth);
  }

  public void testAnglesAreDisplayedUnderLocation() {
    Rectangle elevationBounds = getLabelBounds("Eyes elevation");
    Rectangle yawBounds = getLabelBounds("Body angle");
    assertTrue(elevationBounds.y < yawBounds.y);
    assertEquals(elevationBounds.x, yawBounds.x);
  }

  public void testCoordinatesAndAnglesAreDisplayedInCompactRows() {
    assertEquals(getLabelBounds("X (").y, getLabelBounds("Y (").y);
    assertTrue(getLabelBounds("X (").x < getLabelBounds("Y (").x);
    assertTrue(getLabelBounds("X (").y < getLabelBounds("Eyes elevation").y);
    assertEquals(getLabelBounds("Body angle").y, getLabelBounds("Head angle").y);
    assertTrue(getLabelBounds("Body angle").y < getLabelBounds("Field of view").y);
  }
}
