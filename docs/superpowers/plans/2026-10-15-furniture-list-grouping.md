# Furniture List Grouped by Level and Room Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the furniture list grouped by level, then by room (plus "Doors & windows" and "No room" buckets), with collapsible level and room headers, behind a toggle that is on by default.

**Architecture:** A pure `FurnitureRoomResolver` (viewcontroller) assigns each top-level piece to a `Bucket`. `FurnitureTable.FurnitureTreeTableModel` gains a grouped mode in which rows are `Level` objects (level headers), `Bucket` objects (room/bucket headers) or pieces; flat mode stays byte-for-byte today's code path. All code that assumes a row is a piece goes through `isPieceRow(row)`.

**Tech Stack:** Java (source level of the project), Swing `JTable` + `JTree` renderer, JUnit 3 style `TestCase`.

**Spec:** `docs/superpowers/specs/2026-10-15-furniture-list-grouping-design.md`

## Global Constraints

- Grouping home property: `com.eteks.sweethome3d.SweetHome3D.FurnitureGroupedByLevelAndRoom`; absent or `"true"` = grouped, `"false"` = flat.
- Collapsed headers home property: `com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders`.
- Action type name: `TOGGLE_FURNITURE_GROUPING`; menu label `Group by level and room`.
- Header labels (English, `FurnitureTable.*` keys in `src/com/eteks/sweethome3d/swing/package.properties`): `doorsAndWindowsHeader=Doors & windows`, `noRoomHeader=No room`, `unnamedRoomHeader=Unnamed room`.
- Toggle is not undoable. CSV export output must stay identical to today's flat output.
- Source files may contain Latin-1 bytes: compile with `-encoding ISO-8859-1`; when editing with scripts use `encoding='latin-1'`.
- Match surrounding style: 2-space indent, Javadoc on every method, `this.` field access.

**Test command** (from memory note `running-junit-tests`; save once as `$SCRATCH/run-test.sh`, `$SCRATCH` = session scratchpad):

```bash
#!/bin/bash
# usage: run-test.sh TestClassName   (jars junit-4.13.2.jar + hamcrest-core-1.3.jar downloaded into $SCRATCH)
set -e; cd /work/sh3d/sweethome3d; S="$(dirname "$0")"
CP="$S/junit-4.13.2.jar:$S/hamcrest-core-1.3.jar:$(echo lib/*.jar lib/java3d-1.6/*.jar libtest/*.jar | tr ' ' ':')"
mkdir -p "$S/classes" "$S/testclasses"
javac -nowarn -encoding ISO-8859-1 -cp "$CP" -d "$S/classes" $(find src -name '*.java') 2>&1 | grep -v '^Note:' || true
javac -nowarn -encoding ISO-8859-1 -cp "$CP:$S/classes" -sourcepath test -d "$S/testclasses" test/com/eteks/sweethome3d/junit/$1.java
java --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/sun.awt=ALL-UNNAMED \
  --enable-native-access=ALL-UNNAMED -Djogamp.gluegen.UseTempJarCache=false \
  -Djava.library.path=lib/java3d-1.6/linux/amd64 \
  -cp "$CP:$S/classes:$S/testclasses:src:test" junit.textui.TestRunner com.eteks.sweethome3d.junit.$1
```

Expected pass output ends with `OK (N tests)`.

## Review Focus

1. **Home with no levels and no rooms** (every new home, many existing ones): grouping must not wrap everything in a lone "No room" header. Rule: if the home has no levels and no rooms, grouped mode produces the flat list. Pinned by `testGroupedFallsBackToFlatWithoutLevelsAndRooms` (Task 3). Existing `FurnitureTableTest` tests must keep passing unchanged because of this rule.
2. **NAME column hidden**: headers have nowhere to draw their label and arrow. Rule: grouped mode produces the flat list while `NAME` is not a visible property (mirrors how group expansion is cleared today). Pinned by `testGroupedFallsBackToFlatWhenNameColumnHidden` (Task 3).
3. **Every column renderer on a header row**: any missed cast throws `ClassCastException` during paint. Pinned by `testHeaderRowsRenderInAllColumns` (Task 3), which renders every row × every `SortableProperty` column.
4. **Piece moved across a room boundary / to another level, room renamed or reshaped, level deleted**: list must rebuild without stale headers or exceptions. Pinned by `testGroupedRowsFollowModelChanges` (Task 3).
5. **Print with the print furniture filter active** (`HomePrintableComponent` sets a filter): headers whose pieces are all filtered must disappear, and collapsed headers must print expanded. Pinned by `testPrintExpandsHeadersAndHidesFilteredHeaders` (Task 6).

---

### Task 1: FurnitureRoomResolver

**Files:**
- Create: `src/com/eteks/sweethome3d/viewcontroller/FurnitureRoomResolver.java`
- Test: `test/com/eteks/sweethome3d/junit/FurnitureRoomResolverTest.java`

**Interfaces:**
- Produces:
  - `public final class FurnitureRoomResolver` (no instances; private constructor).
  - `public static final class FurnitureRoomResolver.Bucket` with `public enum Kind {ROOM, DOORS_AND_WINDOWS, NO_ROOM}`, getters `Level getLevel()` (null when home has no levels), `Kind getKind()`, `Room getRoom()` (non-null only for `ROOM`), value-based `equals`/`hashCode` on (level identity, kind, room identity), `toString()` for debugging.
  - `public static Map<HomePieceOfFurniture, Bucket> resolve(Home home, List<HomePieceOfFurniture> furniture)` — keys are the given pieces, insertion order preserved (`LinkedHashMap`).
  - `public static List<Bucket> sortBuckets(Home home, Collection<Bucket> buckets)` — returns a new list ordered: by level in `home.getLevels()` order (null level first), then within a level: `ROOM` buckets with non-empty names alphabetically (`Collator.getInstance()`), then `ROOM` buckets with null/blank names in `home.getRooms()` order, then `DOORS_AND_WINDOWS`, then `NO_ROOM`.

- [ ] **Step 1: Write the failing tests** in `FurnitureRoomResolverTest extends TestCase`. Build rooms with `new Room(new float[][] {{x0,y0},{x1,y0},{x1,y1},{x0,y1}})`, pieces with `new HomePieceOfFurniture(catalogPiece)` + `setX/setY/setLevel`, doors via a catalog door/window (`new HomeDoorOrWindow(catalogDoorOrWindow)`; find one in `new DefaultUserPreferences().getFurnitureCatalog()` where `isDoorOrWindow()`).
  - `testPieceCenterInsideRoom`: room 0..100², piece at (50,50) → `Kind.ROOM`, `getRoom() == room`.
  - `testOverlappingRoomsChooseSmallest`: rooms 0..400² and 0..100², piece at (50,50) → the 0..100 room.
  - `testDoorOrWindowBucket`: door at (50,50) inside a room → `Kind.DOORS_AND_WINDOWS`, `getRoom() == null`.
  - `testNoRoomBucket`: piece at (500,500) → `Kind.NO_ROOM`.
  - `testRoomsOnOtherLevelsIgnored`: levels L0, L1; room on L1 covering (50,50); piece on L0 at (50,50) → `NO_ROOM` with `getLevel() == L0`.
  - `testHomeWithoutLevels`: piece in room, no levels → `getLevel() == null`, `Kind.ROOM`.
  - `testBucketEquality`: two `resolve` calls for the same piece return `equals` buckets with equal `hashCode`.
  - `testSortBuckets`: on one level, rooms named "Kitchen", "bath", unnamed U1, unnamed U2 (U1 before U2 in `home.getRooms()`), plus D&W and NO_ROOM buckets, given shuffled → `[bath, Kitchen, U1, U2, DOORS_AND_WINDOWS, NO_ROOM]`; with two levels, all L0 buckets precede L1 buckets.

- [ ] **Step 2: Run to verify failure**
Run: `$SCRATCH/run-test.sh FurnitureRoomResolverTest`
Expected: compilation error, `cannot find symbol: class FurnitureRoomResolver`.

- [ ] **Step 3: Implement `FurnitureRoomResolver`.** Room containment: `room.containsPoint(piece.getX(), piece.getY(), 0)` for rooms where `room.getLevel() == piece.getLevel()`; smallest by `room.getArea()`. Doors/windows checked first via `piece.isDoorOrWindow()`.

- [ ] **Step 4: Run to verify pass**
Run: `$SCRATCH/run-test.sh FurnitureRoomResolverTest`
Expected: `OK (8 tests)`.

- [ ] **Step 5: Commit**
```bash
git add src/com/eteks/sweethome3d/viewcontroller/FurnitureRoomResolver.java test/com/eteks/sweethome3d/junit/FurnitureRoomResolverTest.java
git commit -m "Add resolver assigning furniture to level and room buckets"
```

### Task 2: Grouping toggle

**Files:**
- Modify: `src/com/eteks/sweethome3d/viewcontroller/FurnitureController.java` (near `toggleFurnitureSort`, ~line 632)
- Modify: `src/com/eteks/sweethome3d/viewcontroller/HomeView.java:45-56` (furniture action types)
- Modify: `src/com/eteks/sweethome3d/viewcontroller/HomeController.java` (~line 273, next to `DISPLAY_HOME_FURNITURE_NAME` enabling)
- Modify: `src/com/eteks/sweethome3d/swing/HomePane.java` (action creation near line 553 pattern `createToggleAction`; Furniture menu ~line 1258; furniture popup ~line 3312; selected-state sync like `addLevelVisibilityListener` ~line 795)
- Modify: `src/com/eteks/sweethome3d/swing/package.properties` (next to `HomePane.TOGGLE_SEE_THROUGH_WALLS.*`)
- Create: `test/com/eteks/sweethome3d/junit/FurnitureControllerTest.java`

**Interfaces:**
- Produces:
  - `public static final String FurnitureController.FURNITURE_GROUPED_PROPERTY = "com.eteks.sweethome3d.SweetHome3D.FurnitureGroupedByLevelAndRoom"`
  - `public boolean FurnitureController.isFurnitureGroupedByLevelAndRoom()` — `!"false".equals(home.getProperty(FURNITURE_GROUPED_PROPERTY))`
  - `public void FurnitureController.toggleFurnitureGrouping()` — sets the property to `"false"` or `"true"`; `Home.setProperty` fires a property change named by the key, which later tasks listen to.
  - `HomeView.ActionType.TOGGLE_FURNITURE_GROUPING`

- [ ] **Step 1: Write the failing test** `testToggleFurnitureGrouping`: new `Home`, controller → `isFurnitureGroupedByLevelAndRoom()` true and property null; toggle → false and property `"false"`; toggle → true. Also assert a `PropertyChangeListener` registered with `home.addPropertyChangeListener(FurnitureController.FURNITURE_GROUPED_PROPERTY, …)` is called twice.

- [ ] **Step 2: Run to verify failure** — `$SCRATCH/run-test.sh FurnitureControllerTest`; expected `cannot find symbol: method toggleFurnitureGrouping()`.

- [ ] **Step 3: Implement controller methods and the action type.** Enable it in `HomeController` with the other furniture display actions.

- [ ] **Step 4: Wire `HomePane`.** `createToggleAction(ActionType.TOGGLE_FURNITURE_GROUPING, furnitureController.isFurnitureGroupedByLevelAndRoom(), null, preferences, furnitureController, "toggleFurnitureGrouping")`; `addToggleActionToMenu(ActionType.TOGGLE_FURNITURE_GROUPING, false, furnitureMenu)` right after the display-property menu, and the same in the furniture view popup after its sort/display menus; listen to `FURNITURE_GROUPED_PROPERTY` on `home` and call `setToggleButtonModelSelected` (guard on the action being present, as for `TOGGLE_SEE_THROUGH_WALLS`). Properties:
```
HomePane.TOGGLE_FURNITURE_GROUPING.Name=Group by level and room
HomePane.TOGGLE_FURNITURE_GROUPING.ShortDescription=Group the furniture list by level and room
HomePane.TOGGLE_FURNITURE_GROUPING.MnemonicKey=G
```
(If `G` clashes with another Furniture menu mnemonic, pick a free letter.)

- [ ] **Step 5: Run to verify pass** — `$SCRATCH/run-test.sh FurnitureControllerTest` → `OK`; also `$SCRATCH/run-test.sh HomePaneToolBarTest` still passes (or fails only with the known Abbot `java/applet/Applet` error listed in the memory note).

- [ ] **Step 6: Commit** — `git commit -m "Add toggle to group furniture list by level and room"` (stage the files above).

### Task 3: Grouped rows, rendering and rebuild triggers

**Files:**
- Modify: `src/com/eteks/sweethome3d/swing/FurnitureTable.java` — `FurnitureTreeTableModel` (~2636-3099), `addHomeListener` (~682-751), `TreeTableNameCellRenderer` (~2355-2580), constructor area (~161-220)
- Modify: `src/com/eteks/sweethome3d/swing/package.properties` (`FurnitureTable.*` header labels, see Global Constraints)
- Test: `test/com/eteks/sweethome3d/junit/FurnitureTableTest.java`

**Interfaces:**
- Consumes: `FurnitureRoomResolver.resolve/sortBuckets/Bucket` (Task 1); `FurnitureController.isFurnitureGroupedByLevelAndRoom()`, `FURNITURE_GROUPED_PROPERTY` (Task 2).
- Produces:
  - `FurnitureTreeTableModel.isPieceRow(int row)` and `public boolean FurnitureTable.isPieceRow(int row)` (public for tests).
  - Row values: `Level` (level header), `FurnitureRoomResolver.Bucket` (bucket header), `HomePieceOfFurniture`.
  - `FurnitureTreeTableModel.setGrouped(boolean)`; model is constructed with the initial grouped state.

- [ ] **Step 1: Write failing tests** (helper `createGroupedHome()`: 2 levels L0 < L1; on L0 rooms "Kitchen" (0..100²) and unnamed (200..300 × 0..100); on L0 a chair in Kitchen, a table in the unnamed room, a door at (100,50), a lamp at (500,500); on L1 one bed with no room. Build the table via `new FurnitureController(home, preferences, new SwingViewFactory())` as in `testFurnitureTableFilter`.)
  - `testGroupedRowOrder`: rows are exactly `[L0, Bucket(Kitchen), chair, Bucket(unnamed), table, Bucket(D&W), door, Bucket(NO_ROOM), lamp, L1, Bucket(L1,NO_ROOM), bed]`; `isPieceRow` false exactly on header rows.
  - `testEmptyBucketsHidden`: add an empty room "Hall" on L0 → no `Bucket` row with room Hall.
  - `testSortAppliesWithinBuckets`: put two pieces named "B" and "A" in Kitchen, sort by NAME → Kitchen bucket lists A then B and bucket order is unchanged; descending sort → B then A, bucket order still unchanged.
  - `testGroupingToggleRestoresFlatList`: `controller.toggleFurnitureGrouping()` → row count equals `home.getFurniture().size()` and every row is a piece in `home.getFurniture()` order; toggle back → grouped order again.
  - `testGroupedFallsBackToFlatWithoutLevelsAndRooms`: home with furniture and no levels/rooms → rows equal flat list.
  - `testGroupedFallsBackToFlatWhenNameColumnHidden`: `home.setFurnitureVisibleProperties` without `NAME` on `createGroupedHome()` → flat list.
  - `testHeaderRowsRenderInAllColumns`: set visible properties to every `HomePieceOfFurniture.SortableProperty` value; for each row × column call `table.prepareRenderer(table.getCellRenderer(r, c), r, c)` → no exception.
  - `testGroupedRowsFollowModelChanges`: move lamp to (50,50) → lamp now under Kitchen; rename Kitchen to "Attic" → header still present with the same room; set Kitchen points to 600..700 → chair moves to NO_ROOM; delete L1 → no L1 rows remain.

- [ ] **Step 2: Run to verify failure** — `$SCRATCH/run-test.sh FurnitureTableTest`; expected compile error on `isPieceRow`.

- [ ] **Step 3: Model grouped mode.** Row list type becomes `List<Object>`. In `filterAndSortFurniture()`, when grouped and not falling back (Review Focus 1–2): compute viewable+filtered top-level pieces as today, `resolve`, group per bucket, `sortBuckets`, emit a `Level` row before the first bucket of each non-null level, then the bucket row, then that bucket's pieces sorted with `getFurnitureComparator(home)` (when a sort is set; otherwise home order), each followed by its expanded group children exactly as the existing recursion does. Collapsed state is added in Task 4 — for now every header is expanded. In grouped mode the furniture `CollectionListener` always calls `filterAndSortFurniture()` (keep the incremental paths for flat mode only). `getPieceOfFurnitureIndex` keeps working on the `Object` list.

- [ ] **Step 4: TreeModel in grouped mode.** `getChildFurniture(parent)` generalises to `getChildren(Object parent)`: root → level rows (or bucket rows when no levels); `Level` → its buckets; `Bucket` → its pieces; group → group pieces (filtered/sorted as today). `isLeaf` true only for non-group pieces. Cache keyed by parent as today.

- [ ] **Step 5: Rendering.** Override `FurnitureTable.getCellRenderer(int row, int column)`: when `!isPieceRow(row)` and the column identifier isn't `SortableProperty.NAME`, return a shared `DefaultTableCellRenderer` showing an empty string (respecting selection colours). In `TreeTableNameCellRenderer`, the inner `JTree` cell renderer draws `Level.getName()`, bucket room name, or the localized `FurnitureTable.doorsAndWindowsHeader` / `noRoomHeader` / `unnamedRoomHeader` in bold with no icon; skip information-icon logic and the `updateExpandedRows` group check for header rows. Guard the tooltip and `mouseClicked` casts with `isPieceRow`.

- [ ] **Step 6: Rebuild triggers.** In `FurnitureTable.addHomeListener`, add the existing `changeListener` to every room (and to rooms added later via `home.addRoomsListener`, removing on delete), and call `filterAndSortFurniture()` + `updateTableSelectedFurniture(home)` on room collection changes. Listen to `FURNITURE_GROUPED_PROPERTY` on `home`: `model.setGrouped(controller.isFurnitureGroupedByLevelAndRoom())`, then update selection.

- [ ] **Step 7: Run to verify pass** — `$SCRATCH/run-test.sh FurnitureTableTest` → `OK`, including the pre-existing tests.

- [ ] **Step 8: Commit** — `git commit -m "Group furniture list rows by level and room"`.

### Task 4: Collapsing headers and persisting state

**Files:**
- Modify: `src/com/eteks/sweethome3d/swing/FurnitureTable.java` — model (`isRowExpanded`, `toggleRowExpandedState`, `expandPathToPieceOfFurniture`), `updateExpandedRows` / `storeExpandedRows` (~361-410), `mouseClicked` (~464-535), `TreeTableNameCellRenderer.updateExpandedRows` (~2532)
- Test: `test/com/eteks/sweethome3d/junit/FurnitureTableTest.java`

**Interfaces:**
- Consumes: Task 3 row values and `isPieceRow`.
- Produces:
  - Model field `Set<Object> collapsedHeaders` holding `Level` and `Bucket` keys.
  - `isRowExpanded(row)` returns `!collapsedHeaders.contains(header)` for header rows; `toggleRowExpandedState(row)` toggles header rows too.
  - Property `com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders`: comma-separated tokens `L:<levelId>`, `R:<roomId>`, `D:<levelId>`, `N:<levelId>` (empty `<levelId>` when level is null). Unknown IDs ignored on load.

- [ ] **Step 1: Write failing tests**
  - `testCollapseLevelAndRoomHeaders`: on `createGroupedHome()`, toggle the Kitchen bucket row → chair row gone, Kitchen header still present, `isRowExpanded` false on it; toggle L0 → only `[L0, L1, Bucket(L1,NO_ROOM), bed]` remain; toggle L0 again → Kitchen still collapsed under it.
  - `testCollapsedHeadersPersisted`: collapse Kitchen and L1, then read `home.getProperty("com.eteks.sweethome3d.SweetHome3D.CollapsedFurnitureHeaders")` → contains `R:<kitchen.getId()>` and `L:<l1.getId()>`; build a second `FurnitureController`/table on the same home → the same rows are collapsed (call the deferred restore directly or trigger the ancestor-added path as existing expanded-groups code does).
  - `testSelectionFromPlanExpandsCollapsedHeader`: collapse Kitchen, `home.setSelectedItems(Arrays.asList(chair))` → chair row visible and selected; Kitchen header expanded.

- [ ] **Step 2: Run to verify failure** — `$SCRATCH/run-test.sh FurnitureTableTest`; the three new tests fail.

- [ ] **Step 3: Implement collapse** in the model (skip children of collapsed headers when building rows). Click on the arrow of a header row toggles it (reuse `getExpandedStateBounds`) and must **not** change selection (unlike the group branch, which selects the group).

- [ ] **Step 4: Persist.** `storeExpandedRows` also writes the collapsed property; restore collapsed headers **before** restoring the existing row-index-based expanded groups (stored indices were computed with headers present). Purge entries whose level/room no longer exist when rebuilding.

- [ ] **Step 5: Expand path on external selection.** `expandPathToPieceOfFurniture(piece)` also removes the piece's `Level` and `Bucket` from `collapsedHeaders` — but only when the selection change did not originate in the table (Task 5 adds the header-click case; here, external selection always expands).

- [ ] **Step 6: Run to verify pass** — `$SCRATCH/run-test.sh FurnitureTableTest` → `OK`.

- [ ] **Step 7: Commit** — `git commit -m "Collapse level and room headers in furniture list and remember their state"`.

### Task 5: Selecting through headers

**Files:**
- Modify: `src/com/eteks/sweethome3d/swing/FurnitureTable.java` — `tableSelectionListener` (~252-275), `updateTableSelectedFurniture` (~282-358), `mouseClicked` double-click branch
- Test: `test/com/eteks/sweethome3d/junit/FurnitureTableTest.java`

**Interfaces:**
- Consumes: Tasks 3–4.
- Produces: `FurnitureTreeTableModel.getPiecesUnderHeader(int row) -> List<HomePieceOfFurniture>` — top-level pieces of the header's bucket(s), including those hidden by collapse, excluding filtered ones.

- [ ] **Step 1: Write failing tests**
  - `testSelectingBucketHeaderSelectsItsPieces`: put chair and stool in Kitchen; `table.setRowSelectionInterval(kitchenRow, kitchenRow)` → `home.getSelectedItems()` equals {chair, stool} (any order).
  - `testSelectingLevelHeaderSelectsAllLevelPieces`: select the L0 row → selection equals all L0 top-level pieces.
  - `testSelectingCollapsedHeaderKeepsItCollapsed`: collapse Kitchen, select its header row → home selection {chair, stool}; Kitchen still collapsed and its row is still selected in the table.
  - `testHeaderAddsToSelectionWithCtrl`: select lamp row, then `table.addRowSelectionInterval(kitchenRow, kitchenRow)` → selection {lamp, chair, stool}.

- [ ] **Step 2: Run to verify failure** — `$SCRATCH/run-test.sh FurnitureTableTest`.

- [ ] **Step 3: Table → home.** In `tableSelectionListener`, for a header row add `getPiecesUnderHeader(row)` (deduplicated with the existing `ignoredGroupsFurniture` logic); for piece rows keep today's code.

- [ ] **Step 4: Home → table.** In `updateTableSelectedFurniture`, skip `expandPathToPieceOfFurniture` when `selectionByUser` is true (the change came from the table). After collecting piece rows, also select each collapsed header row whose `getPiecesUnderHeader` is non-empty and fully contained in the home selection. Expanded header rows are never selected by this sync.

- [ ] **Step 5: Double-click.** In `mouseClicked`, the `getClickCount() == 2` branch calls `modifySelectedFurniture()` only when `isPieceRow(row)`.

- [ ] **Step 6: Run to verify pass** — `$SCRATCH/run-test.sh FurnitureTableTest` → `OK`.

- [ ] **Step 7: Commit** — `git commit -m "Select all furniture under a level or room header in furniture list"`.

### Task 6: Printing and CSV export

**Files:**
- Modify: `src/com/eteks/sweethome3d/swing/FurnitureTable.java` — `print(Graphics, PageFormat, int)` (~875-995; its two inline renderers at ~883 and ~906), `exportToCSV` (~1031-1240)
- Test: `test/com/eteks/sweethome3d/junit/FurnitureTableTest.java`

**Interfaces:**
- Consumes: Tasks 3–4.
- Produces: `FurnitureTreeTableModel.getFlatFurniture() -> List<HomePieceOfFurniture>` — the list flat mode would display (viewable, filtered, sorted, with expanded group children), independent of grouping and collapse.

- [ ] **Step 1: Write failing tests**
  - `testCsvExportIgnoresGrouping`: on `createGroupedHome()` collapse Kitchen, `exportToCSV` to a `StringWriter`; toggle grouping off and export again → both strings equal, and line count = flat row count + 1 header line.
  - `testPrintExpandsHeadersAndHidesFilteredHeaders`: collapse Kitchen; set a filter excluding doors/windows; call `table.print(g, pageFormat, 0)` on a `BufferedImage` graphics with a default `PageFormat` → returns `PAGE_EXISTS` without exception; after printing Kitchen is still collapsed. Additionally, with the filter set, assert no `Bucket(DOORS_AND_WINDOWS)` row exists.

- [ ] **Step 2: Run to verify failure** — `$SCRATCH/run-test.sh FurnitureTableTest`.

- [ ] **Step 3: CSV.** `exportToCSV` iterates `getFlatFurniture()` and `exportRowToCSV` takes the piece instead of a row index (keep `getClipboardCSV` working through the same path).

- [ ] **Step 4: Print.** Save `collapsedHeaders`, clear it and rebuild before laying out pages; restore and rebuild in a `finally` (and in the `invokeAndWait` path if printing happens off the EDT, as the existing code does). The two inline print renderers go through the header-row guard from Task 3.

- [ ] **Step 5: Run to verify pass** — `$SCRATCH/run-test.sh FurnitureTableTest` → `OK`.

- [ ] **Step 6: Commit** — `git commit -m "Print grouped furniture list fully expanded and keep CSV export flat"`.

### Task 7: Final verification

- [ ] **Step 1:** Run `FurnitureRoomResolverTest`, `FurnitureTableTest`, `FurnitureControllerTest`, `HomeControllerTest`, `HomePaneToolBarTest`, `PlanComponentWithFurnitureTest`. Expected: all `OK` except failures already listed as pre-existing in the `running-junit-tests` memory note (compare against `main` if unsure).
- [ ] **Step 2:** Launch the app (`run` skill or `ant` build used in earlier sessions), open a home with levels and rooms, and check: toggle on/off from the Furniture menu and the furniture list popup; collapse a level and a room, save, reopen → still collapsed; click a room header → pieces highlighted in plan; drag a piece in the plan into another room → it moves to that room's header; Print preview shows all headers expanded; Export to CSV unchanged.
- [ ] **Step 3:** Add a line to `README.md` alongside the previous feature entries describing the grouping toggle; commit `git commit -m "Document furniture list grouping"`.
