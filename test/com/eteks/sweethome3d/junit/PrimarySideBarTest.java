/*
 * PrimarySideBarTest.java
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
import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.swing.CollapsibleSection;
import com.eteks.sweethome3d.swing.PrimarySideBar;

import junit.framework.TestCase;

/**
 * Tests {@link com.eteks.sweethome3d.swing.PrimarySideBar primary side bar}
 * and its {@link com.eteks.sweethome3d.swing.CollapsibleSection collapsible sections}.
 */
public class PrimarySideBarTest extends TestCase {
  private PrimarySideBar     sideBar;
  private CollapsibleSection topSection;
  private CollapsibleSection middleSection;
  private CollapsibleSection bottomSection;
  private int                headerHeight;
  private int                sizerHeight;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    this.sideBar = new PrimarySideBar(new DefaultUserPreferences());
    this.topSection = new CollapsibleSection("Top", new JPanel());
    this.middleSection = new CollapsibleSection("Middle", new JPanel());
    this.bottomSection = new CollapsibleSection("Bottom", new JPanel());
    this.sideBar.addSection(this.topSection, 1);
    this.sideBar.addSection(this.middleSection, 1);
    this.sideBar.addSection(this.bottomSection, 2);
    this.headerHeight = this.topSection.getHeader().getPreferredSize().height;
    this.sizerHeight = this.sideBar.getSizer(this.bottomSection).getPreferredSize().height;
    // Give a height that leaves 400 pixels to the contents of the three sections separated by two sizers
    setContentsHeight(400, 2);
  }

  /**
   * Sets the size of the side bar to leave the given height to the contents of its sections.
   */
  private void setContentsHeight(int contentsHeight, int visibleSizersCount) {
    this.sideBar.setSize(200, 3 * this.headerHeight + visibleSizersCount * this.sizerHeight + contentsHeight);
    layout(this.sideBar);
  }

  public void testSizersAreDisplayedBetweenExpandedSections() {
    assertTrue(this.sizerHeight > 0);
    assertNull("No sizer above first section", this.sideBar.getSizer(this.topSection));
    JComponent middleSizer = this.sideBar.getSizer(this.middleSection);
    JComponent bottomSizer = this.sideBar.getSizer(this.bottomSection);
    assertTrue(middleSizer.isVisible());
    assertEquals(this.topSection.getHeight(), middleSizer.getY());
    assertEquals(this.sizerHeight, middleSizer.getHeight());
    assertEquals(200, middleSizer.getWidth());
    assertEquals(middleSizer.getY() + this.sizerHeight, this.middleSection.getY());
    assertEquals(this.bottomSection.getY() - this.sizerHeight, bottomSizer.getY());
    assertEquals(Cursor.N_RESIZE_CURSOR, bottomSizer.getCursor().getType());
  }

  public void testSizersAreDisplayedOnlyAboveExpandedSectionsWithAnExpandedSectionAbove() {
    this.middleSection.setCollapsed(true);
    layout(this.sideBar);
    assertFalse(this.sideBar.getSizer(this.middleSection).isVisible());
    assertTrue(this.sideBar.getSizer(this.bottomSection).isVisible());
    assertEquals("Hidden sizer shouldn't take room",
        this.topSection.getHeight(), this.middleSection.getY());

    this.middleSection.setCollapsed(false);
    this.topSection.setCollapsed(true);
    layout(this.sideBar);
    assertFalse(this.sideBar.getSizer(this.middleSection).isVisible());
    assertTrue(this.sideBar.getSizer(this.bottomSection).isVisible());

    this.middleSection.setCollapsed(true);
    layout(this.sideBar);
    assertFalse(this.sideBar.getSizer(this.bottomSection).isVisible());
  }

  public void testDraggingHeaderDoesNotResizeSections() {
    int middleHeight = this.middleSection.getHeight();
    drag(this.bottomSection.getHeader(), -50);
    layout(this.sideBar);

    assertEquals(middleHeight, this.middleSection.getHeight());
  }

  public void testExpandedSectionsShareHeightAccordingToTheirWeights() {
    assertEquals(this.headerHeight + 100, this.topSection.getHeight());
    assertEquals(this.headerHeight + 100, this.middleSection.getHeight());
    assertEquals(this.headerHeight + 200, this.bottomSection.getHeight());
    assertEquals(0, this.topSection.getY());
    assertEquals(this.topSection.getHeight() + this.sizerHeight, this.middleSection.getY());
    assertEquals(200, this.bottomSection.getWidth());
  }

  public void testCollapsedSectionShowsOnlyItsHeader() {
    this.middleSection.setCollapsed(true);
    setContentsHeight(400, 1);

    assertFalse(this.middleSection.getContent().isVisible());
    assertEquals(this.headerHeight, this.middleSection.getHeight());
    // The height of the collapsed section is shared by the other ones
    assertEquals(this.headerHeight + 133, this.topSection.getHeight());
    assertEquals(this.headerHeight + 267, this.bottomSection.getHeight());
  }

  public void testSectionsStackedAtTopWhenAllCollapsed() {
    this.topSection.setCollapsed(true);
    this.middleSection.setCollapsed(true);
    this.bottomSection.setCollapsed(true);
    layout(this.sideBar);

    assertEquals(this.headerHeight, this.topSection.getHeight());
    assertEquals(this.headerHeight, this.middleSection.getY());
    assertEquals(2 * this.headerHeight, this.bottomSection.getY());
    assertEquals(this.headerHeight, this.bottomSection.getHeight());
  }

  public void testClickOnHeaderTogglesSectionAndNotifiesListeners() {
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    this.topSection.addPropertyChangeListener(CollapsibleSection.COLLAPSED_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });

    click(this.topSection.getHeader());
    assertTrue(this.topSection.isCollapsed());
    assertEquals(1, events.size());
    assertEquals(Boolean.TRUE, events.get(0).getNewValue());

    click(this.topSection.getHeader());
    assertFalse(this.topSection.isCollapsed());
    assertTrue(this.topSection.getContent().isVisible());
  }

  public void testDraggingSizerResizesExpandedSectionsAroundIt() {
    // Drag the sizer above bottom section 50 pixels up
    drag(this.sideBar.getSizer(this.bottomSection), -50);
    layout(this.sideBar);

    assertFalse("Drag shouldn't collapse section", this.bottomSection.isCollapsed());
    assertEquals(this.headerHeight + 100, this.topSection.getHeight());
    assertEquals(this.headerHeight + 50, this.middleSection.getHeight());
    assertEquals(this.headerHeight + 250, this.bottomSection.getHeight());
    // Check weights changed to keep this layout when side bar is resized
    assertEquals(5f, this.sideBar.getSectionWeight(this.bottomSection) / this.sideBar.getSectionWeight(this.middleSection), 0.01f);
  }

  public void testDraggingSizerSkipsCollapsedSections() {
    this.middleSection.setCollapsed(true);
    layout(this.sideBar);
    int topHeight = this.topSection.getHeight();
    int bottomHeight = this.bottomSection.getHeight();

    // Dragging the sizer of bottom section resizes top section since middle one is collapsed
    drag(this.sideBar.getSizer(this.bottomSection), 30);
    layout(this.sideBar);

    assertEquals(topHeight + 30, this.topSection.getHeight());
    assertEquals(this.headerHeight, this.middleSection.getHeight());
    assertEquals(bottomHeight - 30, this.bottomSection.getHeight());
  }

  public void testDraggingSizerCantShrinkSectionBelowItsHeader() {
    drag(this.sideBar.getSizer(this.bottomSection), -1000);
    layout(this.sideBar);

    assertEquals(this.headerHeight, this.middleSection.getHeight());
    assertEquals(this.headerHeight + 100, this.topSection.getHeight());
    assertEquals(this.headerHeight + 300, this.bottomSection.getHeight());
  }

  public void testToggleButtonCollapsesAndExpandsSideBar() {
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    this.sideBar.addPropertyChangeListener(PrimarySideBar.COLLAPSED_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });
    assertFalse(this.sideBar.isCollapsed());
    String collapseToolTip = this.sideBar.getToggleButton().getToolTipText();

    this.sideBar.getToggleButton().doClick();
    assertTrue(this.sideBar.isCollapsed());
    assertFalse("Collapsed side bar should be hidden", this.sideBar.isVisible());
    assertEquals(1, events.size());
    assertFalse(collapseToolTip.equals(this.sideBar.getToggleButton().getToolTipText()));

    this.sideBar.getToggleButton().doClick();
    assertFalse(this.sideBar.isCollapsed());
    assertTrue(this.sideBar.isVisible());
    assertEquals(collapseToolTip, this.sideBar.getToggleButton().getToolTipText());
  }

  public void testToggleButtonIsInEdgeStripOutOfSideBar() {
    JComponent edgeStrip = this.sideBar.getEdgeStrip();
    assertTrue(javax.swing.SwingUtilities.isDescendingFrom(this.sideBar.getToggleButton(), edgeStrip));
    assertFalse("Edge strip should remain visible when side bar is collapsed",
        javax.swing.SwingUtilities.isDescendingFrom(edgeStrip, this.sideBar));
  }

  public void testSectionContentCanBeReplaced() {
    JPanel newContent = new JPanel();
    this.topSection.setContent(newContent);
    assertSame(newContent, this.topSection.getContent());
    assertSame(this.topSection, newContent.getParent());

    this.topSection.setCollapsed(true);
    this.topSection.setContent(new JPanel());
    assertFalse("Content of a collapsed section should be hidden", this.topSection.getContent().isVisible());
  }

  /**
   * Lays out the given container and its children, as validation does for a displayed component.
   */
  private void layout(Container container) {
    container.doLayout();
    for (Component child : container.getComponents()) {
      if (child instanceof Container) {
        layout((Container)child);
      }
    }
  }

  private void click(JComponent component) {
    long time = System.currentTimeMillis();
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, time,
        InputEvent.BUTTON1_DOWN_MASK, 10, 5, 1, false, MouseEvent.BUTTON1));
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, time,
        0, 10, 5, 1, false, MouseEvent.BUTTON1));
  }

  private void drag(JComponent component, int dy) {
    long time = System.currentTimeMillis();
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_PRESSED, time,
        InputEvent.BUTTON1_DOWN_MASK, 10, 5, 1, false, MouseEvent.BUTTON1));
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_DRAGGED, time,
        InputEvent.BUTTON1_DOWN_MASK, 10, 5 + dy, 1, false, MouseEvent.BUTTON1));
    component.dispatchEvent(new MouseEvent(component, MouseEvent.MOUSE_RELEASED, time,
        0, 10, 5 + dy, 1, false, MouseEvent.BUTTON1));
  }
}
