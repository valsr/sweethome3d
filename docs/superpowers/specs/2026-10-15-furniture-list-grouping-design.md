# Furniture list grouped by level and room — design

Date: 2026-10-15
Status: approved in conversation, pending written-spec review

## Goal

Let the furniture list show home furniture grouped by floor (level) and then by room,
with each floor and each room collapsible, so that users can find and select furniture
by where it is in the home.

## Decisions (from the user)

- Grouping is a **toggle**, **enabled by default**.
- Pieces are assigned to rooms by their **center point**; **doors and windows** go to a
  dedicated **"Doors & windows"** bucket on each floor (each piece listed exactly once).
- Header rows show the **name only** (no count, no subtotals).
- **Clicking a header selects every piece under it**; only the arrow icon expands/collapses.
- **Printing follows the grouping** (fully expanded); **CSV export stays flat**.

## Behavior

### Toggle

- New check box menu item **Furniture > Group by level and room**, backed by a new
  `HomeView.ActionType.TOGGLE_FURNITURE_GROUPING`.
- State stored as the home property
  `com.eteks.sweethome3d.SweetHome3D.FurnitureGroupedByLevelAndRoom`.
  Absent or `"true"` means grouped; `"false"` means flat. Existing and new homes therefore
  open grouped.
- Toggled through a new `FurnitureController.toggleFurnitureGrouping()` method. Not undoable
  (same as the sort toggle).
- When off, the furniture list behaves exactly as today.

### Tree structure when grouped

```
Home (root, not shown)
├── Level header            (omitted tier when the home has no levels)
│   ├── Room header         (named rooms alphabetically, then unnamed rooms in plan order)
│   │   └── pieces          (sorted by the current column sort)
│   │       └── group children (existing furniture group expansion)
│   ├── "Doors & windows" header
│   └── "No room" header
└── ...
```

- Levels: only viewable levels (existing behavior), ordered like the plan level tabs
  (elevation, then elevation index).
- Homes without levels: room / bucket headers are the top tier.
- Headers whose bucket contains no listed piece are hidden (empty rooms, empty buckets,
  and buckets emptied by the furniture filter).
- Unnamed rooms are labelled "Unnamed room".
- Column sorting applies to pieces within each bucket, never to headers.
- Initial state: all headers expanded.

### Room assignment

For each top-level piece of furniture (pieces inside a group follow their group):

1. If `piece.isDoorOrWindow()` → "Doors & windows" bucket of its level.
2. Else, among rooms on the same level whose outline contains the piece center
   (`room.containsPoint(piece.getX(), piece.getY(), 0)`), choose the one with the smallest
   area → that room's bucket.
3. Else → "No room" bucket of its level.

### Rendering

- Header rows: label in bold in the name column, indented by tree depth, with the same
  expand/collapse arrow as furniture groups and no icon. All other columns render empty.
- No tooltip and no information-icon handling on header rows.

### Interaction

- **Click on header row** (outside the arrow): the table → home selection listener maps a
  selected header row to all top-level pieces beneath it. Shift/Ctrl modifiers work as usual.
- **Click on arrow**: toggles the header's collapsed state; selection unchanged.
- **Double-click on header**: no action beyond selection (does not open Modify furniture).
- **Home → table selection sync**: selected pieces' rows are selected. When a piece lies
  under a collapsed header:
  - if the selection change came from outside the table (e.g. plan), the path to the
    piece is expanded (same as for furniture groups today);
  - if it came from the table itself (header click), nothing expands; a collapsed header
    row is selected when all pieces beneath it are selected.

### Persistence of collapsed state

- Collapsed headers are saved in a home property next to the existing expanded-groups
  property, keyed by level ID and room ID (plus bucket kind for "Doors & windows" /
  "No room"). Unknown IDs are ignored on load.

### Printing

- `FurnitureTable.print()` temporarily expands all level and room headers, prints, then
  restores the collapsed state. Furniture group expansion is left as is.

### CSV export

- Unaffected by grouping and collapse: exports the flat-mode piece list, identical to
  today's output.

## Architecture

### `FurnitureRoomResolver` (new, `com.eteks.sweethome3d.viewcontroller`)

Pure helper with no Swing dependency:

```java
public static Map<HomePieceOfFurniture, Bucket> resolve(Home home, List<HomePieceOfFurniture> furniture)
```

where `Bucket` identifies `(Level level, Kind kind, Room room)` with
`Kind ∈ {ROOM, DOORS_AND_WINDOWS, NO_ROOM}`, and with `equals`/`hashCode` based on these
values. Unit tested in isolation.

### Header nodes (in `FurnitureTable`)

Immutable key objects `LevelHeader(level)` and `BucketHeader(Bucket)` whose equality is
value-based, so collapsed state survives model rebuilds.

### `FurnitureTreeTableModel` changes

- Rows become `Object` (header or piece). New helper `isPieceRow(int row)`.
- `filterAndSortFurniture()`:
  - flat mode: current algorithm, unchanged (incremental add/delete shortcuts kept);
  - grouped mode: full rebuild — resolve buckets, emit level header → bucket headers →
    sorted pieces → expanded group children, skipping children of collapsed headers and
    hiding empty headers.
- `TreeModel` methods (`getChild`, `getChildCount`, `getIndexOfChild`, `isLeaf`) return
  headers as intermediate nodes in grouped mode.
- `isRowExpanded` / `toggleRowExpandedState` handle header rows (collapsed set) as well as
  groups (expanded set).
- `expandPathToPieceOfFurniture` also expands the piece's level and bucket headers.
- New method returning all top-level pieces beneath a header row (for selection).
- Rebuild triggers in grouped mode: furniture collection changes; piece `X`, `Y`, `LEVEL`
  changes (door/window status is immutable, so it needs no listener); room collection changes; room `POINTS`, `NAME`, `LEVEL`
  changes; level collection and level `ELEVATION`, `ELEVATION_INDEX`, `NAME`, `VIEWABLE`
  changes; the grouping property change.

### `FurnitureTable` changes

- Every place that casts `getValueAt(row, 0)` to `HomePieceOfFurniture` checks
  `isPieceRow(row)` first: table selection listener, home selection sync, tooltip,
  information-icon / double-click handling, column renderers, and `exportToCSV`.
- `TreeTableNameCellRenderer` renders header labels.
- `print()` expands headers temporarily.
- `exportToCSV()` iterates the flat-mode list.

### Toggle wiring

- `HomeView.ActionType.TOGGLE_FURNITURE_GROUPING`.
- `HomePane`: action creation, check box menu item in the Furniture menu, selected state
  synchronized with the home property.
- `FurnitureController.toggleFurnitureGrouping()`.
- `package.properties` (English) entries for the action name and the header labels
  "Doors & windows", "No room", "Unnamed room".

## Testing

- `FurnitureRoomResolverTest`: center-in-room, overlapping rooms (smallest wins), door /
  window bucket, no-room bucket, pieces on different levels, home without levels.
- `FurnitureTableTest` additions:
  - grouped row order for a two-level home with rooms, doors, and no-room pieces;
  - hidden empty headers; unnamed room label; sort applies within buckets;
  - collapse/expand of level and room headers, and persistence through the home property;
  - selecting a header selects all its pieces; plan-originated selection expands the path;
  - toggle off restores today's flat list exactly;
  - CSV export identical in grouped and flat modes.
- Manual check in the running app: toggle, collapse, print preview, moving a piece
  across a room boundary.

## Out of scope

- Counts or price subtotals on headers.
- Grouping CSV export.
- Drag-and-drop of pieces between room headers.
- Localized labels beyond English.
