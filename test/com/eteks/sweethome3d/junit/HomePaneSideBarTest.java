/*
 * HomePaneSideBarTest.java
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.CollapsibleSection;
import com.eteks.sweethome3d.swing.FurniturePropertiesPanel;
import com.eteks.sweethome3d.swing.HomePane;
import com.eteks.sweethome3d.swing.PrimarySideBar;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;

import junit.framework.TestCase;

/**
 * Tests the primary side bar displayed by {@link com.eteks.sweethome3d.swing.HomePane home pane}.
 */
public class HomePaneSideBarTest extends TestCase {
  private UserPreferences preferences;
  private Home            home;
  private HomeController  homeController;
  private HomePane        homePane;
  private PrimarySideBar  sideBar;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    this.preferences = new DefaultUserPreferences() {
        @Override
        public void write() throws RecorderException {
          // Ignore the requests to save preferences
        }
      };
    this.preferences.setFurnitureCatalogViewedInTree(true);
    this.home = new Home();
    createHomePane();
  }

  private void createHomePane() {
    this.homeController = new HomeController(this.home, this.preferences, new SwingViewFactory());
    this.homePane = (HomePane)this.homeController.getView();
    this.sideBar = SwingTools.findChildren(this.homePane, PrimarySideBar.class).get(0);
  }

  private CollapsibleSection getSection(String title) {
    for (CollapsibleSection section : this.sideBar.getSections()) {
      if (title.equals(section.getTitle())) {
        return section;
      }
    }
    fail("No section " + title);
    return null;
  }

  public void testCatalogAndFurnitureAreDisplayedInSections() {
    JComponent catalogView = (JComponent)this.homeController.getFurnitureCatalogController().getView();
    JComponent furnitureView = (JComponent)this.homeController.getFurnitureController().getView();

    List<CollapsibleSection> sections = this.sideBar.getSections();
    assertEquals("Catalog", sections.get(0).getTitle());
    assertEquals("Furniture", sections.get(sections.size() - 1).getTitle());
    assertTrue(SwingUtilities.isDescendingFrom(catalogView, getSection("Catalog").getContent()));
    assertTrue(SwingUtilities.isDescendingFrom(furnitureView, getSection("Furniture").getContent()));
    assertFalse(getSection("Catalog").isCollapsed());
    assertFalse(getSection("Furniture").isCollapsed());
  }

  public void testCollapsedSectionsAreRestoredWithHome() {
    getSection("Catalog").setCollapsed(true);

    createHomePane();
    assertTrue(getSection("Catalog").isCollapsed());
    assertFalse(getSection("Furniture").isCollapsed());

    getSection("Catalog").setCollapsed(false);
    createHomePane();
    assertFalse(getSection("Catalog").isCollapsed());
  }

  public void testSectionWeightsAreRestoredWithHome() {
    this.sideBar.setSectionWeight(getSection("Catalog"), 3);
    this.sideBar.setSectionWeight(getSection("Furniture"), 7);
    // Simulate the notification sent at the end of a drag in a section header
    this.sideBar.firePropertyChange(PrimarySideBar.SECTION_WEIGHTS_PROPERTY, 0, 1);

    createHomePane();
    assertEquals(3f, this.sideBar.getSectionWeight(getSection("Catalog")), 0.001f);
    assertEquals(7f, this.sideBar.getSectionWeight(getSection("Furniture")), 0.001f);
  }

  public void testCatalogSectionDisplaysNewCatalogViewWhenItsTypeChanges() {
    JComponent treeView = (JComponent)this.homeController.getFurnitureCatalogController().getView();

    this.preferences.setFurnitureCatalogViewedInTree(false);

    JComponent listView = (JComponent)this.homeController.getFurnitureCatalogController().getView();
    assertNotSame(treeView, listView);
    assertTrue(SwingUtilities.isDescendingFrom(listView, getSection("Catalog").getContent()));
    assertFalse(SwingUtilities.isDescendingFrom(treeView, this.homePane));
  }

  private FurniturePropertiesPanel getPropertiesPanel() {
    return SwingTools.findChildren(getSection("Properties"), FurniturePropertiesPanel.class).get(0);
  }

  /**
   * Adds a selected piece to home and returns it.
   */
  private HomePieceOfFurniture addSelectedPiece() {
    HomePieceOfFurniture piece = new HomePieceOfFurniture(
        this.preferences.getFurnitureCatalog().getCategories().get(0).getFurniture().get(0));
    this.home.addPieceOfFurniture(piece);
    this.home.setSelectedItems(Arrays.asList(new Selectable [] {piece}));
    return piece;
  }

  private void waitForUpdate() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
        }
      });
  }

  public void testFurniturePropertiesAreDisplayedBetweenCatalogAndFurniture() throws Exception {
    List<CollapsibleSection> sections = this.sideBar.getSections();
    assertEquals(3, sections.size());
    assertEquals("Properties", sections.get(1).getTitle());

    HomePieceOfFurniture piece = addSelectedPiece();
    waitForUpdate();
    assertEquals(piece.getName(), getPropertiesPanel().getFurnitureController().getName());
  }

  public void testModifyFurnitureRevealsPropertiesSectionInsteadOfDialog() throws Exception {
    HomePieceOfFurniture piece = addSelectedPiece();
    getSection("Properties").setCollapsed(true);
    this.sideBar.setCollapsed(true);

    this.homeController.getFurnitureController().modifySelectedFurniture();
    waitForUpdate();

    assertFalse(this.sideBar.isCollapsed());
    assertFalse(getSection("Properties").isCollapsed());
    assertEquals(piece.getName(), getPropertiesPanel().getFurnitureController().getName());
  }

  public void testModifyFurnitureSelectedInPlanRevealsPropertiesSection() throws Exception {
    addSelectedPiece();
    getSection("Properties").setCollapsed(true);

    this.homeController.getPlanController().modifySelectedItem();
    waitForUpdate();

    assertFalse(getSection("Properties").isCollapsed());
  }

  public void testModifyFurnitureKeepsPendingEdits() throws Exception {
    addSelectedPiece();
    waitForUpdate();
    getPropertiesPanel().getFurnitureController().setName("Renamed");

    this.homeController.getFurnitureController().modifySelectedFurniture();
    waitForUpdate();

    assertEquals("Renamed", getPropertiesPanel().getFurnitureController().getName());
  }

  public void testFurniturePropertiesAreNotUpdatedWhileHidden() throws Exception {
    getSection("Properties").setCollapsed(true);
    addSelectedPiece();
    waitForUpdate();
    assertNull(getPropertiesPanel().getFurnitureController());

    // Collapse side bar first to avoid an update between the two changes
    this.sideBar.setCollapsed(true);
    getSection("Properties").setCollapsed(false);
    waitForUpdate();
    assertNull(getPropertiesPanel().getFurnitureController());

    this.sideBar.setCollapsed(false);
    waitForUpdate();
    assertNotNull(getPropertiesPanel().getFurnitureController());
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

  public void testEdgeStripIsDisplayedOutOfSideBar() {
    assertTrue(SwingUtilities.isDescendingFrom(this.sideBar.getEdgeStrip(), this.homePane));
    assertFalse(SwingUtilities.isDescendingFrom(this.sideBar.getEdgeStrip(), this.sideBar.getParent()));
  }

  public void testCollapsedSideBarIsRestoredWithHome() {
    this.sideBar.getToggleButton().doClick();
    assertTrue(this.sideBar.isCollapsed());

    createHomePane();
    assertTrue(this.sideBar.isCollapsed());
    assertFalse(this.sideBar.isVisible());

    this.sideBar.getToggleButton().doClick();
    createHomePane();
    assertFalse(this.sideBar.isCollapsed());
  }

  public void testPlanAnd3DViewTakeTheWidthOfCollapsedSideBar() {
    JSplitPane mainPane = (JSplitPane)this.sideBar.getParent();
    // Don't rely on the side where the side bar is displayed, which depends on components orientation
    Component planView3DPane = mainPane.getLeftComponent() == this.sideBar
        ? mainPane.getRightComponent()
        : mainPane.getLeftComponent();
    this.homePane.setSize(1200, 800);
    layout(this.homePane);
    int sideBarWidth = this.sideBar.getWidth();
    int planView3DPaneWidth = planView3DPane.getWidth();
    int edgeStripWidth = this.sideBar.getEdgeStrip().getWidth();
    assertTrue(sideBarWidth > 100);
    assertTrue(edgeStripWidth > 0);

    this.sideBar.setCollapsed(true);
    layout(this.homePane);
    assertTrue("Plan and 3D view not enlarged", planView3DPane.getWidth() >= planView3DPaneWidth + sideBarWidth);
    assertEquals("Divider of a collapsed side bar shouldn't be displayed",
        mainPane.getWidth() - mainPane.getInsets().left - mainPane.getInsets().right, planView3DPane.getWidth());
    assertEquals("Edge strip should remain displayed", edgeStripWidth, this.sideBar.getEdgeStrip().getWidth());

    this.sideBar.setCollapsed(false);
    layout(this.homePane);
    assertEquals(sideBarWidth, this.sideBar.getWidth());
    assertEquals(planView3DPaneWidth, planView3DPane.getWidth());
  }
}
