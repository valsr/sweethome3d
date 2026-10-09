/*
 * BlenderTestCheck.java 9 oct. 2026
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

import junit.framework.AssertionFailedError;

/**
 * Assertions described by a message given as their last parameter.
 */
final class BlenderTestCheck {
  private BlenderTestCheck() {
  }

  static void equal(Object expected, Object actual, String what) {
    if (expected == null ? actual != null : !expected.equals(actual)) {
      throw new AssertionFailedError(what + ": expected <" + expected + "> but was <" + actual + ">");
    }
  }

  static void isTrue(boolean condition, String what) {
    if (!condition) {
      throw new AssertionFailedError(what);
    }
  }

  interface Failing {
    void run() throws Exception;
  }

  /**
   * Returns the exception of class <code>type</code> thrown by <code>code</code> or fails.
   */
  static <T extends Throwable> T thrown(Class<T> type, Failing code, String what) {
    try {
      code.run();
    } catch (Throwable ex) {
      if (type.isInstance(ex)) {
        return type.cast(ex);
      }
      throw new AssertionFailedError(what + ": expected " + type.getSimpleName() + " but got " + ex);
    }
    throw new AssertionFailedError(what + ": expected " + type.getSimpleName() + " but nothing was thrown");
  }
}
