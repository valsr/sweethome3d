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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.InputStream;

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
    final Content model = catalogPiece.getModel();
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
      BufferedImage image;
      try {
        image = ImageIO.read(in);
      } finally {
        in.close();
      }
      assertTrue("Icon too small " + image.getWidth() + "x" + image.getHeight(),
          image.getWidth() > 100 && image.getHeight() > 100);
      int transparentPixelCount = 0;
      int opaquePixelCount = 0;
      for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
          int argb = image.getRGB(x, y);
          if ((argb >>> 24) == 0) {
            transparentPixelCount++;
          } else if ((argb >>> 24) == 0xFF) {
            opaquePixelCount++;
          }
        }
      }
      int pixelCount = image.getWidth() * image.getHeight();
      // The model is drawn at the center of a transparent background
      assertEquals("Corner not transparent", 0, image.getRGB(0, 0) >>> 24);
      assertTrue("Model not drawn: " + opaquePixelCount + " opaque pixels", opaquePixelCount > pixelCount / 50);
      assertTrue("Background not transparent: " + transparentPixelCount + " transparent pixels",
          transparentPixelCount > pixelCount / 4);
    } finally {
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            frame.dispose();
          }
        });
    }
  }
}
