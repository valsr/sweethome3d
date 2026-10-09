/*
 * ModelPreviewComponentIconTest.java 9 oct. 2026
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.CatalogLight;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.ModelPreviewComponent;
import com.eteks.sweethome3d.tools.URLContent;

/**
 * Tests the icon created from the view displayed by a
 * {@link com.eteks.sweethome3d.swing.ModelPreviewComponent model preview component}.
 */
public class ModelPreviewComponentIconTest extends TestCase {
  private ModelPreviewComponent previewComponent;
  private JFrame                frame;
  private Content               icon;
  private Exception             iconException;

  @Override
  protected void setUp() throws Exception {
    // Initialize Java 3D first as the application does with its 3D view, to avoid a deadlock between
    // the event dispatch thread and the thread loading models if both initialize it at the same time
    new javax.media.j3d.BranchGroup();
  }

  public void testIcon() throws Exception {
    UserPreferences preferences = new DefaultUserPreferences();
    CatalogPieceOfFurniture catalogPiece = null;
    for (FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (catalogPiece == null && !(piece instanceof CatalogLight) && !piece.isDoorOrWindow()) {
          catalogPiece = piece;
        }
      }
    }
    BufferedImage image = createIconImage(catalogPiece.getModel());
    int [] pixelCounts = countPixels(image);
    int pixelCount = image.getWidth() * image.getHeight();
    // The model is drawn at the center of a transparent background
    assertEquals("Corner not transparent", 0, image.getRGB(0, 0) >>> 24);
    assertTrue("Model not drawn: " + pixelCounts [1] + " opaque pixels", pixelCounts [1] > pixelCount / 50);
    assertTrue("Background not transparent: " + pixelCounts [0] + " transparent pixels",
        pixelCounts [0] > pixelCount / 4);
  }

  /**
   * Tests the icon of a model with a texture smaller than its icon. Java 3D named the first texture
   * of an on screen canvas like the texture where JOGL renders an off screen canvas, which limited
   * the image of the latter to the size of the texture of the model, the other pixels being black.
   */
  public void testTexturedModelIcon() throws Exception {
    File modelFolder = Files.createTempDirectory("texturedModel").toFile();
    File objFile = new File(modelFolder, "box.obj");
    File mtlFile = new File(modelFolder, "box.mtl");
    File textureFile = new File(modelFolder, "orange.png");
    try {
      // Write an orange image much wider than high
      BufferedImage texture = new BufferedImage(64, 16, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2D = texture.createGraphics();
      g2D.setColor(new Color(0xFF8000));
      g2D.fillRect(0, 0, texture.getWidth(), texture.getHeight());
      g2D.dispose();
      ImageIO.write(texture, "png", textureFile);
      Files.write(mtlFile.toPath(), ("newmtl orange\n"
          + "Kd 1 1 1\n"
          + "map_Kd orange.png\n").getBytes("ISO-8859-1"));
      // Write a box with all its faces textured
      Files.write(objFile.toPath(), ("mtllib box.mtl\n"
          + "v 0 0 0\nv 1 0 0\nv 1 1 0\nv 0 1 0\n"
          + "v 0 0 1\nv 1 0 1\nv 1 1 1\nv 0 1 1\n"
          + "vt 0 0\nvt 1 0\nvt 1 1\nvt 0 1\n"
          + "g box\n"
          + "usemtl orange\n"
          + "f 4/1 3/2 2/3 1/4\n"
          + "f 5/1 6/2 7/3 8/4\n"
          + "f 1/1 2/2 6/3 5/4\n"
          + "f 2/1 3/2 7/3 6/4\n"
          + "f 3/1 4/2 8/3 7/4\n"
          + "f 4/1 1/2 5/3 8/4\n").getBytes("ISO-8859-1"));

      BufferedImage image = createIconImage(new URLContent(objFile.toURI().toURL()));
      int [] pixelCounts = countPixels(image);
      int pixelCount = image.getWidth() * image.getHeight();
      assertEquals("Corner not transparent", 0, image.getRGB(0, 0) >>> 24);
      assertTrue("Model not drawn: " + pixelCounts [1] + " opaque pixels", pixelCounts [1] > pixelCount / 50);
      assertEquals("Black pixels in the icon of an orange model", 0, pixelCounts [2]);
      // The top of the box is visible in the upper half of the icon
      int upperOpaquePixelCount = 0;
      for (int y = 0; y < image.getHeight() / 2; y++) {
        for (int x = 0; x < image.getWidth(); x++) {
          if ((image.getRGB(x, y) >>> 24) == 0xFF) {
            upperOpaquePixelCount++;
          }
        }
      }
      assertTrue("Model not drawn in the upper half of the icon", upperOpaquePixelCount > pixelCount / 200);
    } finally {
      textureFile.delete();
      mtlFile.delete();
      objFile.delete();
      modelFolder.delete();
    }
  }

  /**
   * Returns the count of transparent pixels, opaque pixels and opaque black pixels of <code>image</code>.
   */
  private int [] countPixels(BufferedImage image) {
    int [] pixelCounts = new int [3];
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y);
        if ((argb >>> 24) == 0) {
          pixelCounts [0]++;
        } else if ((argb >>> 24) == 0xFF) {
          pixelCounts [1]++;
          if ((argb & 0xFFFFFF) == 0) {
            pixelCounts [2]++;
          }
        }
      }
    }
    return pixelCounts;
  }

  /**
   * Returns the image of the icon created by a preview component displaying <code>model</code>.
   */
  private BufferedImage createIconImage(final Content model) throws Exception {
    this.iconException = null;
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          previewComponent = new ModelPreviewComponent(true);
          previewComponent.setPreferredSize(new Dimension(200, 200));
          previewComponent.setModel(model);
          frame = new JFrame("Model preview test");
          frame.add(previewComponent);
          frame.pack();
          frame.setVisible(true);
        }
      });
    try {
      // Wait for the model to be loaded and displayed
      Thread.sleep(2000);
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            try {
              icon = previewComponent.getIcon(400);
            } catch (Exception ex) {
              iconException = ex;
            }
          }
        });
      if (this.iconException != null) {
        throw this.iconException;
      }

      InputStream in = this.icon.openStream();
      try {
        BufferedImage image = ImageIO.read(in);
        assertTrue("Icon too small " + image.getWidth() + "x" + image.getHeight(),
            image.getWidth() > 100 && image.getHeight() > 100);
        return image;
      } finally {
        in.close();
      }
    } finally {
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            frame.dispose();
          }
        });
    }
  }
}
