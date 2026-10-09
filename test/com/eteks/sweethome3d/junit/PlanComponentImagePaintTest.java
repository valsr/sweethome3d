/*
 * PlanComponentImagePaintTest.java 9 oct. 2026
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
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InterruptedIOException;
import java.util.Arrays;

import javax.swing.JFrame;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.ObserverCamera;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.PlanComponent;

/**
 * Tests the plan painted through an intermediate image gives the same result as the plan painted directly.
 */
public class PlanComponentImagePaintTest extends TestCase {
  private static final String PLAN_PAINTED_IN_IMAGE_PROPERTY = "com.eteks.sweethome3d.swing.planPaintedInImage";

  private Home          home;
  private PlanComponent plan;
  private JFrame        frame;
  private Rectangle     itemsGraphicsDeviceClip;

  @Override
  protected void setUp() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          home = new Home();
          home.addWall(new Wall(0, 0, 2000, 0, 10, 250));
          home.addWall(new Wall(2000, 0, 2000, 1500, 10, 250));
          home.addWall(new Wall(2000, 1500, 0, 1500, 10, 250));
          home.addWall(new Wall(0, 1500, 0, 0, 10, 250));
          Room room = new Room(new float [][] {{0, 0}, {2000, 0}, {2000, 1500}, {0, 1500}});
          room.setName("Living room");
          room.setAreaVisible(true);
          room.setFloorColor(0xFFC0A080);
          home.addRoom(room);
          ObserverCamera camera = home.getObserverCamera();
          camera.setX(1000);
          camera.setY(750);
          home.setCamera(camera);
          home.setSelectedItems(Arrays.asList(new Selectable [] {room}));

          plan = new PlanComponent(home, new DefaultUserPreferences(), null) {
              @Override
              protected void paintHomeItems(Graphics g, Level level, float planScale, Color backgroundColor,
                                            Color foregroundColor, PaintMode paintMode) throws InterruptedIOException {
                // Store the clipped area in the coordinates of the image where items are painted
                Graphics2D g2D = (Graphics2D)g;
                itemsGraphicsDeviceClip = g2D.getTransform().createTransformedShape(g2D.getClip()).getBounds();
                super.paintHomeItems(g, level, planScale, backgroundColor, foregroundColor, paintMode);
              }
            };
          frame = new JFrame();
          frame.add(plan);
          frame.pack();
        }
      });
  }

  @Override
  protected void tearDown() throws Exception {
    System.clearProperty(PLAN_PAINTED_IN_IMAGE_PROPERTY);
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          frame.dispose();
        }
      });
  }

  /**
   * Tests home items are painted in an intermediate image of the size of the clip when requested.
   */
  public void testPlanPaintedInImage() throws Exception {
    Rectangle clip = new Rectangle(150, 120, 200, 160);
    System.setProperty(PLAN_PAINTED_IN_IMAGE_PROPERTY, "true");
    paintPlan(clip, 1);
    assertEquals("Items should be painted in an image of clip size", new Rectangle(clip.getSize()), this.itemsGraphicsDeviceClip);

    System.setProperty(PLAN_PAINTED_IN_IMAGE_PROPERTY, "false");
    paintPlan(clip, 1);
    assertEquals("Items should be painted directly", clip, this.itemsGraphicsDeviceClip);
  }

  /**
   * Tests the plan fully painted through an image is the same as the plan painted directly.
   */
  public void testFullPaintInImageSameAsDirectPaint() throws Exception {
    assertPaintInImageSameAsDirectPaint(null, 1);
  }

  /**
   * Tests a clipped area of the plan painted through an image is the same as the plan painted directly.
   */
  public void testClippedPaintInImageSameAsDirectPaint() throws Exception {
    assertPaintInImageSameAsDirectPaint(new Rectangle(431, 257, 333, 211), 1);
    // Clip partially out of the component
    assertPaintInImageSameAsDirectPaint(new Rectangle(-50, -30, 300, 250), 1);
    assertPaintInImageSameAsDirectPaint(
        new Rectangle(this.plan.getWidth() - 120, this.plan.getHeight() - 90, 400, 400), 1);
  }

  /**
   * Tests the plan painted through an image on a high resolution screen is the same as the plan painted directly.
   */
  public void testScaledPaintInImageSameAsDirectPaint() throws Exception {
    assertPaintInImageSameAsDirectPaint(null, 2);
    assertPaintInImageSameAsDirectPaint(new Rectangle(431, 257, 333, 211), 2);
  }

  private void assertPaintInImageSameAsDirectPaint(Rectangle clip, int screenScale) throws Exception {
    System.setProperty(PLAN_PAINTED_IN_IMAGE_PROPERTY, "false");
    BufferedImage directImage = paintPlan(clip, screenScale);
    System.setProperty(PLAN_PAINTED_IN_IMAGE_PROPERTY, "true");
    BufferedImage image = paintPlan(clip, screenScale);
    int paintedPixelCount = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (!isSameColor(image.getRGB(x, y), directImage.getRGB(x, y))) {
          fail("Pixel at " + x + "," + y + " differs with clip " + clip + " and scale " + screenScale);
        }
        if (image.getRGB(x, y) != 0xFFFF00FF) {
          paintedPixelCount++;
        }
      }
    }
    assertTrue("Plan should be painted", paintedPixelCount > 1000);
  }

  /**
   * Returns an image filled in magenta, in which the plan is painted in the given <code>clip</code>
   * for a screen at the given scale.
   */
  private BufferedImage paintPlan(final Rectangle clip, final int screenScale) throws Exception {
    final BufferedImage image = new BufferedImage(
        this.plan.getWidth() * screenScale, this.plan.getHeight() * screenScale, BufferedImage.TYPE_INT_RGB);
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          Graphics2D g2D = image.createGraphics();
          g2D.setColor(Color.MAGENTA);
          g2D.fillRect(0, 0, image.getWidth(), image.getHeight());
          g2D.scale(screenScale, screenScale);
          if (clip != null) {
            g2D.setClip(clip);
          }
          plan.paint(g2D);
          g2D.dispose();
        }
      });
    return image;
  }

  /**
   * Returns <code>true</code> if the two given colors are the same, ignoring the rounding differences
   * of antialiasing that may appear when a shape isn't drawn at the same location in two images.
   */
  private static boolean isSameColor(int rgb1, int rgb2) {
    for (int shift = 0; shift <= 16; shift += 8) {
      if (Math.abs(((rgb1 >> shift) & 0xFF) - ((rgb2 >> shift) & 0xFF)) > 2) {
        return false;
      }
    }
    return true;
  }
}
