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
import javax.swing.JToolBar;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.HomePane;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeView;

import junit.framework.TestCase;

/**
 * Tests the tool bar displayed by {@link com.eteks.sweethome3d.swing.HomePane home pane}.
 */
public class HomePaneToolBarTest extends TestCase {
  private HomePane homePane;
  private JToolBar toolBar;

  @Override
  protected void setUp() throws Exception {
    Locale.setDefault(Locale.US);
    UserPreferences preferences = new DefaultUserPreferences() {
        @Override
        public void write() throws RecorderException {
          // Ignore the requests to save preferences
        }
      };
    HomeController homeController = new HomeController(new Home(), preferences, new SwingViewFactory());
    this.homePane = (HomePane)homeController.getView();
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
    assertTrue(previousIndex < indexOf(toolBarActions, HomeView.ActionType.CREATE_PHOTO));
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
