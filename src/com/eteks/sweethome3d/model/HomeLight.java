/*
 * HomeLight.java 12 mars 2009
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

/**
 * A light in {@linkplain Home home}.
 * @author Emmanuel Puybaret
 * @since  1.7
 */
public class HomeLight extends HomePieceOfFurniture implements Light {
  private static final long serialVersionUID = 1L;

  /**
   * The properties of a light that may change. <code>PropertyChangeListener</code>s added
   * to a light will be notified under a property name equal to the string value of one these properties.
   */
  public enum Property {POWER, LIGHT_SOURCES, LIGHT_SOURCE_MATERIAL_NAMES, LIGHT_COLOR, LIGHT_COLOR_TEMPERATURE, POWER_UNIT};

  /**
   * The units in which the power of a light may be displayed.
   */
  public enum PowerUnit {PERCENTAGE, LUMEN};

  /**
   * The luminous flux in lumens emitted by a light at a power equal to 1, i.e. 100%.
   */
  public static final float FULL_POWER_LUMINOUS_FLUX = 800f;

  private LightSource [] lightSources;
  private String []      lightSourceMaterialNames;
  private float power;
  private Integer lightColor;
  private Integer lightColorTemperature;
  private PowerUnit powerUnit;

  /**
   * Creates a home light from an existing one.
   * No additional properties will be copied.
   * @param light the light from which data are copied
   */
  public HomeLight(Light light) {
    this(light, EMPTY_PROPERTY_ARRAY);
  }

  /**
   * Creates a home light from an existing one.
   * No additional properties will be copied.
   * @param light the light from which data are copied
   * @param copiedProperties the names of the additional properties which should be copied from the existing piece
   *                         or <code>null</code> if all properties should be copied.
   * @since 7.2
   */
  public HomeLight(Light light, String [] copiedProperties) {
    this(createId("light"), light, copiedProperties);
  }

  /**
   * Creates a home light from an existing one.
   * No additional properties will be copied.
   * @param id    the ID of the light
   * @param light the light from which data are copied
   * @since 6.4
   */
  public HomeLight(String id, Light light) {
    this(id, light, EMPTY_PROPERTY_ARRAY);
  }

  /**
   * Creates a home light from an existing one.
   * No additional properties will be copied.
   * @param id    the ID of the light
   * @param light the light from which data are copied
   * @param copiedProperties the names of the additional properties which should be copied from the existing piece
   *                         or <code>null</code> if all properties should be copied.
   * @since 7.2
   */
  public HomeLight(String id, Light light, String [] copiedProperties) {
    super(id, light, copiedProperties);
    this.lightSources = light.getLightSources();
    this.lightSourceMaterialNames = light.getLightSourceMaterialNames();
    this.power = 0.5f;
  }

  /**
   * Initializes new piece fields to their default values
   * and reads light from <code>in</code> stream with default reading method.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.lightSourceMaterialNames = new String [0];
    in.defaultReadObject();
  }


  /**
   * Returns the sources managed by this light. Each light source point
   * is a percentage of the width, the depth and the height of this light.
   * with the abscissa origin at the left side of the piece,
   * the ordinate origin at the front side of the piece
   * and the elevation origin at the bottom side of the piece.
   * @return a copy of light sources array.
   */
  public LightSource [] getLightSources() {
    if (this.lightSources.length == 0) {
      return this.lightSources;
    } else {
      return this.lightSources.clone();
    }
  }

  /**
   * Sets the sources managed by this light. Once this light is updated,
   * listeners added to this light will receive a change notification.
   * @param lightSources sources of the light
   * @since 6.5
   */
  public void setLightSources(LightSource [] lightSources) {
    if (!Arrays.equals(lightSources, this.lightSources)) {
      LightSource [] oldLightSources = this.lightSources.length == 0
          ? this.lightSources
          : this.lightSources.clone();
      this.lightSources = lightSources.length == 0
          ? lightSources
          : lightSources.clone();
      firePropertyChange(Property.LIGHT_SOURCES.name(), oldLightSources, lightSources);
    }
  }

  /**
   * Returns the material names of the light sources in the 3D model managed by this light.
   * @return a copy of light source material names array.
   * @since 7.0
   */
  public String [] getLightSourceMaterialNames() {
    if (this.lightSourceMaterialNames.length == 0) {
      return this.lightSourceMaterialNames;
    } else {
      return this.lightSourceMaterialNames.clone();
    }
  }

  /**
   * Sets the material names of the light sources in the 3D model managed by this light.
   * Once this light is updated, listeners added to this light will receive a change notification.
   * @param lightSourceMaterialNames material names of the light sources
   * @since 7.0
   */
  public void setLightSourceMaterialNames(String [] lightSourceMaterialNames) {
    if (!Arrays.equals(lightSourceMaterialNames, this.lightSourceMaterialNames)) {
      String [] oldLightSourceMaterialNames = this.lightSourceMaterialNames.length == 0
          ? this.lightSourceMaterialNames
          : this.lightSourceMaterialNames.clone();
      this.lightSourceMaterialNames = lightSourceMaterialNames.length == 0
          ? lightSourceMaterialNames
          : lightSourceMaterialNames.clone();
      firePropertyChange(Property.LIGHT_SOURCE_MATERIAL_NAMES.name(), oldLightSourceMaterialNames, lightSourceMaterialNames);
    }
  }

  /**
   * Returns the power of this light.
   * @since 3.0
   */
  public float getPower() {
    return this.power;
  }

  /**
   * Sets the power of this light. Once this light is updated,
   * listeners added to this light will receive a change notification.
   * @param power power of the light
   * @since 3.0
   */
  public void setPower(float power) {
    if (power != this.power) {
      float oldPower = this.power;
      this.power = power;
      firePropertyChange(Property.POWER.name(), oldPower, power);
    }
  }

  /**
   * Returns the unit in which the power of this light is displayed.
   */
  public PowerUnit getPowerUnit() {
    return this.powerUnit != null
        ? this.powerUnit
        : PowerUnit.PERCENTAGE;
  }

  /**
   * Sets the unit in which the power of this light is displayed. Once this light is updated,
   * listeners added to this light will receive a change notification.
   * @param powerUnit the unit of the power of the light
   */
  public void setPowerUnit(PowerUnit powerUnit) {
    PowerUnit oldPowerUnit = getPowerUnit();
    if (powerUnit != oldPowerUnit) {
      this.powerUnit = powerUnit;
      firePropertyChange(Property.POWER_UNIT.name(), oldPowerUnit, powerUnit);
    }
  }

  /**
   * Returns the luminous flux in lumens emitted by this light.
   */
  public float getLuminousFlux() {
    return this.power * FULL_POWER_LUMINOUS_FLUX;
  }

  /**
   * Returns the RGB color of the light emitted by all the sources of this light,
   * or <code>null</code> if the sources use their own color.
   * This color is ignored when a {@linkplain #getLightColorTemperature() color temperature} is set.
   */
  public Integer getLightColor() {
    return this.lightColor;
  }

  /**
   * Sets the RGB color of the light emitted by all the sources of this light. Once this light is updated,
   * listeners added to this light will receive a change notification.
   * @param lightColor the color of the light or <code>null</code> if the sources should use their own color
   */
  public void setLightColor(Integer lightColor) {
    if (lightColor != this.lightColor
        && (lightColor == null || !lightColor.equals(this.lightColor))) {
      Integer oldLightColor = this.lightColor;
      this.lightColor = lightColor;
      firePropertyChange(Property.LIGHT_COLOR.name(), oldLightColor, lightColor);
    }
  }

  /**
   * Returns the color temperature in kelvins of the light emitted by all the sources of this light,
   * or <code>null</code> if the color of the light isn't defined by a temperature.
   */
  public Integer getLightColorTemperature() {
    return this.lightColorTemperature;
  }

  /**
   * Sets the color temperature in kelvins of the light emitted by all the sources of this light.
   * Once this light is updated, listeners added to this light will receive a change notification.
   * @param lightColorTemperature the temperature of the light or <code>null</code>
   *            if the color of the light shouldn't be defined by a temperature
   */
  public void setLightColorTemperature(Integer lightColorTemperature) {
    if (lightColorTemperature != this.lightColorTemperature
        && (lightColorTemperature == null || !lightColorTemperature.equals(this.lightColorTemperature))) {
      Integer oldLightColorTemperature = this.lightColorTemperature;
      this.lightColorTemperature = lightColorTemperature;
      firePropertyChange(Property.LIGHT_COLOR_TEMPERATURE.name(), oldLightColorTemperature, lightColorTemperature);
    }
  }

  /**
   * Returns the RGB color of the light emitted by the given source of this light,
   * i.e. the color matching the temperature of this light if it's set,
   * otherwise the color of this light if it's set, otherwise the color of the source.
   */
  public int getLightSourceColor(LightSource lightSource) {
    Integer emittedLightColor = getEmittedLightColor();
    return emittedLightColor != null
        ? emittedLightColor
        : lightSource.getColor();
  }

  /**
   * Returns the RGB color of the light emitted by this light if its temperature or its color is set,
   * or <code>null</code> if its sources use their own color.
   */
  public Integer getEmittedLightColor() {
    if (this.lightColorTemperature != null) {
      return getColorAtTemperature(this.lightColorTemperature);
    } else {
      return this.lightColor;
    }
  }

  /**
   * Returns an approximation of the RGB color of a black body at the given temperature in kelvins.
   */
  public static int getColorAtTemperature(int temperature) {
    // Algorithm of Tanner Helland fitted on the CIE 1964 10 degree color matching functions
    double hundredsOfKelvins = Math.max(1000, Math.min(temperature, 40000)) / 100.;
    double red;
    double green;
    double blue;
    if (hundredsOfKelvins <= 66) {
      red = 255;
      green = 99.4708025861 * Math.log(hundredsOfKelvins) - 161.1195681661;
      blue = hundredsOfKelvins <= 19
          ? 0
          : 138.5177312231 * Math.log(hundredsOfKelvins - 10) - 305.0447927307;
    } else {
      red = 329.698727446 * Math.pow(hundredsOfKelvins - 60, -0.1332047592);
      green = 288.1221695283 * Math.pow(hundredsOfKelvins - 60, -0.0755148492);
      blue = 255;
    }
    return (getColorComponent(red) << 16) | (getColorComponent(green) << 8) | getColorComponent(blue);
  }

  private static int getColorComponent(double value) {
    return (int)Math.round(Math.max(0, Math.min(value, 255)));
  }

  /**
   * Returns a clone of this light.
   */
  @Override
  public HomeLight clone() {
    return (HomeLight)super.clone();
  }
}
