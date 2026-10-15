/*
 * FurnitureRoomResolver.java 15 oct. 2026
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
package com.eteks.sweethome3d.viewcontroller;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;

/**
 * Assigns home furniture to the level and room in which it's placed.
 */
public final class FurnitureRoomResolver {
  private FurnitureRoomResolver() {
    // This class contains only static methods
  }

  /**
   * A level and room bucket in which a piece of furniture is listed.
   */
  public static final class Bucket {
    /**
     * The kind of bucket.
     */
    public enum Kind {ROOM, DOORS_AND_WINDOWS, NO_ROOM}

    private final Level level;
    private final Kind  kind;
    private final Room  room;

    /**
     * Creates a bucket for the given level, kind and room.
     */
    public Bucket(Level level, Kind kind, Room room) {
      this.level = level;
      this.kind = kind;
      this.room = room;
    }

    /**
     * Returns the level of this bucket or <code>null</code> if home has no levels.
     */
    public Level getLevel() {
      return this.level;
    }

    /**
     * Returns the kind of this bucket.
     */
    public Kind getKind() {
      return this.kind;
    }

    /**
     * Returns the room of this bucket or <code>null</code> if its kind isn't {@link Kind#ROOM ROOM}.
     */
    public Room getRoom() {
      return this.room;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Bucket) {
        Bucket bucket = (Bucket)obj;
        return bucket.level == this.level
            && bucket.kind == this.kind
            && bucket.room == this.room;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this.level) * 31 * 31
          + this.kind.hashCode() * 31
          + System.identityHashCode(this.room);
    }

    @Override
    public String toString() {
      return "Bucket[" + (this.level != null ? this.level.getName() : null) + ", " + this.kind
          + (this.room != null ? ", " + this.room.getName() : "") + "]";
    }
  }

  /**
   * Returns the bucket of each piece of the given <code>furniture</code>, keyed in the order of the list.
   * Doors and windows are placed in a {@link Bucket.Kind#DOORS_AND_WINDOWS DOORS_AND_WINDOWS} bucket,
   * other pieces in the smallest room of their level containing their center, or in a
   * {@link Bucket.Kind#NO_ROOM NO_ROOM} bucket if none contains it.
   */
  public static Map<HomePieceOfFurniture, Bucket> resolve(Home home, List<HomePieceOfFurniture> furniture) {
    List<Room> rooms = home.getRooms();
    Map<HomePieceOfFurniture, Bucket> buckets = new LinkedHashMap<HomePieceOfFurniture, Bucket>();
    for (HomePieceOfFurniture piece : furniture) {
      Level level = piece.getLevel();
      if (piece.isDoorOrWindow()) {
        buckets.put(piece, new Bucket(level, Bucket.Kind.DOORS_AND_WINDOWS, null));
      } else {
        Room pieceRoom = null;
        float pieceRoomArea = Float.MAX_VALUE;
        for (Room room : rooms) {
          if (room.getLevel() == level
              && room.containsPoint(piece.getX(), piece.getY(), 0)) {
            float area = room.getArea();
            if (area < pieceRoomArea) {
              pieceRoom = room;
              pieceRoomArea = area;
            }
          }
        }
        buckets.put(piece, pieceRoom != null
            ? new Bucket(level, Bucket.Kind.ROOM, pieceRoom)
            : new Bucket(level, Bucket.Kind.NO_ROOM, null));
      }
    }
    return buckets;
  }

  /**
   * Returns a new list containing the given <code>buckets</code> sorted by level, then rooms with a name
   * in alphabetical order, rooms without name in home order, doors and windows, and pieces in no room.
   */
  public static List<Bucket> sortBuckets(final Home home, Collection<Bucket> buckets) {
    final List<Level> levels = home.getLevels();
    final List<Room> rooms = home.getRooms();
    final Collator collator = Collator.getInstance();
    List<Bucket> sortedBuckets = new ArrayList<Bucket>(buckets);
    Collections.sort(sortedBuckets, new Comparator<Bucket>() {
        public int compare(Bucket bucket1, Bucket bucket2) {
          int levelComparison = levels.indexOf(bucket1.getLevel()) - levels.indexOf(bucket2.getLevel());
          if (levelComparison != 0) {
            return levelComparison;
          }
          int kindComparison = getKindRank(bucket1) - getKindRank(bucket2);
          if (kindComparison != 0) {
            return kindComparison;
          } else if (getKindRank(bucket1) == 0) {
            return collator.compare(bucket1.getRoom().getName().trim(), bucket2.getRoom().getName().trim());
          } else if (getKindRank(bucket1) == 1) {
            return rooms.indexOf(bucket1.getRoom()) - rooms.indexOf(bucket2.getRoom());
          } else {
            return 0;
          }
        }

        private int getKindRank(Bucket bucket) {
          switch (bucket.getKind()) {
            case ROOM :
              String name = bucket.getRoom().getName();
              return name != null && name.trim().length() > 0  ? 0  : 1;
            case DOORS_AND_WINDOWS :
              return 2;
            default :
              return 3;
          }
        }
      });
    return sortedBuckets;
  }
}
