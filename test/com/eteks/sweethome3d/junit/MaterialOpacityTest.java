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
import java.awt.image.BufferedImage;
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
import com.eteks.sweethome3d.j3d.PhotoRenderer;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomeMaterial;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Wall;
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
    // Initialize Java 3D first as the application does with its 3D view, to avoid a deadlock between
    // the event dispatch thread and the thread loading models if both initialize it at the same time
    new javax.media.j3d.BranchGroup();
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

  /**
   * Returns the brightness of the floor of a closed room lit by a lamp placed above a slab
   * covering the whole room, which materials have the given <code>opacity</code>.
   */
  private double getBrightnessUnderSlab(float opacity) throws Exception {
    Home home = new Home();
    home.getEnvironment().setLightColor(0xFFFFFF);
    home.getEnvironment().setCeillingLightColor(0);
    float [][] points = {{0, 0}, {400, 0}, {400, 400}, {0, 400}};
    Room room = new Room(points);
    room.setFloorColor(0xFFFFFF);
    room.setCeilingColor(0xFFFFFF);
    home.addRoom(room);
    for (int i = 0; i < points.length; i++) {
      Wall wall = new Wall(points [i][0], points [i][1],
          points [(i + 1) % points.length][0], points [(i + 1) % points.length][1], 10, 250);
      wall.setLeftSideColor(0xFFFFFF);
      wall.setRightSideColor(0xFFFFFF);
      home.addWall(wall);
    }
    HomeLight light = null;
    for (FurnitureCategory category : this.preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (light == null
            && piece instanceof CatalogLight
            && ((CatalogLight)piece).getLightSources().length > 0) {
          light = new HomeLight((CatalogLight)piece);
        }
      }
    }
    light.setX(200);
    light.setY(200);
    light.setElevation(210);
    light.setLightColor(0xFFFFFF);
    light.setPower(0.08f);
    home.addPieceOfFurniture(light);
    HomePieceOfFurniture slab = createPiece();
    slab.setX(200);
    slab.setY(200);
    slab.setElevation(150);
    slab.setWidth(420);
    slab.setDepth(420);
    slab.setHeight(10);
    home.addPieceOfFurniture(slab);
    List<HomeMaterial> materials = new ArrayList<HomeMaterial>();
    for (Appearance appearance : getAppearances(home, slab)) {
      materials.add(new HomeMaterial(appearance.getName(), null, null, null, null, opacity));
    }
    slab.setModelMaterials(materials.toArray(new HomeMaterial [materials.size()]));

    // View the floor from under the slab
    Camera camera = home.getObserverCamera();
    camera.setX(200);
    camera.setY(380);
    camera.setZ(100);
    camera.setYaw((float)Math.PI);
    camera.setPitch(0.5f);
    camera.setFieldOfView((float)Math.toRadians(80));
    PhotoRenderer renderer = new PhotoRenderer(home, PhotoRenderer.Quality.LOW);
    BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
    try {
      renderer.render(image, camera, null);
    } finally {
      renderer.dispose();
    }
    long sum = 0;
    for (int y = image.getHeight() / 2; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int rgb = image.getRGB(x, y);
        sum += ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
      }
    }
    return sum / (3. * image.getWidth() * image.getHeight() / 2);
  }

  /**
   * Tests the light passing through a material depends on its opacity in photos,
   * instead of passing as through glass as soon as the material isn't opaque.
   */
  public void testLightThroughMaterialInPhoto() throws Exception {
    double opaqueBrightness = getBrightnessUnderSlab(1f);
    double almostOpaqueBrightness = getBrightnessUnderSlab(0.9f);
    double halfOpaqueBrightness = getBrightnessUnderSlab(0.5f);
    double almostTransparentBrightness = getBrightnessUnderSlab(0.05f);
    String brightnesses = opaqueBrightness + " " + almostOpaqueBrightness + " "
        + halfOpaqueBrightness + " " + almostTransparentBrightness;
    assertTrue("Light should increase when opacity decreases: " + brightnesses,
        opaqueBrightness < almostOpaqueBrightness
        && almostOpaqueBrightness + 5 < halfOpaqueBrightness
        && halfOpaqueBrightness + 5 < almostTransparentBrightness);
    assertTrue("An almost opaque material should stop most of the light: " + brightnesses,
        almostOpaqueBrightness < almostTransparentBrightness / 2);
    System.out.println("Brightness under a slab at opacity 1, 0.9, 0.5 and 0.05: " + brightnesses);
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
