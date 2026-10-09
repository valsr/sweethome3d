/*
 * PrimarySideBar.java
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
package com.eteks.sweethome3d.swing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.eteks.sweethome3d.model.UserPreferences;

/**
 * A side bar displaying {@linkplain CollapsibleSection collapsible sections} stacked vertically.
 * The side bar itself may be collapsed with the toggle button of its {@linkplain #getEdgeStrip() edge strip},
 * which should be displayed out of this side bar to remain visible once the side bar is collapsed.
 */
public class PrimarySideBar extends JPanel {
  /**
   * The name of the bound property fired when this side bar is collapsed or expanded.
   */
  public static final String COLLAPSED_PROPERTY = "collapsed";
  /**
   * The name of the property fired when the user changed the weights of the sections.
   */
  public static final String SECTION_WEIGHTS_PROPERTY = "sectionWeights";

  private final UserPreferences                preferences;
  private final List<CollapsibleSection>       sections = new ArrayList<CollapsibleSection>();
  private final Map<CollapsibleSection, Float> sectionWeights = new HashMap<CollapsibleSection, Float>();
  private final JPanel                         edgeStrip;
  private final JButton                        toggleButton;
  private boolean                              collapsed;

  /**
   * Creates an empty expanded side bar.
   */
  public PrimarySideBar(UserPreferences preferences) {
    this.preferences = preferences;
    setLayout(new SectionsLayout());
    setMinimumSize(new Dimension());

    this.toggleButton = new JButton(new ToggleIcon());
    this.toggleButton.setBorder(BorderFactory.createEmptyBorder());
    this.toggleButton.setContentAreaFilled(false);
    this.toggleButton.setFocusable(false);
    this.toggleButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          setCollapsed(!isCollapsed());
        }
      });
    this.edgeStrip = new JPanel(new BorderLayout());
    this.edgeStrip.add(this.toggleButton, BorderLayout.NORTH);
    updateToggleButtonToolTip();
  }

  /**
   * Adds a section at the bottom of this side bar. The height available for the contents
   * of the expanded sections is shared between them in proportion to their weight.
   */
  public void addSection(CollapsibleSection section, float weight) {
    this.sections.add(section);
    this.sectionWeights.put(section, weight);
    section.addPropertyChangeListener(CollapsibleSection.COLLAPSED_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          revalidate();
        }
      });
    section.setHeaderDragListener(new SectionResizer());
    add(section);
    revalidate();
  }

  /**
   * Returns the sections displayed by this side bar from top to bottom.
   */
  public List<CollapsibleSection> getSections() {
    return new ArrayList<CollapsibleSection>(this.sections);
  }

  /**
   * Returns the weight of the given <code>section</code>.
   */
  public float getSectionWeight(CollapsibleSection section) {
    return this.sectionWeights.get(section);
  }

  /**
   * Sets the weight of the given <code>section</code>.
   */
  public void setSectionWeight(CollapsibleSection section, float weight) {
    this.sectionWeights.put(section, Math.max(0, weight));
    revalidate();
  }

  /**
   * Returns the strip which contains the button used to collapse or expand this side bar.
   */
  public JComponent getEdgeStrip() {
    return this.edgeStrip;
  }

  /**
   * Returns the button which collapses or expands this side bar.
   */
  public JButton getToggleButton() {
    return this.toggleButton;
  }

  /**
   * Returns <code>true</code> if this side bar is collapsed, i.e. hidden.
   */
  public boolean isCollapsed() {
    return this.collapsed;
  }

  /**
   * Collapses or expands this side bar.
   */
  public void setCollapsed(boolean collapsed) {
    if (collapsed != this.collapsed) {
      this.collapsed = collapsed;
      setVisible(!collapsed);
      updateToggleButtonToolTip();
      this.toggleButton.repaint();
      firePropertyChange(COLLAPSED_PROPERTY, !collapsed, collapsed);
    }
  }

  private void updateToggleButtonToolTip() {
    this.toggleButton.setToolTipText(this.preferences.getLocalizedString(PrimarySideBar.class,
        this.collapsed ? "expandButton.tooltip" : "collapseButton.tooltip"));
  }

  /**
   * Returns the height of the content of the given section.
   */
  private int getContentHeight(CollapsibleSection section) {
    return section.isCollapsed()
        ? 0
        : Math.max(0, section.getHeight() - section.getHeader().getPreferredSize().height);
  }

  /**
   * Resizes the expanded sections around the header dragged by the user.
   */
  private class SectionResizer implements CollapsibleSection.HeaderDragListener {
    private CollapsibleSection upperSection;
    private CollapsibleSection lowerSection;
    private int                pressedY;
    private int                upperSectionHeight;
    private int                lowerSectionHeight;

    public void headerPressed(CollapsibleSection section, MouseEvent ev) {
      this.pressedY = SwingUtilities.convertPoint(ev.getComponent(), ev.getPoint(), PrimarySideBar.this).y;
      // Search the closest expanded sections on both sides of the top of the header
      int sectionIndex = sections.indexOf(section);
      this.upperSection = null;
      for (int i = sectionIndex - 1; i >= 0 && this.upperSection == null; i--) {
        if (!sections.get(i).isCollapsed()) {
          this.upperSection = sections.get(i);
        }
      }
      this.lowerSection = null;
      for (int i = sectionIndex; i < sections.size() && this.lowerSection == null; i++) {
        if (!sections.get(i).isCollapsed()) {
          this.lowerSection = sections.get(i);
        }
      }
      if (this.upperSection != null && this.lowerSection != null) {
        this.upperSectionHeight = getContentHeight(this.upperSection);
        this.lowerSectionHeight = getContentHeight(this.lowerSection);
      }
    }

    public void headerDragged(CollapsibleSection section, MouseEvent ev) {
      if (this.upperSection != null && this.lowerSection != null) {
        int y = SwingUtilities.convertPoint(ev.getComponent(), ev.getPoint(), PrimarySideBar.this).y;
        int delta = Math.max(-this.upperSectionHeight, Math.min(y - this.pressedY, this.lowerSectionHeight));
        // Use the current heights of the contents as weights
        for (CollapsibleSection expandedSection : sections) {
          if (!expandedSection.isCollapsed()) {
            sectionWeights.put(expandedSection, (float)getContentHeight(expandedSection));
          }
        }
        sectionWeights.put(this.upperSection, (float)(this.upperSectionHeight + delta));
        sectionWeights.put(this.lowerSection, (float)(this.lowerSectionHeight - delta));
        revalidate();
        firePropertyChange(SECTION_WEIGHTS_PROPERTY, null, null);
      }
    }
  }

  /**
   * A layout which stacks sections and shares the available height between the expanded ones.
   */
  private class SectionsLayout implements LayoutManager {
    public void addLayoutComponent(String name, Component component) {
    }

    public void removeLayoutComponent(Component component) {
    }

    public Dimension preferredLayoutSize(Container parent) {
      Insets insets = parent.getInsets();
      int width = 0;
      int height = 0;
      for (CollapsibleSection section : sections) {
        Dimension size = section.isCollapsed()
            ? section.getHeader().getPreferredSize()
            : section.getPreferredSize();
        width = Math.max(width, size.width);
        height += size.height;
      }
      return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    public Dimension minimumLayoutSize(Container parent) {
      return new Dimension();
    }

    public void layoutContainer(Container parent) {
      Insets insets = parent.getInsets();
      int width = parent.getWidth() - insets.left - insets.right;
      int availableHeight = parent.getHeight() - insets.top - insets.bottom;
      float weightsSum = 0;
      int expandedSectionsCount = 0;
      CollapsibleSection lastExpandedSection = null;
      for (CollapsibleSection section : sections) {
        availableHeight -= section.getHeader().getPreferredSize().height;
        if (!section.isCollapsed()) {
          weightsSum += sectionWeights.get(section);
          expandedSectionsCount++;
          lastExpandedSection = section;
        }
      }
      availableHeight = Math.max(0, availableHeight);

      int y = insets.top;
      int remainingHeight = availableHeight;
      for (CollapsibleSection section : sections) {
        int height = section.getHeader().getPreferredSize().height;
        if (!section.isCollapsed()) {
          int contentHeight;
          if (section == lastExpandedSection) {
            // Give all the remaining pixels to the last section to avoid rounding issues
            contentHeight = remainingHeight;
          } else if (weightsSum > 0) {
            contentHeight = Math.min(remainingHeight,
                Math.round(availableHeight * sectionWeights.get(section) / weightsSum));
          } else {
            contentHeight = Math.min(remainingHeight, availableHeight / expandedSectionsCount);
          }
          remainingHeight -= contentHeight;
          height += contentHeight;
        }
        section.setBounds(insets.left, y, width, height);
        y += height;
      }
    }
  }

  /**
   * An icon displaying a double arrow oriented towards the direction where the side bar will move.
   */
  private class ToggleIcon implements Icon {
    public int getIconWidth() {
      return Math.round(14 * SwingTools.getResolutionScale());
    }

    public int getIconHeight() {
      return Math.round(22 * SwingTools.getResolutionScale());
    }

    public void paintIcon(Component component, Graphics g, int x, int y) {
      Graphics2D g2D = (Graphics2D)g.create();
      g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2D.setColor(component.getForeground());
      g2D.setStroke(new BasicStroke(1.5f * SwingTools.getResolutionScale(),
          BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      float arrowSize = getIconWidth() / 4f;
      float centerX = x + getIconWidth() / 2f;
      float centerY = y + getIconHeight() / 2f;
      // Arrows point to the edge when the side bar is expanded
      boolean pointsToLeft = component.getComponentOrientation().isLeftToRight() != collapsed;
      float direction = pointsToLeft ? -1 : 1;
      Path2D arrows = new Path2D.Float();
      for (int i = 0; i < 2; i++) {
        float arrowX = centerX + (i - 0.5f) * arrowSize * 1.2f;
        arrows.moveTo(arrowX - direction * arrowSize / 2, centerY - arrowSize);
        arrows.lineTo(arrowX + direction * arrowSize / 2, centerY);
        arrows.lineTo(arrowX - direction * arrowSize / 2, centerY + arrowSize);
      }
      g2D.draw(arrows);
      g2D.dispose();
    }
  }
}
