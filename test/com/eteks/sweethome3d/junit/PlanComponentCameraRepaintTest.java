/*
 * PlanComponentCameraRepaintTest.java 9 oct. 2026
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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.ObserverCamera;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.SelectionEvent;
import com.eteks.sweethome3d.model.SelectionListener;
import com.eteks.sweethome3d.model.Wall;
import com.eteks.sweethome3d.swing.PlanComponent;
import com.eteks.sweethome3d.viewcontroller.HomeController3D;

/**
 * Tests the area of the plan repainted and the selection changes notified when
 * the observer camera changes, to ensure navigating in the 3D view doesn't repaint the whole plan.
 */
public class PlanComponentCameraRepaintTest extends TestCase {
  private Home            home;
  private PlanComponent   plan;
  private JFrame          frame;
  private List<Rectangle> repaintedAreas;

  @Override
  protected void setUp() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          home = new Home();
          // Build a 20 m x 15 m home where the observer camera stands in the middle
          home.addWall(new Wall(0, 0, 2000, 0, 10, 250));
          home.addWall(new Wall(2000, 0, 2000, 1500, 10, 250));
          home.addWall(new Wall(2000, 1500, 0, 1500, 10, 250));
          home.addWall(new Wall(0, 1500, 0, 0, 10, 250));
          ObserverCamera camera = home.getObserverCamera();
          camera.setX(1000);
          camera.setY(750);
          home.setCamera(camera);

          repaintedAreas = new ArrayList<Rectangle>();
          plan = new PlanComponent(home, new DefaultUserPreferences(), null) {
              @Override
              public void repaint(long tm, int x, int y, int width, int height) {
                repaintedAreas.add(new Rectangle(x, y, width, height));
                super.repaint(tm, x, y, width, height);
              }
            };
          frame = new JFrame();
          frame.add(plan);
          frame.pack();
        }
      });
    flushEvents();
  }

  @Override
  protected void tearDown() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          frame.dispose();
        }
      });
  }

  /**
   * Tests rotating and moving the camera inside the plan repaints only the camera area.
   */
  public void testCameraChangeInsidePlanRepaintsCameraAreaOnly() throws Exception {
    final Dimension planSize = this.plan.getSize();
    assertTrue("Plan should be large", planSize.width > 500 && planSize.height > 400);

    Rectangle repaintedArea = changeCamera(new Runnable() {
        public void run() {
          home.getObserverCamera().setYaw(home.getObserverCamera().getYaw() + 0.3f);
        }
      });
    assertRepaintedAreaLimitedToCamera("Yaw change", repaintedArea, planSize);

    Rectangle movedRepaintedArea = changeCamera(new Runnable() {
        public void run() {
          home.getObserverCamera().setX(1050);
        }
      });
    assertRepaintedAreaLimitedToCamera("Move", movedRepaintedArea, planSize);
    assertEquals("Plan size shouldn't change", planSize, this.plan.getSize());
  }

  /**
   * Tests the area repainted after a change of the selected camera covers all the pixels that changed.
   */
  public void testRepaintedAreaCoversCameraChanges() throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          // Select camera to display its indicators too
          home.setSelectedItems(Arrays.asList(new Selectable [] {home.getObserverCamera()}));
        }
      });
    flushEvents();
    final BufferedImage image = new BufferedImage(this.plan.getWidth(), this.plan.getHeight(), BufferedImage.TYPE_INT_RGB);
    paintPlan(image, null);
    Runnable [] changes = {
        new Runnable() {
          public void run() {
            home.getObserverCamera().setYaw(home.getObserverCamera().getYaw() + 2.5f);
          }
        },
        new Runnable() {
          public void run() {
            home.getObserverCamera().setX(home.getObserverCamera().getX() + 130);
            home.getObserverCamera().setY(home.getObserverCamera().getY() - 90);
          }
        },
        new Runnable() {
          public void run() {
            home.getObserverCamera().setFieldOfView((float)Math.toRadians(100));
          }
        }};
    for (Runnable change : changes) {
      Rectangle repaintedArea = changeCamera(change);
      assertNotNull("Camera area should be repainted", repaintedArea);
      // Repaint only the repainted area in the image and compare it to a full paint
      paintPlan(image, repaintedArea);
      BufferedImage expectedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
      paintPlan(expectedImage, null);
      for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
          if (!isSameColor(image.getRGB(x, y), expectedImage.getRGB(x, y))) {
            fail("Pixel at " + x + "," + y + " not updated by repainted area " + repaintedArea);
          }
        }
      }
    }
  }

  /**
   * Paints the plan in the given image, clipped to <code>clip</code> if it's not <code>null</code>.
   */
  private void paintPlan(final BufferedImage image, final Rectangle clip) throws Exception {
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          Graphics2D g2D = image.createGraphics();
          if (clip != null) {
            g2D.setClip(clip);
          }
          plan.paint(g2D);
          g2D.dispose();
        }
      });
  }

  /**
   * Tests moving the camera out of plan bounds still enlarges the plan.
   */
  public void testCameraMovedOutOfPlanEnlargesPlan() throws Exception {
    Dimension planSize = this.plan.getPreferredSize();
    changeCamera(new Runnable() {
        public void run() {
          home.getObserverCamera().setX(5000);
        }
      });
    assertTrue("Plan should be enlarged to show camera",
        this.plan.getPreferredSize().width > planSize.width);
  }

  /**
   * Tests navigating with the 3D view controller selects the observer camera once,
   * and doesn't notify a selection change again at each camera change.
   */
  public void testCameraNavigationDoesntRepeatSelection() throws Exception {
    final int [] selectionChangeCount = {0};
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          home.setSelectedItems(new ArrayList<Selectable>());
          HomeController3D controller = new HomeController3D(home, new DefaultUserPreferences(), null, null, null);
          home.addSelectionListener(new SelectionListener() {
              public void selectionChanged(SelectionEvent ev) {
                selectionChangeCount [0]++;
              }
            });
          controller.rotateCameraYaw(0.1f);
          assertEquals("Camera should be selected", Arrays.asList(new Object [] {home.getObserverCamera()}), home.getSelectedItems());
          selectionChangeCount [0] = 0;
          controller.rotateCameraYaw(0.1f);
          controller.rotateCameraPitch(0.05f);
          controller.moveCamera(10);
        }
      });
    assertEquals("Selection shouldn't be notified again while camera is already selected", 0, selectionChangeCount [0]);
    assertEquals("Camera should still be selected", Arrays.asList(new Object [] {this.home.getObserverCamera()}), this.home.getSelectedItems());
  }

  /**
   * Runs the given camera change in the EDT and returns the union of the areas for which a repaint was requested.
   */
  private Rectangle changeCamera(final Runnable change) throws Exception {
    final Rectangle [] repaintedArea = {null};
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          repaintedAreas.clear();
          change.run();
        }
      });
    flushEvents();
    EventQueue.invokeAndWait(new Runnable() {
        public void run() {
          for (Rectangle area : repaintedAreas) {
            repaintedArea [0] = repaintedArea [0] == null  ? area  : repaintedArea [0].union(area);
          }
        }
      });
    return repaintedArea [0];
  }

  private void assertRepaintedAreaLimitedToCamera(String message, Rectangle repaintedArea, Dimension planSize) {
    assertNotNull(message + ": camera area should be repainted", repaintedArea);
    ObserverCamera camera = this.home.getObserverCamera();
    Rectangle planArea = new Rectangle(planSize);
    // Check the camera is in the repainted area
    float [][] cameraPoints = camera.getPoints();
    for (float [] point : cameraPoints) {
      assertTrue(message + ": repainted area " + repaintedArea + " should contain camera",
          this.plan.convertXPixelToModel(repaintedArea.x) <= point [0]
          && this.plan.convertXPixelToModel(repaintedArea.x + repaintedArea.width) >= point [0]
          && this.plan.convertYPixelToModel(repaintedArea.y) <= point [1]
          && this.plan.convertYPixelToModel(repaintedArea.y + repaintedArea.height) >= point [1]);
    }
    Rectangle visibleRepaintedArea = repaintedArea.intersection(planArea);
    assertTrue(message + ": repainted area " + repaintedArea + " should be much smaller than plan " + planSize,
        (long)visibleRepaintedArea.width * visibleRepaintedArea.height < (long)planSize.width * planSize.height / 4);
  }

  private void flushEvents() throws InterruptedException, InvocationTargetException {
    // Wait for the events posted by the previous ones to be dispatched
    for (int i = 0; i < 3; i++) {
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
          }
        });
    }
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
