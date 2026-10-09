/*
 * BlenderSceneExporterTest.java 9 oct. 2026
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
package com.eteks.sweethome3d.j3d;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import com.eteks.sweethome3d.io.DefaultTexturesCatalog;
import com.eteks.sweethome3d.model.CatalogTexture;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomeMaterial;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;

import junit.framework.TestCase;

/**
 * Tests the files written to describe a home to Blender.
 */
public class BlenderSceneExporterTest extends TestCase {
  @SuppressWarnings("unchecked")
  public void testExport() throws Exception {
    HomeLight lamp = BlenderTestHomes.createLamp(false);
    BlenderTestCheck.isTrue(lamp != null, "default catalog has a lamp without light source materials");
    Home home = BlenderTestHomes.createRoomHome(lamp);
    home.getEnvironment().setLightColor(0xFF8040);
    home.getEnvironment().setCeillingLightColor(0);
    File folder = Files.createTempDirectory("sh3d-exporter-test").toFile();

    Map<String, Object> scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);

    String obj = new String(Files.readAllBytes(new File(folder, "scene.obj").toPath()), StandardCharsets.ISO_8859_1);
    BlenderTestCheck.isTrue(obj.contains("\nv "), "OBJ file has vertices");
    BlenderTestCheck.isTrue(obj.contains("mtllib scene.mtl"), "OBJ file references its MTL file");
    BlenderTestCheck.isTrue(new File(folder, "scene.mtl").length() > 0, "MTL file written");
    BlenderTestCheck.equal(BlenderJson.write(scene),
        new String(Files.readAllBytes(new File(folder, "scene.json").toPath()), StandardCharsets.UTF_8).trim(),
        "scene.json content");
    BlenderTestCheck.equal("scene.obj", scene.get("obj"), "OBJ file name");
    BlenderTestCheck.equal("[1.0, 0.5019608, 0.2509804]", java.util.Arrays.toString((float [])scene.get("lightColor")), "light color");
    BlenderTestCheck.equal(null, scene.get("skyTexture"), "no sky texture");

    List<Map<String, Object>> lights = (List<Map<String, Object>>)scene.get("lights");
    BlenderTestCheck.equal(lamp.getLightSources().length, lights.size(), "one light per light source of the lamp");
    for (Map<String, Object> light : lights) {
      float [] position = (float [])light.get("position");
      BlenderTestCheck.isTrue(Math.abs(position [0] - lamp.getX()) <= lamp.getWidth() / 2 + 1
          && Math.abs(position [2] - lamp.getY()) <= lamp.getDepth() / 2 + 1
          && position [1] >= lamp.getElevation() - 1
          && position [1] <= lamp.getElevation() + lamp.getHeight() + 1,
          "light source is in the box of its lamp: " + java.util.Arrays.toString(position));
      BlenderTestCheck.equal(0.5f, light.get("power"), "lamp power");
      BlenderTestCheck.isTrue((Float)light.get("radius") > 0, "light radius");
    }
    BlenderTestCheck.equal(0, ((List<?>)scene.get("emissiveMaterials")).size(), "no light source material");

    // A lamp turned off doesn't light, a ceiling light is added in rooms with a ceiling
    lamp.setPower(0);
    home.getEnvironment().setCeillingLightColor(0xD0D0D0);
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    lights = (List<Map<String, Object>>)scene.get("lights");
    BlenderTestCheck.equal(1, lights.size(), "one ceiling light");
    float [] position = (float [])lights.get(0).get("position");
    BlenderTestCheck.equal("[250.0, " + (home.getWallHeight() - 25) + ", 200.0]", java.util.Arrays.toString(position), "ceiling light location");

    // Walls and rooms made transparent in the 3D view are rendered opaque, as Sweet Home 3D renderers do
    BlenderTestCheck.equal(0, ((List<?>)scene.get("opaqueMaterials")).size(), "no material to make opaque with opaque walls");
    home.getEnvironment().setWallsAlpha(0.5f);
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    List<String> opaqueMaterials = (List<String>)scene.get("opaqueMaterials");
    BlenderTestCheck.isTrue(opaqueMaterials.size() > 0, "materials of transparent walls listed");
    String wallsMtl = new String(Files.readAllBytes(new File(folder, "scene.mtl").toPath()), StandardCharsets.ISO_8859_1);
    int transparentMaterialCount = 0;
    for (String material : opaqueMaterials) {
      int definition = wallsMtl.indexOf("newmtl " + material + "\n");
      BlenderTestCheck.isTrue(definition >= 0, "opaque material " + material + " is in MTL file");
      int end = wallsMtl.indexOf("newmtl ", definition + 1);
      if (wallsMtl.substring(definition, end < 0 ? wallsMtl.length() : end).contains("\nd 0.5")) {
        transparentMaterialCount++;
      }
    }
    BlenderTestCheck.isTrue(transparentMaterialCount > 0, "transparent material of walls listed");
    BlenderTestCheck.equal(transparentMaterialCount, wallsMtl.split("\nd 0.5", -1).length - 1, "all transparent materials listed");
    home.getEnvironment().setWallsAlpha(0);

    // Textures are written beside the MTL file which references them
    CatalogTexture texture = new DefaultTexturesCatalog().getCategories().get(0).getTextures().get(0);
    home.getRooms().get(0).setFloorTexture(new HomeTexture(texture));
    BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    String texturedMtl = new String(Files.readAllBytes(new File(folder, "scene.mtl").toPath()), StandardCharsets.ISO_8859_1);
    int textureLine = texturedMtl.indexOf("map_Kd ");
    BlenderTestCheck.isTrue(textureLine >= 0, "MTL file references a texture image");
    String textureFile = texturedMtl.substring(textureLine + "map_Kd ".length(), texturedMtl.indexOf('\n', textureLine)).trim();
    BlenderTestCheck.isTrue(new File(folder, textureFile).length() > 0, "texture image " + textureFile + " written in export folder");

    // Lamps with light source materials emit light from these materials instead of point lights
    HomeLight materialLamp = BlenderTestHomes.createLamp(true);
    BlenderTestCheck.isTrue(materialLamp != null, "default catalog has a lamp with a named material");
    {
      Home materialHome = BlenderTestHomes.createRoomHome(materialLamp);
      materialHome.getEnvironment().setCeillingLightColor(0);
      scene = BlenderSceneExporter.export(materialHome, new Object3DBranchFactory(), folder);
      BlenderTestCheck.equal(0, ((List<?>)scene.get("lights")).size(), "no point light for a lamp with light source materials");
      List<Map<String, Object>> emissive = (List<Map<String, Object>>)scene.get("emissiveMaterials");
      BlenderTestCheck.isTrue(emissive.size() > 0, "light source materials listed");
      String mtl = new String(Files.readAllBytes(new File(folder, "scene.mtl").toPath()), StandardCharsets.ISO_8859_1);
      for (Map<String, Object> material : emissive) {
        BlenderTestCheck.isTrue(mtl.contains("newmtl " + material.get("name") + "\n"), "emissive material " + material.get("name") + " is in MTL file");
        BlenderTestCheck.equal(0.5f, material.get("power"), "emissive material power");
      }
    }
    // Hidden ceilings and levels are written apart to block light when asked, and only then
    {
      Object3DBranchFactory factory = new Object3DBranchFactory();
      Home twoLevelHome = BlenderTestHomes.createTwoLevelHome();
      Level upper = twoLevelHome.getLevels().get(1);
      Room groundRoom = twoLevelHome.getRooms().get(0);
      File occludersFile = new File(folder, "occluders.obj");

      scene = BlenderSceneExporter.export(twoLevelHome, factory, folder);
      BlenderTestCheck.isTrue(!scene.containsKey("occluders") && !scene.containsKey("occludersBlock"), "no occluders unless asked");
      BlenderTestCheck.isTrue(!occludersFile.exists(), "no occluders file unless asked");
      byte [] sceneObj = Files.readAllBytes(new File(folder, "scene.obj").toPath());
      String sceneLights = BlenderJson.write(scene.get("lights"));

      scene = BlenderSceneExporter.export(twoLevelHome, factory, folder, "all");
      BlenderTestCheck.equal("occluders.obj", scene.get("occluders"), "occluders file name");
      BlenderTestCheck.equal("all", scene.get("occludersBlock"), "occluders block all light");
      BlenderTestCheck.equal(6, countOccluders(occludersFile), "ground ceiling, 4 upper walls and upper room block light");
      BlenderTestCheck.equal("sun", BlenderSceneExporter.export(twoLevelHome, factory, folder, "sun").get("occludersBlock"),
          "occluders block the sun only");
      BlenderTestCheck.equal(6, countOccluders(occludersFile), "same occluders whatever they block");

      // Values of the rendering parameter
      BlenderTestCheck.equal(null, BlenderSceneExporter.getOccludersBlock("false"), "parameter false");
      BlenderTestCheck.equal("all", BlenderSceneExporter.getOccludersBlock("all"), "parameter all");
      BlenderTestCheck.equal("all", BlenderSceneExporter.getOccludersBlock("true"), "parameter true");
      BlenderTestCheck.equal("sun", BlenderSceneExporter.getOccludersBlock(" Sun "), "parameter sun");
      BlenderTestCheck.thrown(IllegalArgumentException.class, () -> BlenderSceneExporter.getOccludersBlock("moon"), "unknown parameter value");
      BlenderTestCheck.isTrue(java.util.Arrays.equals(sceneObj, Files.readAllBytes(new File(folder, "scene.obj").toPath())),
          "visible items are written the same with occluders");
      BlenderTestCheck.equal(sceneLights, BlenderJson.write(scene.get("lights")), "occluders add no light");
      BlenderTestCheck.isTrue(!upper.isVisible() && !groundRoom.isCeilingVisible() && groundRoom.isFloorVisible(),
          "home unchanged by the export of occluders");

      BlenderSceneExporter.export(twoLevelHome, factory, folder, null);
      BlenderTestCheck.isTrue(!occludersFile.exists(), "occluders file of a previous export removed");

      // A visible ceiling is written once, with the visible items
      groundRoom.setCeilingVisible(true);
      BlenderSceneExporter.export(twoLevelHome, factory, folder, "all");
      BlenderTestCheck.equal(5, countOccluders(occludersFile), "visible ceiling isn't an occluder");
      groundRoom.setCeilingVisible(false);

      // A level which isn't viewable stays out of the scene
      upper.setViewable(false);
      BlenderSceneExporter.export(twoLevelHome, factory, folder, "all");
      BlenderTestCheck.equal(1, countOccluders(occludersFile), "only the ground ceiling when upper level isn't viewable");

      // A hidden level without anything to stop light gives no occluders file, which Blender couldn't read
      Home emptyLevelHome = BlenderTestHomes.createTwoLevelHome();
      emptyLevelHome.getRooms().get(0).setCeilingVisible(true);
      for (Wall wall : new java.util.ArrayList<Wall>(emptyLevelHome.getWalls())) {
        if (wall.getLevel() == emptyLevelHome.getLevels().get(1)) {
          emptyLevelHome.deleteWall(wall);
        }
      }
      emptyLevelHome.deleteRoom(emptyLevelHome.getRooms().get(1));
      scene = BlenderSceneExporter.export(emptyLevelHome, factory, folder, "all");
      BlenderTestCheck.isTrue(!scene.containsKey("occluders") && !occludersFile.exists(), "empty hidden level, no occluders");

      Home roomHome = BlenderTestHomes.createRoomHome(null);
      scene = BlenderSceneExporter.export(roomHome, factory, folder, "all");
      BlenderTestCheck.isTrue(!scene.containsKey("occluders") && !occludersFile.exists(), "nothing hidden, no occluders");
      roomHome.getRooms().get(0).setCeilingVisible(false);
      BlenderSceneExporter.export(roomHome, factory, folder, "all");
      BlenderTestCheck.equal(1, countOccluders(occludersFile), "hidden ceiling of a home without levels");
    }
  }

  /**
   * Tests the color and the power of lamps are exported as chosen by the user.
   */
  @SuppressWarnings("unchecked")
  public void testLightColorAndPower() throws Exception {
    File folder = Files.createTempDirectory("sh3d-exporter-test").toFile();
    HomeLight lamp = BlenderTestHomes.createLamp(false);
    Home home = BlenderTestHomes.createRoomHome(lamp);
    home.getEnvironment().setCeillingLightColor(0);
    int sourceColor = lamp.getLightSources() [0].getColor();

    Map<String, Object> scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    Map<String, Object> light = ((List<Map<String, Object>>)scene.get("lights")).get(0);
    BlenderTestCheck.equal(java.util.Arrays.toString(BlenderSceneExporter.getColor(sourceColor)),
        java.util.Arrays.toString((float [])light.get("color")), "color of the light source by default");

    lamp.setLightColor(0xFF0000);
    lamp.setPower(2.5f);
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    light = ((List<Map<String, Object>>)scene.get("lights")).get(0);
    BlenderTestCheck.equal("[1.0, 0.0, 0.0]", java.util.Arrays.toString((float [])light.get("color")), "chosen color");
    BlenderTestCheck.equal(2.5f, light.get("power"), "power greater than 100%");

    lamp.setLightColorTemperature(2700);
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    light = ((List<Map<String, Object>>)scene.get("lights")).get(0);
    BlenderTestCheck.equal(java.util.Arrays.toString(BlenderSceneExporter.getColor(HomeLight.getColorAtTemperature(2700))),
        java.util.Arrays.toString((float [])light.get("color")), "color of the chosen temperature");

    // The chosen color replaces the color of light source materials too
    HomeLight materialLamp = BlenderTestHomes.createLamp(true);
    Home materialHome = BlenderTestHomes.createRoomHome(materialLamp);
    scene = BlenderSceneExporter.export(materialHome, new Object3DBranchFactory(), folder);
    Map<String, Object> material = ((List<Map<String, Object>>)scene.get("emissiveMaterials")).get(0);
    BlenderTestCheck.isTrue(!material.containsKey("color"), "color of the material kept by default");
    materialLamp.setLightColor(0x00FF00);
    scene = BlenderSceneExporter.export(materialHome, new Object3DBranchFactory(), folder);
    material = ((List<Map<String, Object>>)scene.get("emissiveMaterials")).get(0);
    BlenderTestCheck.equal("[0.0, 1.0, 0.0]", java.util.Arrays.toString((float [])material.get("color")),
        "chosen color of a light source material");

    // A ceiling light gets a power proportional to the side of its room
    Home ceilingLightHome = BlenderTestHomes.createRoomHome(null);
    ceilingLightHome.getEnvironment().setCeillingLightColor(0xD0D0D0);
    scene = BlenderSceneExporter.export(ceilingLightHome, new Object3DBranchFactory(), folder);
    light = ((List<Map<String, Object>>)scene.get("lights")).get(0);
    BlenderTestCheck.isTrue(Math.abs((Float)light.get("power") - (float)Math.sqrt(500 * 400) / 1000) < 1E-5f,
        "ceiling light power " + light.get("power"));
  }

  /**
   * Tests the materials which opacity was chosen by the user are listed apart from the ones of glass.
   */
  @SuppressWarnings("unchecked")
  public void testChosenOpacity() throws Exception {
    File folder = Files.createTempDirectory("sh3d-exporter-test").toFile();
    Home home = BlenderTestHomes.createRoomHome(null);
    HomePieceOfFurniture piece = home.getFurniture().get(0);
    Map<String, Object> scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    BlenderTestCheck.equal(0, ((List<?>)scene.get("translucentMaterials")).size(), "no chosen opacity by default");

    String materialName = BlenderTestHomes.getFirstAppearanceName(
        (javax.media.j3d.Node)new Object3DBranchFactory().createObject3D(home, piece, true));
    // A color without opacity changes nothing
    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, 0xFF2040C0, null, null)});
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    BlenderTestCheck.equal(0, ((List<?>)scene.get("translucentMaterials")).size(), "no chosen opacity with a color");

    piece.setModelMaterials(new HomeMaterial [] {new HomeMaterial(materialName, null, 0xFF2040C0, null, null, 0.25f)});
    scene = BlenderSceneExporter.export(home, new Object3DBranchFactory(), folder);
    List<String> translucentMaterials = (List<String>)scene.get("translucentMaterials");
    BlenderTestCheck.equal(1, translucentMaterials.size(), "material with a chosen opacity listed");
    String mtl = new String(Files.readAllBytes(new File(folder, "scene.mtl").toPath()), StandardCharsets.ISO_8859_1);
    int definition = mtl.indexOf("newmtl " + translucentMaterials.get(0) + "\n");
    BlenderTestCheck.isTrue(definition >= 0, "translucent material " + translucentMaterials.get(0) + " is in MTL file");
    int end = mtl.indexOf("newmtl ", definition + 1);
    BlenderTestCheck.isTrue(mtl.substring(definition, end < 0 ? mtl.length() : end).contains("\nd 0.25"),
        "translucent material written with its opacity");
  }

  /**
   * Returns the count of items written in the OBJ file of occluders.
   */
  private static int countOccluders(File occludersFile) throws Exception {
    java.util.Set<String> names = new java.util.HashSet<String>();
    for (String line : Files.readAllLines(occludersFile.toPath(), StandardCharsets.ISO_8859_1)) {
      if (line.startsWith("g occluder")) {
        int separator = line.indexOf('_');
        names.add(separator > 0 ? line.substring(2, separator) : line.substring(2));
      }
    }
    return names.size();
  }
}
