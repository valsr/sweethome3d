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

  private int rowOf(Object value) {
    return getRows(this.table).indexOf(value);
  }

  private void toggleRow(FurnitureTable table, Object value) {
    table.toggleRowExpandedState(getRows(table).indexOf(value));
  }

  public void testCollapseLevelAndRoomHeaders() {
    toggleRow(this.table, bucket(this.chair));
    assertEquals(-1, rowOf(this.chair));
    assertTrue(rowOf(bucket(this.chair)) >= 0);
    assertFalse(this.table.isRowExpanded(rowOf(bucket(this.chair))));

    toggleRow(this.table, this.level0);
    assertEquals(Arrays.<Object>asList(this.level0, this.level1, bucket(this.bed), this.bed), getRows(this.table));
    assertFalse(this.table.isRowExpanded(0));

    toggleRow(this.table, this.level0);
    assertTrue(rowOf(bucket(this.chair)) >= 0);
    assertEquals("Kitchen should stay collapsed", -1, rowOf(this.chair));
    assertTrue(rowOf(this.desk) >= 0);
  }

  public void testCollapsedHeadersPersisted() {
    toggleRow(this.table, bucket(this.chair));
    toggleRow(this.table, this.level1);
    String property = this.home.getProperty("com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders");
    assertNotNull("Collapsed headers not stored", property);
    List<String> tokens = Arrays.asList(property.split(","));
    assertTrue(property, tokens.contains("R:" + this.kitchen.getId()));
    assertTrue(property, tokens.contains("L:" + this.level1.getId()));

    FurnitureTable otherTable = getTable(new FurnitureController(this.home, this.preferences, new SwingViewFactory()));
    assertEquals(getRows(this.table), getRows(otherTable));
  }

  public void testSelectionFromPlanExpandsCollapsedHeader() {
    toggleRow(this.table, bucket(this.chair));
    toggleRow(this.table, this.level0);
    this.home.setSelectedItems(Arrays.asList(this.chair));
    int chairRow = rowOf(this.chair);
    assertTrue("Chair not visible", chairRow >= 0);
    assertTrue(this.table.isRowSelected(chairRow));
    assertTrue(this.table.isRowExpanded(rowOf(bucket(this.chair))));
    assertTrue(this.table.isRowExpanded(rowOf(this.level0)));
  }

  public void testSelectionKeptWhenHeaderCollapsed() {
    this.home.setSelectedItems(Arrays.asList(this.chair, this.desk));
    toggleRow(this.table, bucket(this.chair));
    assertEquals(Arrays.asList(this.chair, this.desk), this.home.getSelectedItems());
    assertFalse("Kitchen re-expanded", this.table.isRowExpanded(rowOf(bucket(this.chair))));
    assertEquals(-1, rowOf(this.chair));
    assertTrue(this.table.isRowSelected(rowOf(this.desk)));
  }

  public void testSelectionKeptWhenPieceMovesToNewRoom() {
    Room hall = createRoom("Hall", 1000, 1000, 1100, 1100, this.level0);
    // Keep another piece in no room bucket so that row count changes
    createPiece("Rug", 800, 800, this.level0);
    this.home.setSelectedItems(Arrays.asList(this.lamp));
    int rowCount = this.table.getRowCount();
    this.lamp.setX(1050);
    this.lamp.setY(1050);
    assertTrue("Hall not listed", indexOfBucketWithRoom(getRows(this.table), hall) >= 0);
    assertEquals("Row count unchanged", rowCount + 1, this.table.getRowCount());
    assertEquals(Arrays.asList(this.lamp), this.home.getSelectedItems());
    assertTrue(this.table.isRowSelected(rowOf(this.lamp)));
  }

  public void testPressOnHeaderArrowTogglesWithoutChangingSelection() {
    this.home.setSelectedItems(Arrays.asList(this.desk));
    int kitchenRow = rowOf(bucket(this.chair));
    int nameColumn = this.table.getColumnModel().getColumnIndex(HomePieceOfFurniture.SortableProperty.NAME);
    java.awt.Rectangle cellBounds = this.table.getCellRect(kitchenRow, nameColumn, true);
    this.table.dispatchEvent(new java.awt.event.MouseEvent(this.table, java.awt.event.MouseEvent.MOUSE_PRESSED,
        System.currentTimeMillis(), java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
        cellBounds.x + 2, cellBounds.y + cellBounds.height / 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
    this.table.dispatchEvent(new java.awt.event.MouseEvent(this.table, java.awt.event.MouseEvent.MOUSE_RELEASED,
        System.currentTimeMillis(), 0,
        cellBounds.x + 2, cellBounds.y + cellBounds.height / 2, 1, false, java.awt.event.MouseEvent.BUTTON1));
    assertFalse("Kitchen not collapsed", this.table.isRowExpanded(rowOf(bucket(this.chair))));
    assertEquals(Arrays.asList(this.desk), this.home.getSelectedItems());
  }

  private static void assertSameElements(List<?> expected, List<?> actual) {
    assertEquals(actual.toString(), expected.size(), actual.size());
    assertTrue(actual.toString(), actual.containsAll(expected));
  }

  public void testSelectingBucketHeaderSelectsItsPieces() {
    HomePieceOfFurniture stool = createPiece("Stool", 20, 20, this.level0);
    int kitchenRow = rowOf(bucket(this.chair));
    this.table.setRowSelectionInterval(kitchenRow, kitchenRow);
    assertSameElements(Arrays.asList(this.chair, stool), this.home.getSelectedItems());
    assertTrue(this.table.isRowSelected(rowOf(this.chair)));
    assertTrue(this.table.isRowSelected(rowOf(stool)));
  }

  public void testSelectingLevelHeaderSelectsAllLevelPieces() {
    this.table.setRowSelectionInterval(0, 0);
    assertSameElements(Arrays.asList(this.chair, this.desk, this.door, this.lamp), this.home.getSelectedItems());
  }

  public void testSelectingCollapsedHeaderKeepsItCollapsed() {
    HomePieceOfFurniture stool = createPiece("Stool", 20, 20, this.level0);
    toggleRow(this.table, bucket(this.chair));
    int kitchenRow = rowOf(bucket(this.chair));
    this.table.setRowSelectionInterval(kitchenRow, kitchenRow);
    assertSameElements(Arrays.asList(this.chair, stool), this.home.getSelectedItems());
    kitchenRow = rowOf(bucket(this.chair));
    assertFalse("Kitchen expanded", this.table.isRowExpanded(kitchenRow));
    assertTrue("Kitchen row not selected", this.table.isRowSelected(kitchenRow));
  }

  public void testHeaderAddsToSelectionWithCtrl() {
    HomePieceOfFurniture stool = createPiece("Stool", 20, 20, this.level0);
    int lampRow = rowOf(this.lamp);
    this.table.setRowSelectionInterval(lampRow, lampRow);
    int kitchenRow = rowOf(bucket(this.chair));
    this.table.addRowSelectionInterval(kitchenRow, kitchenRow);
    assertSameElements(Arrays.asList(this.lamp, this.chair, stool), this.home.getSelectedItems());
  }

  public void testCsvExportIgnoresGrouping() throws Exception {
    toggleRow(this.table, bucket(this.chair));
    java.io.StringWriter groupedCsv = new java.io.StringWriter();
    this.table.exportToCSV(groupedCsv, ',');
    this.controller.toggleFurnitureGrouping();
    java.io.StringWriter flatCsv = new java.io.StringWriter();
    this.table.exportToCSV(flatCsv, ',');
    assertEquals(flatCsv.toString(), groupedCsv.toString());
    assertEquals(this.table.getRowCount() + 1, groupedCsv.toString().split(System.getProperty("line.separator")).length);
  }

  public void testPrintExpandsHeadersAndHidesFilteredHeaders() throws Exception {
    int expandedRowCount = this.table.getRowCount();
    toggleRow(this.table, bucket(this.chair));
    this.table.setFurnitureFilter(new com.eteks.sweethome3d.viewcontroller.FurnitureView.FurnitureFilter() {
        public boolean include(Home home, HomePieceOfFurniture piece) {
          return !piece.isDoorOrWindow();
        }
      });
    assertFalse("Doors and windows header still listed", getRows(this.table).contains(bucket(this.door)));
    final int [] maxRowCount = {0};
    this.table.getModel().addTableModelListener(new javax.swing.event.TableModelListener() {
        public void tableChanged(javax.swing.event.TableModelEvent ev) {
          maxRowCount [0] = Math.max(maxRowCount [0], table.getModel().getRowCount());
        }
      });
    String collapsedProperty = this.home.getProperty("com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders");
    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(600, 800, java.awt.image.BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D g = image.createGraphics();
    this.table.setSize(this.table.getPreferredSize());
    int result = this.table.print(g, new java.awt.print.PageFormat(), 0);
    g.dispose();
    assertEquals(java.awt.print.Printable.PAGE_EXISTS, result);
    // All rows except door bucket header and door are printed
    assertEquals("Headers not expanded while printing", expandedRowCount - 2, maxRowCount [0]);
    assertFalse("Kitchen not collapsed after printing", this.table.isRowExpanded(rowOf(bucket(this.chair))));
    assertEquals(collapsedProperty, this.home.getProperty("com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders"));
  }

  /**
   * Prints the first page of the table and returns the count of horizontal grid lines.
   */
  private int countPrintedGridLines() throws Exception {
    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(612, 792, java.awt.image.BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D g = image.createGraphics();
    g.setColor(java.awt.Color.WHITE);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
    this.table.print(g, new java.awt.print.PageFormat(), 0);
    g.dispose();
    // Find table left border
    int left = -1;
    for (int column = 0; column < image.getWidth() && left < 0; column++) {
      for (int y = 0; y < image.getHeight(); y++) {
        if (isDark(image, column, y)) {
          left = column;
          break;
        }
      }
    }
    assertTrue("Nothing printed", left >= 0);
    // Count horizontal grid lines crossed by a vertical line inside the name column
    int lineCount = 0;
    boolean previousDark = false;
    for (int y = 0; y < image.getHeight(); y++) {
      boolean dark = isDark(image, left + 4, y);
      if (dark && !previousDark) {
        lineCount++;
      }
      previousDark = dark;
    }
    return lineCount;
  }

  private static boolean isDark(java.awt.image.BufferedImage image, int x, int y) {
    java.awt.Color color = new java.awt.Color(image.getRGB(x, y));
    return color.getRed() + color.getGreen() + color.getBlue() < 300;
  }

  public void testPrintedPageContainsAllRows() throws Exception {
    this.table.setSize(this.table.getPreferredSize());
    int expandedLineCount = countPrintedGridLines();
    toggleRow(this.table, this.level0);
    // Size table as it would be displayed with collapsed rows
    this.table.setSize(this.table.getPreferredSize());
    assertEquals("Wrong printed row count", expandedLineCount, countPrintedGridLines());
  }
}
