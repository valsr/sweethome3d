/*
 * FurnitureTableGroupingTest.java 15 oct. 2026
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
import java.util.List;

import javax.swing.JComponent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.CatalogDoorOrWindow;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.FurnitureCategory;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeDoorOrWindow;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FurnitureTable;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.FurnitureController;
import com.eteks.sweethome3d.viewcontroller.FurnitureRoomResolver;
import com.eteks.sweethome3d.viewcontroller.FurnitureRoomResolver.Bucket;

import junit.framework.TestCase;

/**
 * Tests furniture table grouped by level and room.
 */
public class FurnitureTableGroupingTest extends TestCase {
  private UserPreferences         preferences;
  private CatalogPieceOfFurniture catalogPiece;
  private CatalogDoorOrWindow     catalogDoorOrWindow;
  private Home                    home;
  private Level                   level0;
  private Level                   level1;
  private Room                    kitchen;
  private Room                    unnamedRoom;
  private HomePieceOfFurniture    chair;
  private HomePieceOfFurniture    desk;
  private HomeDoorOrWindow        door;
  private HomePieceOfFurniture    lamp;
  private HomePieceOfFurniture    bed;
  private FurnitureController     controller;
  private FurnitureTable          table;

  @Override
  protected void setUp() {
    this.preferences = new DefaultUserPreferences();
    for (FurnitureCategory category : this.preferences.getFurnitureCatalog().getCategories()) {
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

    this.home = new Home();
    this.level0 = new Level("Ground", 0, 12, 250);
    this.level1 = new Level("Upstairs", 250, 12, 250);
    this.home.addLevel(this.level0);
    this.home.addLevel(this.level1);
    this.kitchen = createRoom("Kitchen", 0, 0, 100, 100, this.level0);
    this.unnamedRoom = createRoom(null, 200, 0, 300, 100, this.level0);
    this.chair = createPiece("Chair", 50, 50, this.level0);
    this.desk = createPiece("Desk", 250, 50, this.level0);
    this.door = new HomeDoorOrWindow(this.catalogDoorOrWindow);
    this.door.setName("Door");
    this.door.setX(100);
    this.door.setY(50);
    this.home.addPieceOfFurniture(this.door);
    this.door.setLevel(this.level0);
    this.lamp = createPiece("Lamp", 500, 500, this.level0);
    this.bed = createPiece("Bed", 50, 50, this.level1);

    this.controller = new FurnitureController(this.home, this.preferences, new SwingViewFactory());
    this.table = getTable(this.controller);
  }

  private static FurnitureTable getTable(FurnitureController controller) {
    return SwingTools.findChildren((JComponent)controller.getView(), FurnitureTable.class).get(0);
  }

  private HomePieceOfFurniture createPiece(String name, float x, float y, Level level) {
    HomePieceOfFurniture piece = new HomePieceOfFurniture(this.catalogPiece);
    piece.setName(name);
    piece.setX(x);
    piece.setY(y);
    this.home.addPieceOfFurniture(piece);
    if (level != null) {
      piece.setLevel(level);
    }
    return piece;
  }

  private Room createRoom(String name, float x0, float y0, float x1, float y1, Level level) {
    Room room = new Room(new float [][] {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}});
    room.setName(name);
    this.home.addRoom(room);
    if (level != null) {
      room.setLevel(level);
    }
    return room;
  }

  private Bucket bucket(HomePieceOfFurniture piece) {
    return FurnitureRoomResolver.resolve(this.home, Arrays.asList(piece)).get(piece);
  }

  private List<Object> getRows(FurnitureTable table) {
    List<Object> rows = new ArrayList<Object>();
    for (int row = 0; row < table.getRowCount(); row++) {
      rows.add(table.getValueAt(row, 0));
    }
    return rows;
  }

  private int indexOfBucketWithRoom(List<Object> rows, Room room) {
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i) instanceof Bucket
          && ((Bucket)rows.get(i)).getRoom() == room) {
        return i;
      }
    }
    return -1;
  }

  public void testGroupedRowOrder() {
    List<Object> expectedRows = Arrays.<Object>asList(
        this.level0, bucket(this.chair), this.chair, bucket(this.desk), this.desk,
        bucket(this.door), this.door, bucket(this.lamp), this.lamp,
        this.level1, bucket(this.bed), this.bed);
    assertEquals(expectedRows, getRows(this.table));
    for (int row = 0; row < this.table.getRowCount(); row++) {
      assertEquals("Wrong piece row state at row " + row,
          this.table.getValueAt(row, 0) instanceof HomePieceOfFurniture, this.table.isPieceRow(row));
    }
  }

  public void testEmptyBucketsHidden() {
    Room hall = createRoom("Hall", 1000, 1000, 1100, 1100, this.level0);
    assertEquals(-1, indexOfBucketWithRoom(getRows(this.table), hall));
  }

  public void testSortAppliesWithinBuckets() {
    HomePieceOfFurniture b = createPiece("B", 20, 20, this.level0);
    HomePieceOfFurniture a = createPiece("A", 30, 30, this.level0);
    this.controller.toggleFurnitureSort(HomePieceOfFurniture.SortableProperty.NAME);
    List<Object> rows = getRows(this.table);
    int kitchenRow = indexOfBucketWithRoom(rows, this.kitchen);
    assertEquals(Arrays.<Object>asList(bucket(this.chair), a, b, this.chair), rows.subList(kitchenRow, kitchenRow + 4));
    assertTrue(kitchenRow < indexOfBucketWithRoom(rows, this.unnamedRoom));

    this.controller.toggleFurnitureSortOrder();
    rows = getRows(this.table);
    kitchenRow = indexOfBucketWithRoom(rows, this.kitchen);
    assertEquals(Arrays.<Object>asList(bucket(this.chair), this.chair, b, a), rows.subList(kitchenRow, kitchenRow + 4));
    assertTrue(kitchenRow < indexOfBucketWithRoom(rows, this.unnamedRoom));
    assertEquals(this.level0, rows.get(0));
  }

  public void testGroupingToggleRestoresFlatList() {
    List<Object> groupedRows = getRows(this.table);
    this.controller.toggleFurnitureGrouping();
    assertEquals(new ArrayList<Object>(this.home.getFurniture()), getRows(this.table));
    this.controller.toggleFurnitureGrouping();
    assertEquals(groupedRows, getRows(this.table));
  }

  public void testGroupedFallsBackToFlatWithoutLevelsAndRooms() {
    Home home = new Home();
    home.addPieceOfFurniture(new HomePieceOfFurniture(this.catalogPiece));
    home.addPieceOfFurniture(new HomeDoorOrWindow(this.catalogDoorOrWindow));
    FurnitureTable table = getTable(new FurnitureController(home, this.preferences, new SwingViewFactory()));
    assertEquals(new ArrayList<Object>(home.getFurniture()), getRows(table));
  }

  public void testGroupedFallsBackToFlatWhenNameColumnHidden() {
    this.home.setFurnitureVisibleProperties(Arrays.asList(
        HomePieceOfFurniture.SortableProperty.WIDTH, HomePieceOfFurniture.SortableProperty.DEPTH));
    assertEquals(new ArrayList<Object>(this.home.getFurniture()), getRows(this.table));
  }

  public void testHeaderRowsRenderInAllColumns() {
    this.home.setFurnitureVisibleProperties(Arrays.asList(HomePieceOfFurniture.SortableProperty.values()));
    for (int row = 0; row < this.table.getRowCount(); row++) {
      for (int column = 0; column < this.table.getColumnCount(); column++) {
        this.table.prepareRenderer(this.table.getCellRenderer(row, column), row, column);
        TableColumn tableColumn = this.table.getColumnModel().getColumn(column);
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        renderer.getTableCellRendererComponent(this.table, this.table.getValueAt(row, column), false, false, -1, column);
      }
    }
  }

  public void testGroupedRowsFollowModelChanges() {
    this.lamp.setX(50);
    this.lamp.setY(50);
    List<Object> rows = getRows(this.table);
    int kitchenRow = indexOfBucketWithRoom(rows, this.kitchen);
    assertTrue("Lamp not in kitchen", rows.indexOf(this.lamp) > kitchenRow
        && rows.indexOf(this.lamp) < indexOfBucketWithRoom(rows, this.unnamedRoom));

    this.kitchen.setName("Attic");
    rows = getRows(this.table);
    assertTrue(indexOfBucketWithRoom(rows, this.kitchen) >= 0);
    // Attic sorts before the unnamed room
    assertTrue(indexOfBucketWithRoom(rows, this.kitchen) < indexOfBucketWithRoom(rows, this.unnamedRoom));

    this.kitchen.setPoints(new float [][] {{600, 0}, {700, 0}, {700, 100}, {600, 100}});
    rows = getRows(this.table);
    assertEquals("Kitchen still listed", -1, indexOfBucketWithRoom(rows, this.kitchen));
    assertEquals(Bucket.Kind.NO_ROOM, bucket(this.chair).getKind());
    int noRoomRow = rows.indexOf(bucket(this.chair));
    assertTrue("Chair not in no room bucket", noRoomRow >= 0
        && rows.indexOf(this.chair) > noRoomRow
        && rows.indexOf(this.chair) < rows.indexOf(this.level1));

    this.home.deletePieceOfFurniture(this.bed);
    this.home.deleteLevel(this.level1);
    rows = getRows(this.table);
    assertFalse(rows.contains(this.level1));
    for (Object row : rows) {
      if (row instanceof Bucket) {
        assertNotSame(this.level1, ((Bucket)row).getLevel());
      }
    }

    Room newRoom = createRoom("Lounge", 0, 0, 60, 60, this.level0);
    rows = getRows(this.table);
    assertTrue("Room addition not followed", indexOfBucketWithRoom(rows, newRoom) >= 0);
  }
}
