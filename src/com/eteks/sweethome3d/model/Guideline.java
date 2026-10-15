/*
 * Guideline.java
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
package com.eteks.sweethome3d.model;

/**
 * A horizontal or vertical line displayed across the plan at all levels,
 * used as a guide to place other items.
 */
public class Guideline extends HomeObject implements Selectable {
  private static final long serialVersionUID = 1L;

  /**
   * The default color of guidelines.
   */
  public static final int DEFAULT_COLOR = 0xE91E63;

  /**
   * The half length of the segment returned by {@link #getPoints()}, long enough to cross any plan.
   */
  private static final float HALF_LENGTH = 1E5f;

  /**
   * The orientation of a guideline.
   */
  public enum Orientation {
    /** A guideline parallel to x axis, located at a given ordinate. */
    HORIZONTAL,
    /** A guideline parallel to y axis, located at a given abscissa. */
    VERTICAL}

  /**
   * The properties of a guideline that may change. <code>PropertyChangeListener</code>s added
   * to a guideline will be notified under a property name equal to the string value of one these properties.
   */
  public enum Property {POSITION, COLOR}

  private final Orientation orientation;
  private float             position;
  private int               color;

  /**
   * Creates a guideline with the default color.
   * @param orientation the orientation of the guideline
   * @param position    the ordinate of an horizontal guideline or the abscissa of a vertical guideline
   */
  public Guideline(Orientation orientation, float position) {
    this(createId("guideline"), orientation, position);
  }

  /**
   * Creates a guideline with the default color.
   */
  public Guideline(String id, Orientation orientation, float position) {
    super(id);
    if (orientation == null) {
      throw new IllegalArgumentException("Orientation can't be null");
    }
    this.orientation = orientation;
    this.position = position;
    this.color = DEFAULT_COLOR;
  }

  /**
   * Returns the orientation of this guideline.
   */
  public Orientation getOrientation() {
    return this.orientation;
  }

  /**
   * Returns the ordinate of this guideline if it's horizontal or its abscissa if it's vertical.
   */
  public float getPosition() {
    return this.position;
  }

  /**
   * Sets the position of this guideline. Once this guideline is updated,
   * listeners added to this guideline will receive a change notification.
   */
  public void setPosition(float position) {
    if (position != this.position) {
      float oldPosition = this.position;
      this.position = position;
      firePropertyChange(Property.POSITION.name(), oldPosition, position);
    }
  }

  /**
   * Returns the RGB color of this guideline.
   */
  public int getColor() {
    return this.color;
  }

  /**
   * Sets the RGB color of this guideline. Once this guideline is updated,
   * listeners added to this guideline will receive a change notification.
   */
  public void setColor(int color) {
    if (color != this.color) {
      int oldColor = this.color;
      this.color = color;
      firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }

  /**
   * Returns the two ends of a segment of this guideline long enough to cross any plan.
   */
  public float [][] getPoints() {
    if (this.orientation == Orientation.HORIZONTAL) {
      return new float [][] {{-HALF_LENGTH, this.position}, {HALF_LENGTH, this.position}};
    } else {
      return new float [][] {{this.position, -HALF_LENGTH}, {this.position, HALF_LENGTH}};
    }
  }

  /**
   * Returns <code>true</code> if this guideline crosses the horizontal rectangle
   * which opposite corners are at points (<code>x0</code>, <code>y0</code>) and (<code>x1</code>, <code>y1</code>).
   */
  public boolean intersectsRectangle(float x0, float y0, float x1, float y1) {
    if (this.orientation == Orientation.HORIZONTAL) {
      return this.position >= Math.min(y0, y1) && this.position <= Math.max(y0, y1);
    } else {
      return this.position >= Math.min(x0, x1) && this.position <= Math.max(x0, x1);
    }
  }

  /**
   * Returns <code>true</code> if this guideline is at the point at (<code>x</code>, <code>y</code>)
   * with a given <code>margin</code>.
   */
  public boolean containsPoint(float x, float y, float margin) {
    return Math.abs((this.orientation == Orientation.HORIZONTAL ? y : x) - this.position) <= margin;
  }

  /**
   * Moves this guideline of (<code>dx</code>, <code>dy</code>) units,
   * along the axis perpendicular to its orientation.
   */
  public void move(float dx, float dy) {
    setPosition(this.position + (this.orientation == Orientation.HORIZONTAL ? dy : dx));
  }

  /**
   * Returns a clone of this guideline.
   */
  @Override
  public Guideline clone() {
    return (Guideline)super.clone();
  }
}
