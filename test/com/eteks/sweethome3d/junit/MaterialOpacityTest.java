/*
 * MaterialOpacityTest.java 9 oct. 2026
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

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.Group;
import javax.media.j3d.Link;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.undo.UndoableEditSupport;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.j3d.Object3DBranchFactory;
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
 * Tests the opacity of {@linkplain HomeMaterial materials}.
 */
public class MaterialOpacityTest extends TestCase {
  private UserPreferences preferences;

  private HomeFurnitureController controller;
  private HomeFurniturePanel      panel;
  private JComponent              materialsPanel;
  private JList                   materialsList;

  @Override
  protected void setUp() throws Exception {
    this.preferences = new DefaultUserPreferences();
  }

  private HomePieceOfFurniture createPiece() {
    for (FurnitureCategory category : this.preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (!(piece instanceof CatalogLight) && !piece.isDoorOrWindow()) {
          return new HomePieceOfFurniture(piece);
        }
      }
    }
    fail("No piece in default catalog");
    return null;
  }

  /**
   * Adds to <code>appearances</code> the appearances of the shapes of <code>node</code>.
   */
  private void listAppearances(Node node, List<Appearance> appearances) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren();
      while (enumeration.hasMoreElements()) {
        listAppearances((Node)enumeration.nextElement(), appearances);
      }
    } else if (node instanceof Link) {
      listAppearances(((Link)node).getSharedGroup(), appearances);
    } else if (node instanceof Shape3D
               && ((Shape3D)node).getAppearance() != null
               && ((Shape3D)node).getAppearance().getName() != null) {
      appearances.add(((Shape3D)node).getAppearance());
    }
  }

  private List<Appearance> getAppearances(Home home, HomePieceOfFurniture piece) {
    List<Appearance> appearances = new ArrayList<Appearance>();
    listAppearances((Node)new Object3DBranchFactory().createObject3D(home, piece, true), appearances);
    assertFalse("Model without named material", appearances.isEmpty());
    return appearances;
  }

  private float getTransparency(Appearance appearance) {
    TransparencyAttributes transparencyAttributes = appearance.getTransparencyAttributes();
    return transparencyAttributes != null && transparencyAttributes.getTransparencyMode() != TransparencyAttributes.NONE
        ? transparencyAttributes.getTransparency()
        : 0;
  }

  public void testMaterialEquality() {
    HomeMaterial material = new HomeMaterial("m", null, 0xFF112233, null, 0.5f, 0.25f);
    assertEquals("Wrong opacity", 0.25f, material.getOpacity());
    assertEquals(material, new HomeMaterial("m", null, 0xFF112233, null, 0.5f, 0.25f));
    assertEquals(material.hashCode(), new HomeMaterial("m", null, 0xFF112233, null, 0.5f, 0.25f).hashCode());
    assertFalse(material.equals(new HomeMaterial("m", null, 0xFF112233, null, 0.5f, 0.5f)));
    assertFalse(material.equals(new HomeMaterial("m", null, 0xFF112233, null, 0.5f)));
    assertNull("Opacity should be unchanged by default", new HomeMaterial("m", null, null, 0.5f).getOpacity());
  }

  public void testMaterialOpacityIn3D() {
    Home home = new Home();
    HomePieceOfFurniture piece = createPiece();
    home.addPieceOfFurniture(piece);
    String materialName = getAppearances(home, piece).get(0).getName();
    float defaultTransparency = getTransparency(getAppearances(home, piece).get(0));

    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, null, null, null, 0.25f)});
    for (Appearance appearance : getAppearances(home, piece)) {
      if (materialName.equals(appearance.getName())) {
        assertEquals("Opacity not applied", 0.75f, getTransparency(appearance), 1E-6f);
      }
    }
    // Opacity is applied with a color too
    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, 0xFFFF0000, null, null, 0.6f)});
    for (Appearance appearance : getAppearances(home, piece)) {
      if (materialName.equals(appearance.getName())) {
        assertEquals("Opacity not applied with a color", 0.4f, getTransparency(appearance), 1E-6f);
      }
    }
    // A material can be made opaque
    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, null, null, null, 1f)});
    for (Appearance appearance : getAppearances(home, piece)) {
      if (materialName.equals(appearance.getName())) {
        assertEquals("Material not opaque", 0f, getTransparency(appearance), 0f);
      }
    }
    // Default transparency comes back without opacity
    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, 0xFFFF0000, null, null)});
    for (Appearance appearance : getAppearances(home, piece)) {
      if (materialName.equals(appearance.getName())) {
        assertEquals("Default transparency not restored", defaultTransparency, getTransparency(appearance), 0f);
      }
    }
  }

  public void testMaterialOpacityRecording() throws Exception {
    Home home = new Home();
    HomePieceOfFurniture piece = createPiece();
    piece.setModelMaterials(new HomeMaterial [] {
        new HomeMaterial("glass", null, null, null, null, 0.25f),
        new HomeMaterial("wood", null, 0xFF804020, null, 0.5f)});
    home.addPieceOfFurniture(piece);
    for (boolean preferXmlEntry : new boolean [] {true, false}) {
      File homeFile = File.createTempFile("materials", ".sh3d");
      try {
        HomeFileRecorder recorder = new HomeFileRecorder(0, false, this.preferences, false, preferXmlEntry);
        recorder.writeHome(home, homeFile.getAbsolutePath());
        HomeMaterial [] readMaterials = recorder.readHome(homeFile.getAbsolutePath()).getFurniture().get(0).getModelMaterials();
        assertEquals("Wrong opacity", 0.25f, readMaterials [0].getOpacity());
        assertNull("Opacity should be unchanged", readMaterials [1].getOpacity());
        assertTrue("Materials changed", Arrays.equals(piece.getModelMaterials(), readMaterials));
      } finally {
        homeFile.delete();
      }
    }
  }

  public void testOpacitySlider() throws Exception {
    final HomePieceOfFurniture piece = createPiece();
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
    // Wait for the model of the piece to be loaded
    for (int i = 0; i < 100 && this.materialsList.getModel().getSize() == 0; i++) {
      Thread.sleep(100);
    }
    assertTrue("Materials not listed", this.materialsList.getModel().getSize() > 0);

    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          try {
            JRadioButton materialsRadioButton = (JRadioButton)TestUtilities.getField(panel, "modelMaterialsRadioButton");
            JSlider opacitySlider = (JSlider)TestUtilities.getField(materialsPanel, "opacitySlider");
            assertFalse("Opacity slider enabled", opacitySlider.isEnabled());
            materialsRadioButton.setSelected(true);
            materialsList.setSelectedIndex(0);
            assertTrue("Opacity slider disabled", opacitySlider.isEnabled());
            assertEquals("Opaque material by default", 100, opacitySlider.getValue());
            assertNull(controller.getModelMaterialsController().getMaterials());

            opacitySlider.setValue(30);
            HomeMaterial [] materials = controller.getModelMaterialsController().getMaterials();
            assertEquals("Wrong opacity", 0.3f, materials [0].getOpacity());
            assertNull("Color modified", materials [0].getColor());

            controller.modifyFurniture();
            assertEquals("Wrong opacity", 0.3f, piece.getModelMaterials() [0].getOpacity());

            // Choosing the default opacity leaves the material unchanged
            opacitySlider.setValue(100);
            assertNull("Material still modified", controller.getModelMaterialsController().getMaterials());
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      });

    // An other panel on the modified piece shows its opacity
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
    for (int i = 0; i < 100 && this.materialsList.getModel().getSize() == 0; i++) {
      Thread.sleep(100);
    }
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          try {
            JSlider opacitySlider = (JSlider)TestUtilities.getField(materialsPanel, "opacitySlider");
            materialsList.setSelectedIndex(0);
            assertEquals("Opacity of the piece not displayed", 30, opacitySlider.getValue());
            assertTrue("Opacity slider disabled", opacitySlider.isEnabled());
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      });
  }
}
