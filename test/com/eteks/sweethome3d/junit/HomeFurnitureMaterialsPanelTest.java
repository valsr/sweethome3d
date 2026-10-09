/*
 * HomeFurnitureMaterialsPanelTest.java 9 oct. 2026
 *
 * Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.eteks.sweethome3d.junit;

import java.awt.Component;
import java.awt.EventQueue;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.undo.UndoableEditSupport;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeMaterial;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.HomeFurniturePanel;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;

/**
 * Tests the materials panel displayed in the color and texture tab of
 * {@link com.eteks.sweethome3d.swing.HomeFurniturePanel home piece of furniture panel}.
 */
public class HomeFurnitureMaterialsPanelTest extends TestCase {
  private HomeFurnitureController controller;
  private HomeFurniturePanel      panel;
  private JComponent              materialsPanel;
  private JList                   materialsList;

  public void testMaterialsPanel() throws Exception {
    final UserPreferences preferences = new DefaultUserPreferences();
    HomePieceOfFurniture catalogPiece = null;
    for (FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (!(piece instanceof CatalogLight) && !piece.isDoorOrWindow() && catalogPiece == null) {
          catalogPiece = new HomePieceOfFurniture(piece);
        }
      }
    }
    final HomePieceOfFurniture piece = catalogPiece;
    final Home home = new Home();
    home.addPieceOfFurniture(piece);
    home.setSelectedItems(Arrays.asList(new Selectable [] {piece}));

    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          try {
            controller = new HomeFurnitureController(home, preferences, new SwingViewFactory(), new UndoableEditSupport());
            panel = (HomeFurniturePanel)controller.getView();
            materialsPanel = (JComponent)TestUtilities.getField(panel, "modelMaterialsComponent");
            materialsList = (JList)TestUtilities.getField(materialsPanel, "materialsList");
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      });
    assertNotNull("No materials panel", this.materialsPanel);
    // Wait for the model of the piece to be loaded
    for (int i = 0; i < 100 && this.materialsList.getModel().getSize() == 0; i++) {
      Thread.sleep(100);
    }
    assertTrue("Materials not listed", this.materialsList.getModel().getSize() > 0);

    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          try {
            JRadioButton defaultRadioButton = (JRadioButton)TestUtilities.getField(panel, "defaultColorAndTextureRadioButton");
            JRadioButton materialsRadioButton = (JRadioButton)TestUtilities.getField(panel, "modelMaterialsRadioButton");
            JRadioButton invisibleRadioButton = (JRadioButton)TestUtilities.getField(materialsPanel, "invisibleRadioButton");
            Component colorButton = (Component)TestUtilities.getField(materialsPanel, "colorButton");

            // Materials panel is disabled as long as materials radio button isn't selected
            assertTrue(defaultRadioButton.isSelected());
            assertEquals(HomeFurnitureController.FurniturePaint.DEFAULT, controller.getPaint());
            assertFalse("Materials panel enabled", materialsPanel.isEnabled());
            assertFalse("Materials list enabled", materialsList.isEnabled());
            assertFalse("Invisible radio button enabled", invisibleRadioButton.isEnabled());
            assertFalse("Color button enabled", colorButton.isEnabled());
            assertNull("Materials modified", controller.getModelMaterialsController().getMaterials());

            materialsRadioButton.setSelected(true);
            assertEquals(HomeFurnitureController.FurniturePaint.MODEL_MATERIALS, controller.getPaint());
            assertTrue("Materials panel disabled", materialsPanel.isEnabled());
            assertTrue("Materials list disabled", materialsList.isEnabled());
            assertTrue("Invisible radio button disabled", invisibleRadioButton.isEnabled());
            assertTrue("Color button disabled", colorButton.isEnabled());
            assertNull("Materials modified", controller.getModelMaterialsController().getMaterials());

            // A modification in materials panel updates controller at once
            materialsList.setSelectedIndex(0);
            invisibleRadioButton.setSelected(true);
            HomeMaterial [] materials = controller.getModelMaterialsController().getMaterials();
            assertNotNull("Materials not modified", materials);
            assertEquals("Material not invisible", Integer.valueOf(0), materials [0].getColor());
            assertEquals(HomeFurnitureController.FurniturePaint.MODEL_MATERIALS, controller.getPaint());

            // Going back to default disables the panel without changing the paint mode
            defaultRadioButton.setSelected(true);
            assertEquals(HomeFurnitureController.FurniturePaint.DEFAULT, controller.getPaint());
            assertFalse("Materials panel enabled", materialsPanel.isEnabled());
            assertFalse("Invisible radio button enabled", invisibleRadioButton.isEnabled());
            assertEquals(HomeFurnitureController.FurniturePaint.DEFAULT, controller.getPaint());

            controller.modifyFurniture();
            assertNull("Materials used whereas they aren't selected", piece.getModelMaterials());

            materialsRadioButton.setSelected(true);
            assertTrue("Materials panel disabled", materialsPanel.isEnabled());
            controller.modifyFurniture();
            assertNotNull("Materials not used", piece.getModelMaterials());
            assertEquals("Material not invisible", Integer.valueOf(0), piece.getModelMaterials() [0].getColor());
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      });
  }
}
