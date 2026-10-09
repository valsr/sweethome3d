/*
 * HomeLightPropertiesTest.java 9 oct. 2026
 *
 * Sweet Home 3D, Copyright (c) 2026 Space Mushrooms <info@sweethome3d.com>
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

import java.awt.EventQueue;
import java.io.File;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.io.HomeFileRecorder;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.LightSource;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.HomeFurniturePanel;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeFurnitureController;

/**
 * Tests the color and power properties of {@linkplain HomeLight lights}.
 */
public class HomeLightPropertiesTest extends TestCase {
  private UserPreferences preferences;

  @Override
  protected void setUp() throws Exception {
    this.preferences = new DefaultUserPreferences();
  }

  /**
   * Returns a new light with at least one light source created from the default catalog.
   */
  private HomeLight createLight() {
    for (FurnitureCategory category : this.preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (piece instanceof CatalogLight
            && ((CatalogLight)piece).getLightSources().length > 0) {
          return new HomeLight((CatalogLight)piece);
        }
      }
    }
    fail("No light in default catalog");
    return null;
  }

  public void testColorAtTemperature() {
    // Warm light is orange
    int warmColor = HomeLight.getColorAtTemperature(2700);
    assertEquals("Wrong red", 255, warmColor >> 16);
    assertTrue("Green should be smaller than red", ((warmColor >> 8) & 0xFF) < 255);
    assertTrue("Blue should be smaller than green", (warmColor & 0xFF) < ((warmColor >> 8) & 0xFF));
    // Day light is almost white
    int dayColor = HomeLight.getColorAtTemperature(6500);
    assertEquals("Wrong red", 255, dayColor >> 16);
    assertTrue("Green should be close to 255", ((dayColor >> 8) & 0xFF) > 240);
    assertTrue("Blue should be close to 255", (dayColor & 0xFF) > 240);
    // Cold light is blue
    int coldColor = HomeLight.getColorAtTemperature(10000);
    assertEquals("Wrong blue", 255, coldColor & 0xFF);
    assertTrue("Red should be smaller than green", (coldColor >> 16) < ((coldColor >> 8) & 0xFF));
    assertTrue("Green should be smaller than blue", ((coldColor >> 8) & 0xFF) < 255);
    // Warmer lights contain less blue
    assertTrue(HomeLight.getColorAtTemperature(2000) < HomeLight.getColorAtTemperature(3000));
  }

  public void testLightSourceColor() {
    HomeLight light = createLight();
    LightSource lightSource = light.getLightSources() [0];
    assertNull("Light color shouldn't be set by default", light.getLightColor());
    assertNull("Light temperature shouldn't be set by default", light.getLightColorTemperature());
    assertNull(light.getEmittedLightColor());
    assertEquals("Light source color should be used by default",
        lightSource.getColor(), light.getLightSourceColor(lightSource));

    light.setLightColor(0x102030);
    assertEquals("Light color should replace light source color", 0x102030, light.getLightSourceColor(lightSource));

    light.setLightColorTemperature(3000);
    assertEquals("Temperature should replace light color",
        HomeLight.getColorAtTemperature(3000), light.getLightSourceColor(lightSource));

    light.setLightColorTemperature(null);
    light.setLightColor(null);
    assertEquals(lightSource.getColor(), light.getLightSourceColor(lightSource));
  }

  public void testLightPropertiesRecording() throws Exception {
    HomeLight colorLight = createLight();
    colorLight.setLightColor(0x102030);
    HomeLight temperatureLight = createLight();
    temperatureLight.setLightColorTemperature(4000);
    temperatureLight.setPowerUnit(HomeLight.PowerUnit.LUMEN);
    temperatureLight.setPower(0.75f);
    HomeLight defaultLight = createLight();
    Home home = new Home();
    home.addPieceOfFurniture(colorLight);
    home.addPieceOfFurniture(temperatureLight);
    home.addPieceOfFurniture(defaultLight);

    for (boolean preferXmlEntry : new boolean [] {true, false}) {
      File homeFile = File.createTempFile("lights", ".sh3d");
      try {
        HomeFileRecorder recorder = new HomeFileRecorder(0, false, this.preferences, false, preferXmlEntry);
        recorder.writeHome(home, homeFile.getAbsolutePath());
        Home readHome = recorder.readHome(homeFile.getAbsolutePath());

        HomeLight readColorLight = (HomeLight)readHome.getFurniture().get(0);
        assertEquals("Wrong color", Integer.valueOf(0x102030), readColorLight.getLightColor());
        assertNull(readColorLight.getLightColorTemperature());
        HomeLight readTemperatureLight = (HomeLight)readHome.getFurniture().get(1);
        assertNull(readTemperatureLight.getLightColor());
        assertEquals("Wrong temperature", Integer.valueOf(4000), readTemperatureLight.getLightColorTemperature());
        assertEquals("Wrong unit", HomeLight.PowerUnit.LUMEN, readTemperatureLight.getPowerUnit());
        assertEquals("Wrong power", 0.75f, readTemperatureLight.getPower());
        HomeLight readDefaultLight = (HomeLight)readHome.getFurniture().get(2);
        assertNull(readDefaultLight.getLightColor());
        assertNull(readDefaultLight.getLightColorTemperature());
        assertEquals("Wrong unit", HomeLight.PowerUnit.PERCENTAGE, readDefaultLight.getPowerUnit());
      } finally {
        homeFile.delete();
      }
    }
  }

  public void testLightColorModification() {
    HomeLight light = createLight();
    Home home = new Home();
    home.addPieceOfFurniture(light);
    home.setSelectedItems(Arrays.asList(new Selectable [] {light}));
    UndoableEditSupport undoSupport = new UndoableEditSupport();
    UndoManager undoManager = new UndoManager();
    undoSupport.addUndoableEditListener(undoManager);

    HomeFurnitureController controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertTrue(controller.isPropertyEditable(HomeFurnitureController.Property.LIGHT_COLOR_MODE));
    assertEquals(HomeFurnitureController.LightColorMode.DEFAULT, controller.getLightColorMode());
    // Choose a temperature
    controller.setLightColorTemperature(3000);
    controller.setLightColorMode(HomeFurnitureController.LightColorMode.TEMPERATURE);
    controller.modifyFurniture();
    assertEquals(Integer.valueOf(3000), light.getLightColorTemperature());
    assertNull(light.getLightColor());

    // Choose a color
    controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertEquals(HomeFurnitureController.LightColorMode.TEMPERATURE, controller.getLightColorMode());
    assertEquals(Integer.valueOf(3000), controller.getLightColorTemperature());
    controller.setLightColor(0xFF0000);
    controller.setLightColorMode(HomeFurnitureController.LightColorMode.COLOR);
    controller.modifyFurniture();
    assertEquals(Integer.valueOf(0xFF0000), light.getLightColor());
    assertNull(light.getLightColorTemperature());

    // Undo and redo
    undoManager.undo();
    assertEquals(Integer.valueOf(3000), light.getLightColorTemperature());
    assertNull(light.getLightColor());
    undoManager.undo();
    assertNull(light.getLightColorTemperature());
    assertNull(light.getLightColor());
    undoManager.redo();
    undoManager.redo();
    assertEquals(Integer.valueOf(0xFF0000), light.getLightColor());
    assertNull(light.getLightColorTemperature());

    // Go back to the color of the light sources
    controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertEquals(HomeFurnitureController.LightColorMode.COLOR, controller.getLightColorMode());
    controller.setLightColorMode(HomeFurnitureController.LightColorMode.DEFAULT);
    controller.modifyFurniture();
    assertNull(light.getLightColor());
    assertNull(light.getLightColorTemperature());

    // A piece which isn't a light has no light property
    home.setSelectedItems(Arrays.asList(new Selectable [] {light,
        new com.eteks.sweethome3d.model.HomePieceOfFurniture(light)}));
    controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertFalse(controller.isPropertyEditable(HomeFurnitureController.Property.LIGHT_COLOR_MODE));
  }

  public void testLightPowerUnit() {
    HomeLight light = createLight();
    assertEquals("Wrong default unit", HomeLight.PowerUnit.PERCENTAGE, light.getPowerUnit());
    assertEquals("Wrong default power", 0.5f, light.getPower());
    assertEquals("Wrong luminous flux", 400f, light.getLuminousFlux());

    Home home = new Home();
    home.addPieceOfFurniture(light);
    home.setSelectedItems(Arrays.asList(new Selectable [] {light}));
    UndoableEditSupport undoSupport = new UndoableEditSupport();
    UndoManager undoManager = new UndoManager();
    undoSupport.addUndoableEditListener(undoManager);
    HomeFurnitureController controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertEquals(HomeLight.PowerUnit.PERCENTAGE, controller.getLightPowerUnit());
    controller.setLightPowerUnit(HomeLight.PowerUnit.LUMEN);
    controller.setLightPower(600 / HomeLight.FULL_POWER_LUMINOUS_FLUX);
    controller.modifyFurniture();
    assertEquals(HomeLight.PowerUnit.LUMEN, light.getPowerUnit());
    assertEquals(0.75f, light.getPower());
    assertEquals(600f, light.getLuminousFlux());

    undoManager.undo();
    assertEquals(HomeLight.PowerUnit.PERCENTAGE, light.getPowerUnit());
    assertEquals(0.5f, light.getPower());
    undoManager.redo();
    assertEquals(HomeLight.PowerUnit.LUMEN, light.getPowerUnit());
    assertEquals(0.75f, light.getPower());

    // Lights with different units have no common unit
    HomeLight otherLight = createLight();
    home.addPieceOfFurniture(otherLight);
    home.setSelectedItems(Arrays.asList(new Selectable [] {light, otherLight}));
    controller = new HomeFurnitureController(home, this.preferences, null, undoSupport);
    assertNull(controller.getLightPowerUnit());
    assertNull(controller.getLightPower());
    // Changing only the color shouldn't change power or unit
    controller.setLightColorTemperature(5000);
    controller.setLightColorMode(HomeFurnitureController.LightColorMode.TEMPERATURE);
    controller.modifyFurniture();
    assertEquals(HomeLight.PowerUnit.LUMEN, light.getPowerUnit());
    assertEquals(0.75f, light.getPower());
    assertEquals(HomeLight.PowerUnit.PERCENTAGE, otherLight.getPowerUnit());
    assertEquals(0.5f, otherLight.getPower());
    assertEquals(Integer.valueOf(5000), light.getLightColorTemperature());
    assertEquals(Integer.valueOf(5000), otherLight.getLightColorTemperature());
  }

  public void testLightPowerUnitInPanel() throws Exception {
    final HomeLight light = createLight();
    final Home home = new Home();
    home.addPieceOfFurniture(light);
    home.setSelectedItems(Arrays.asList(new Selectable [] {light}));
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          try {
            HomeFurnitureController controller = new HomeFurnitureController(home, preferences,
                new SwingViewFactory(), new UndoableEditSupport());
            HomeFurniturePanel panel = (HomeFurniturePanel)controller.getView();
            JSpinner powerSpinner = (JSpinner)TestUtilities.getField(panel, "lightPowerSpinner");
            JComboBox unitComboBox = (JComboBox)TestUtilities.getField(panel, "lightPowerUnitComboBox");
            assertEquals(50f, ((Number)powerSpinner.getValue()).floatValue());
            assertEquals(HomeLight.PowerUnit.PERCENTAGE, unitComboBox.getSelectedItem());

            // Changing unit converts the displayed value without changing power
            unitComboBox.setSelectedItem(HomeLight.PowerUnit.LUMEN);
            assertEquals(400f, ((Number)powerSpinner.getValue()).floatValue());
            assertEquals(0.5f, controller.getLightPower());
            assertEquals(HomeLight.PowerUnit.LUMEN, controller.getLightPowerUnit());

            powerSpinner.setValue(600f);
            assertEquals(0.75f, controller.getLightPower());

            unitComboBox.setSelectedItem(HomeLight.PowerUnit.PERCENTAGE);
            assertEquals(75f, ((Number)powerSpinner.getValue()).floatValue());
            assertEquals(0.75f, controller.getLightPower());


            // Power may be greater than 100% up to 10,000 lumens
            assertEquals(1250f, ((Number)((SpinnerNumberModel)powerSpinner.getModel()).getMaximum()).floatValue());
            powerSpinner.setValue(250f);
            assertEquals(2.5f, controller.getLightPower());
            unitComboBox.setSelectedItem(HomeLight.PowerUnit.LUMEN);
            assertEquals(2000f, ((Number)powerSpinner.getValue()).floatValue());
            assertEquals(10000f, ((Number)((SpinnerNumberModel)powerSpinner.getModel()).getMaximum()).floatValue());
            powerSpinner.setValue(10000f);
            assertEquals(HomeLight.MAXIMUM_POWER, controller.getLightPower());

            controller.modifyFurniture();
            assertEquals(12.5f, light.getPower());
            assertEquals(10000f, light.getLuminousFlux());
            assertEquals(HomeLight.PowerUnit.LUMEN, light.getPowerUnit());
          } catch (Exception ex) {
            throw new RuntimeException(ex);
          }
        }
      });
  }

  public void testRenderedPower() {
    HomeLight light = createLight();
    // A light at its default power is rendered as it was with a quadratic scale
    assertEquals("Wrong rendered power", 0.5f * 0.5f, light.getRenderedPower());
    // Rendered power is proportional to power
    light.setPower(1f);
    assertEquals("Wrong rendered power", 0.5f, light.getRenderedPower());
    light.setPower(2f);
    assertEquals("Wrong rendered power", 1f, light.getRenderedPower());
    light.setPower(0f);
    assertEquals("Wrong rendered power", 0f, light.getRenderedPower());
  }
}
