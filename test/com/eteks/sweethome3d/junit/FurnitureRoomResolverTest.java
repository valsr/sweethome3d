/*
 * FurnitureRoomResolverTest.java 15 oct. 2026
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.CatalogDoorOrWindow;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeDoorOrWindow;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.viewcontroller.FurnitureRoomResolver;
import com.eteks.sweethome3d.viewcontroller.FurnitureRoomResolver.Bucket;

import junit.framework.TestCase;

/**
 * Tests the assignment of furniture to level and room buckets.
 */
public class FurnitureRoomResolverTest extends TestCase {
  private CatalogPieceOfFurniture catalogPiece;
  private CatalogDoorOrWindow     catalogDoorOrWindow;

  @Override
  protected void setUp() {
    for (FurnitureCategory category : new DefaultUserPreferences().getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (piece instanceof CatalogDoorOrWindow) {
          if (this.catalogDoorOrWindow == null) {
            this.catalogDoorOrWindow = (CatalogDoorOrWindow)piece;
          }
        } else if (this.catalogPiece == null) {
          this.catalogPiece = piece;
        }
      }
    }
  }

  private HomePieceOfFurniture createPiece(Home home, float x, float y, Level level) {
    HomePieceOfFurniture piece = new HomePieceOfFurniture(this.catalogPiece);
    piece.setX(x);
    piece.setY(y);
    home.addPieceOfFurniture(piece);
    if (level != null) {
      piece.setLevel(level);
    }
    return piece;
  }

  private static Room createRoom(Home home, String name, float x0, float y0, float x1, float y1, Level level) {
    Room room = new Room(new float [][] {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}});
    room.setName(name);
    home.addRoom(room);
    if (level != null) {
      room.setLevel(level);
    }
    return room;
  }

  private static Bucket resolve(Home home, HomePieceOfFurniture piece) {
    return FurnitureRoomResolver.resolve(home, Arrays.asList(piece)).get(piece);
  }

  public void testPieceCenterInsideRoom() {
    Home home = new Home();
    Room room = createRoom(home, "Kitchen", 0, 0, 100, 100, null);
    HomePieceOfFurniture piece = createPiece(home, 50, 50, null);
    Bucket bucket = resolve(home, piece);
    assertEquals(Bucket.Kind.ROOM, bucket.getKind());
    assertSame(room, bucket.getRoom());
  }

  public void testOverlappingRoomsChooseSmallest() {
    Home home = new Home();
    createRoom(home, "Big", 0, 0, 400, 400, null);
    Room small = createRoom(home, "Small", 0, 0, 100, 100, null);
    HomePieceOfFurniture piece = createPiece(home, 50, 50, null);
    assertSame(small, resolve(home, piece).getRoom());
  }

  public void testDoorOrWindowBucket() {
    Home home = new Home();
    createRoom(home, "Kitchen", 0, 0, 100, 100, null);
    HomeDoorOrWindow door = new HomeDoorOrWindow(this.catalogDoorOrWindow);
    door.setX(50);
    door.setY(50);
    home.addPieceOfFurniture(door);
    Bucket bucket = resolve(home, door);
    assertEquals(Bucket.Kind.DOORS_AND_WINDOWS, bucket.getKind());
    assertNull(bucket.getRoom());
  }

  public void testNoRoomBucket() {
    Home home = new Home();
    createRoom(home, "Kitchen", 0, 0, 100, 100, null);
    HomePieceOfFurniture piece = createPiece(home, 500, 500, null);
    assertEquals(Bucket.Kind.NO_ROOM, resolve(home, piece).getKind());
  }

  public void testRoomsOnOtherLevelsIgnored() {
    Home home = new Home();
    Level level0 = new Level("L0", 0, 12, 250);
    Level level1 = new Level("L1", 250, 12, 250);
    home.addLevel(level0);
    home.addLevel(level1);
    createRoom(home, "Upstairs", 0, 0, 100, 100, level1);
    HomePieceOfFurniture piece = createPiece(home, 50, 50, level0);
    Bucket bucket = resolve(home, piece);
    assertEquals(Bucket.Kind.NO_ROOM, bucket.getKind());
    assertSame(level0, bucket.getLevel());
  }

  public void testHomeWithoutLevels() {
    Home home = new Home();
    createRoom(home, "Kitchen", 0, 0, 100, 100, null);
    HomePieceOfFurniture piece = createPiece(home, 50, 50, null);
    Bucket bucket = resolve(home, piece);
    assertNull(bucket.getLevel());
    assertEquals(Bucket.Kind.ROOM, bucket.getKind());
  }

  public void testBucketEquality() {
    Home home = new Home();
    createRoom(home, "Kitchen", 0, 0, 100, 100, null);
    HomePieceOfFurniture piece = createPiece(home, 50, 50, null);
    Bucket bucket1 = resolve(home, piece);
    Bucket bucket2 = resolve(home, piece);
    assertNotSame(bucket1, bucket2);
    assertEquals(bucket1, bucket2);
    assertEquals(bucket1.hashCode(), bucket2.hashCode());
  }

  public void testSortBuckets() {
    Home home = new Home();
    Level level0 = new Level("L0", 0, 12, 250);
    Level level1 = new Level("L1", 250, 12, 250);
    home.addLevel(level0);
    home.addLevel(level1);
    HomePieceOfFurniture kitchenPiece = createPiece(home, 50, 50, level0);
    createRoom(home, "Kitchen", 0, 0, 100, 100, level0);
    HomePieceOfFurniture bathPiece = createPiece(home, 250, 50, level0);
    createRoom(home, "bath", 200, 0, 300, 100, level0);
    HomePieceOfFurniture unnamed1Piece = createPiece(home, 450, 50, level0);
    createRoom(home, null, 400, 0, 500, 100, level0);
    HomePieceOfFurniture unnamed2Piece = createPiece(home, 650, 50, level0);
    createRoom(home, "", 600, 0, 700, 100, level0);
    HomeDoorOrWindow door = new HomeDoorOrWindow(this.catalogDoorOrWindow);
    home.addPieceOfFurniture(door);
    door.setLevel(level0);
    HomePieceOfFurniture noRoomPiece = createPiece(home, 2000, 2000, level0);
    HomePieceOfFurniture upstairsPiece = createPiece(home, 2000, 2000, level1);

    Map<HomePieceOfFurniture, Bucket> buckets = FurnitureRoomResolver.resolve(home, home.getFurniture());
    List<Bucket> expected = Arrays.asList(buckets.get(bathPiece), buckets.get(kitchenPiece),
        buckets.get(unnamed1Piece), buckets.get(unnamed2Piece), buckets.get(door),
        buckets.get(noRoomPiece), buckets.get(upstairsPiece));
    List<Bucket> shuffled = new ArrayList<Bucket>(expected);
    Collections.reverse(shuffled);
    Collections.swap(shuffled, 0, 3);
    assertEquals(expected, FurnitureRoomResolver.sortBuckets(home, shuffled));
  }
}
