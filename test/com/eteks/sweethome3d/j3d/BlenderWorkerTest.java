/*
 * BlenderWorkerTest.java 9 oct. 2026
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests the worker protocol against shell scripts standing in for Blender.
 */
public class BlenderWorkerTest extends TestCase {
  private static List<String> fake(String script) {
    return Arrays.asList("bash", "-c", script);
  }

  public void testProtocol() throws Exception {
    // Replies OK to a first command, ERR to a second one, and mixes in its own chatter
    String talkative = "echo 'Blender 5.2.1'; echo '@@SH3D READY device=HIP';"
        + "read line; echo \"got $line\"; echo '@@SH3D OK';"
        + "read line; echo 'Traceback: boom'; echo '@@SH3D ERR no such file';"
        + "read line";
    BlenderWorker worker = new BlenderWorker(fake(talkative));
    BlenderTestCheck.equal("HIP", worker.getDevice(), "device read from READY line");
    worker.send(Map.of("cmd", "load"));
    IOException error = BlenderTestCheck.thrown(IOException.class, () -> worker.send(Map.of("cmd", "render")), "ERR reply");
    BlenderTestCheck.isTrue(error.getMessage().contains("no such file"), "ERR message reported: " + error.getMessage());
    BlenderTestCheck.isTrue(error.getMessage().contains("Traceback: boom"), "Blender output reported: " + error.getMessage());
    BlenderTestCheck.isTrue(error.getMessage().contains("got {\"cmd\":\"load\"}"), "command sent as one JSON line: " + error.getMessage());
    worker.quit();
    BlenderTestCheck.isTrue(!worker.isAlive(), "process ended by quit");

    error = BlenderTestCheck.thrown(IOException.class, () -> new BlenderWorker(fake("echo 'cannot start'; exit 3")), "exit before READY");
    BlenderTestCheck.isTrue(error.getMessage().contains("cannot start"), "start failure output reported: " + error.getMessage());

    BlenderWorker dying = new BlenderWorker(fake("echo '@@SH3D READY device=CPU'; read line; echo 'Segfault'"));
    error = BlenderTestCheck.thrown(IOException.class, () -> dying.send(Map.of("cmd", "render")), "exit during command");
    BlenderTestCheck.isTrue(error.getMessage().contains("Segfault"), "crash output reported: " + error.getMessage());

    // A command blocked in another thread ends when the process is killed
    BlenderWorker hanging = new BlenderWorker(fake("echo '@@SH3D READY device=HIP'; exec sleep 60"));
    Thread killer = new Thread(() -> {
        try {
          Thread.sleep(300);
        } catch (InterruptedException ex) {
        }
        hanging.kill();
      });
    killer.start();
    long start = System.currentTimeMillis();
    BlenderTestCheck.thrown(IOException.class, () -> hanging.send(Map.of("cmd", "render")), "killed during command");
    BlenderTestCheck.isTrue(System.currentTimeMillis() - start < 5000, "kill unblocks send");
    killer.join();
    BlenderTestCheck.isTrue(!hanging.isAlive(), "process ended by kill");
  }
}
