/*
 * BlenderJsonTest.java 9 oct. 2026
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

public class BlenderJsonTest extends TestCase {
  public void testWrite() throws Exception {
    Map<String, Object> object = new LinkedHashMap<String, Object>();
    object.put("cmd", "render");
    object.put("width", 640);
    object.put("fov", 1.5f);
    object.put("sky", null);
    object.put("on", true);
    object.put("position", new float [] {1, 2.5f, -3});
    object.put("items", Arrays.asList("a", Map.of("k", 1)));
    BlenderTestCheck.equal("{\"cmd\":\"render\",\"width\":640,\"fov\":1.5,\"sky\":null,\"on\":true,"
        + "\"position\":[1.0,2.5,-3.0],\"items\":[\"a\",{\"k\":1}]}",
        BlenderJson.write(object), "nested object");

    BlenderTestCheck.equal("\"a\\\"b\\\\c\\nd\\u0001\\u00e9\"", BlenderJson.write("a\"b\\c\nd\u0001\u00e9"),
        "string escaped to one ASCII line");

    BlenderTestCheck.thrown(IllegalArgumentException.class, () -> BlenderJson.write(Float.NaN), "NaN refused");
  }
}
