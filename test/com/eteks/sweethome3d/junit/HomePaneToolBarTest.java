/*
 * HomePaneToolBarTest.java
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

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.JToolBar;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeEnvironment;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.HomePane;
import com.eteks.sweethome3d.swing.PlanComponent;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.PlanController;
import com.eteks.sweethome3d.viewcontroller.HomeView;

import junit.framework.TestCase;

/**
 * Tests the tool bar displayed by {@link com.eteks.sweethome3d.swing.HomePane home pane}.
 */
public class HomePaneToolBarTest extends TestCase {
  private UserPreferences preferences;
  private Home           home;
  private HomeController homeController;
  private HomePane       homePane;
  private JToolBar       toolBar;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = this.preferences = new DefaultUserPreferences() {
        @Override
        public void write() throws RecorderException {
          // Ignore the requests to save preferences
        }
      };
    this.home = new Home();
    this.homeController = new HomeController(this.home, preferences, new SwingViewFactory());
    this.homePane = (HomePane)this.homeController.getView();
    this.toolBar = SwingTools.findChildren(this.homePane, JToolBar.class).get(0);
  }

  /**
   * Returns the actions of the tool bar buttons in their order.
   */
  private List<Action> getToolBarActions() {
    List<Action> actions = new ArrayList<Action>();
    for (AbstractButton button : SwingTools.findChildren(this.toolBar, AbstractButton.class)) {
      if (button.getAction() != null) {
        actions.add(button.getAction());
      }
    }
    return actions;
  }

  private int indexOf(List<Action> toolBarActions, HomeView.ActionType actionType) {
    Object description = this.homePane.getActionMap().get(actionType).getValue(Action.SHORT_DESCRIPTION);
    for (int i = 0; i < toolBarActions.size(); i++) {
      if (description.equals(toolBarActions.get(i).getValue(Action.SHORT_DESCRIPTION))) {
        return i;
      }
    }
    return -1;
  }

  public void testAlignmentFlipAndGroupActionsAreInToolBarInOrder() {
    List<Action> toolBarActions = getToolBarActions();
    List<HomeView.ActionType> expectedActions = Arrays.asList(
        HomeView.ActionType.FLIP_HORIZONTALLY, HomeView.ActionType.FLIP_VERTICALLY,
        HomeView.ActionType.GROUP_FURNITURE, HomeView.ActionType.UNGROUP_FURNITURE,
        HomeView.ActionType.ALIGN_FURNITURE_ON_TOP, HomeView.ActionType.ALIGN_FURNITURE_ON_BOTTOM,
        HomeView.ActionType.ALIGN_FURNITURE_ON_LEFT, HomeView.ActionType.ALIGN_FURNITURE_ON_RIGHT,
        HomeView.ActionType.ALIGN_FURNITURE_ON_FRONT_SIDE, HomeView.ActionType.ALIGN_FURNITURE_ON_BACK_SIDE,
        HomeView.ActionType.ALIGN_FURNITURE_ON_LEFT_SIDE, HomeView.ActionType.ALIGN_FURNITURE_ON_RIGHT_SIDE,
        HomeView.ActionType.ALIGN_FURNITURE_SIDE_BY_SIDE,
        HomeView.ActionType.DISTRIBUTE_FURNITURE_HORIZONTALLY, HomeView.ActionType.DISTRIBUTE_FURNITURE_VERTICALLY);
    int previousIndex = indexOf(toolBarActions, HomeView.ActionType.ZOOM_OUT);
    for (HomeView.ActionType actionType : expectedActions) {
      int index = indexOf(toolBarActions, actionType);
      assertTrue(actionType + " missing or misplaced", index > previousIndex);
      previousIndex = index;
    }
    int seeThroughWallsIndex = indexOf(toolBarActions, HomeView.ActionType.TOGGLE_SEE_THROUGH_WALLS);
    assertTrue(previousIndex < seeThroughWallsIndex);
    assertTrue(seeThroughWallsIndex < indexOf(toolBarActions, HomeView.ActionType.CREATE_PHOTO));
  }

  private JToggleButton getSeeThroughWallsButton() {
    Object description = this.homePane.getActionMap().get(HomeView.ActionType.TOGGLE_SEE_THROUGH_WALLS)
        .getValue(Action.SHORT_DESCRIPTION);
    for (JToggleButton button : SwingTools.findChildren(this.toolBar, JToggleButton.class)) {
      if (description.equals(button.getAction().getValue(Action.SHORT_DESCRIPTION))) {
        return button;
      }
    }
    fail("No see-through walls button");
    return null;
  }

  public void testSeeThroughWallsButtonTogglesWallsTransparency() {
    JToggleButton button = getSeeThroughWallsButton();
    assertTrue(button.isEnabled());
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
    assertFalse(button.isSelected());

    button.doClick();
    assertEquals(0.5f, this.home.getEnvironment().getWallsAlpha());
    assertTrue(button.isSelected());
    button.doClick();
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
    assertFalse(button.isSelected());

    this.homeController.undo();
    assertEquals(0.5f, this.home.getEnvironment().getWallsAlpha());
    assertTrue(button.isSelected());
    this.homeController.redo();
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
  }

  public void testSeeThroughWallsControlsChangeTransparencyOnce() {
    final int [] changeCount = {0};
    this.home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.WALLS_ALPHA,
        new java.beans.PropertyChangeListener() {
          public void propertyChange(java.beans.PropertyChangeEvent ev) {
            changeCount [0]++;
          }
        });
    getSeeThroughWallsButton().doClick();
    assertEquals("Walls transparency changed more than once by a click", 1, changeCount [0]);
    assertEquals(0.5f, this.home.getEnvironment().getWallsAlpha());

    // A single undo restores solid walls and nothing else remains to undo
    this.homeController.undo();
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
    assertFalse(this.homePane.getActionMap().get(HomeView.ActionType.UNDO).isEnabled());
  }

  /**
   * Returns the menu bar item with the given accelerator.
   */
  private JMenuItem getMenuItem(String accelerator) {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(accelerator);
    JMenuItem foundItem = null;
    JMenuBar menuBar = this.homePane.getJMenuBar();
    for (int i = 0; i < menuBar.getMenuCount(); i++) {
      for (JMenuItem item : SwingTools.findChildren(menuBar.getMenu(i).getPopupMenu(), JMenuItem.class)) {
        if (keyStroke.equals(item.getAccelerator())) {
          assertNull("Accelerator " + accelerator + " used twice", foundItem);
          foundItem = item;
        }
      }
    }
    assertNotNull("No menu item with accelerator " + accelerator, foundItem);
    return foundItem;
  }

  public void testMagnetismIsToggledWithShortcut() {
    String shortcut = OperatingSystem.isMacOSX() ? "meta G" : "control G";
    this.preferences.setMagnetismEnabled(true);
    assertEquals("Disable magnetism", getMenuItem(shortcut).getText());
    getMenuItem(shortcut).doClick();
    assertFalse(this.preferences.isMagnetismEnabled());
    // Menu item should propose the opposite action with the same shortcut
    assertEquals("Enable magnetism", getMenuItem(shortcut).getText());
    getMenuItem(shortcut).doClick();
    assertTrue(this.preferences.isMagnetismEnabled());
  }

  public void testSeeThroughWallsAreToggledWithShortcut() {
    JMenuItem seeThroughWallsItem = getMenuItem(OperatingSystem.isMacOSX() ? "meta T" : "control T");
    seeThroughWallsItem.doClick();
    assertEquals(0.5f, this.home.getEnvironment().getWallsAlpha());
    seeThroughWallsItem.doClick();
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
  }

  public void testMagnetismIsForcedWhileSKeyIsHeld() {
    PlanComponent planComponent = SwingTools.findChildren(this.homePane, PlanComponent.class).get(0);
    InputMap inputMap = planComponent.getInputMap(JComponent.WHEN_FOCUSED);
    PlanController planController = this.homeController.getPlanController();
    planComponent.getActionMap().get(inputMap.get(KeyStroke.getKeyStroke("pressed S"))).actionPerformed(null);
    assertTrue(planController.isMagnetismForced());
    planComponent.getActionMap().get(inputMap.get(KeyStroke.getKeyStroke("released S"))).actionPerformed(null);
    assertFalse(planController.isMagnetismForced());
    planComponent.getActionMap().get(inputMap.get(KeyStroke.getKeyStroke("shift pressed S"))).actionPerformed(null);
    assertTrue(planController.isMagnetismForced());
    planComponent.getActionMap().get(inputMap.get(KeyStroke.getKeyStroke("shift released S"))).actionPerformed(null);
    assertFalse(planController.isMagnetismForced());
  }

  public void testSeeThroughWallsButtonRestoresLastTransparency() {
    JToggleButton button = getSeeThroughWallsButton();
    // Simulate a transparency chosen in 3D attributes dialog
    this.home.getEnvironment().setWallsAlpha(0.8f);
    assertTrue(button.isSelected());
    button.doClick();
    assertEquals(0f, this.home.getEnvironment().getWallsAlpha());
    button.doClick();
    assertEquals(0.8f, this.home.getEnvironment().getWallsAlpha());
  }

  public void testToolBarButtonsHaveIconsAndFollowActionState() {
    for (AbstractButton button : SwingTools.findChildren(this.toolBar, AbstractButton.class)) {
      if (button.getAction() != null) {
        assertNotNull("No icon for " + button.getToolTipText(), button.getIcon());
      }
    }
    // Alignment requires several selected pieces
    Action alignAction = this.homePane.getActionMap().get(HomeView.ActionType.ALIGN_FURNITURE_ON_TOP);
    int index = indexOf(getToolBarActions(), HomeView.ActionType.ALIGN_FURNITURE_ON_TOP);
    AbstractButton alignButton = null;
    for (AbstractButton button : SwingTools.findChildren(this.toolBar, AbstractButton.class)) {
      if (button.getAction() == getToolBarActions().get(index)) {
        alignButton = button;
      }
    }
    assertFalse(alignAction.isEnabled());
    assertFalse(alignButton.isEnabled());
    alignAction.setEnabled(true);
    assertTrue(alignButton.isEnabled());
  }
}
