/*
 * CollapsibleSection.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * A panel displaying a header and a content which can be collapsed to show only its header.
 */
public class CollapsibleSection extends JPanel {
  /**
   * The name of the bound property fired when this section is collapsed or expanded.
   */
  public static final String COLLAPSED_PROPERTY = "collapsed";

  private final String       title;
  private final JComponent   header;
  private JComponent         content;
  private boolean            collapsed;
  private HeaderDragListener headerDragListener;

  /**
   * Creates an expanded section with the given <code>title</code> and <code>content</code>.
   */
  public CollapsibleSection(String title, JComponent content) {
    super(new BorderLayout());
    this.title = title;
    this.header = new Header();
    add(this.header, BorderLayout.NORTH);
    setMinimumSize(new Dimension());
    setContent(content);
  }

  /**
   * Returns the title displayed in the header of this section.
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * Returns the header of this section. A click on it collapses or expands this section.
   */
  public JComponent getHeader() {
    return this.header;
  }

  /**
   * Returns the component displayed under the header.
   */
  public JComponent getContent() {
    return this.content;
  }

  /**
   * Replaces the component displayed under the header.
   */
  public void setContent(JComponent content) {
    if (this.content != null) {
      remove(this.content);
    }
    this.content = content;
    content.setVisible(!this.collapsed);
    add(content, BorderLayout.CENTER);
    revalidate();
    repaint();
  }

  /**
   * Returns <code>true</code> if only the header of this section is displayed.
   */
  public boolean isCollapsed() {
    return this.collapsed;
  }

  /**
   * Collapses or expands this section.
   */
  public void setCollapsed(boolean collapsed) {
    if (collapsed != this.collapsed) {
      this.collapsed = collapsed;
      this.content.setVisible(!collapsed);
      revalidate();
      repaint();
      firePropertyChange(COLLAPSED_PROPERTY, !collapsed, collapsed);
    }
  }

  /**
   * Sets the listener notified when the user drags the header of this section.
   */
  void setHeaderDragListener(HeaderDragListener headerDragListener) {
    this.headerDragListener = headerDragListener;
  }

  /**
   * A listener notified when the header of a section is pressed then dragged.
   */
  static interface HeaderDragListener {
    public void headerPressed(CollapsibleSection section, MouseEvent ev);

    public void headerDragged(CollapsibleSection section, MouseEvent ev);
  }

  /**
   * The header of a section displaying an arrow followed by its title.
   */
  private class Header extends JComponent {
    public Header() {
      setFocusable(true);
      setOpaque(true);
      Font font = UIManager.getFont("Label.font");
      if (font != null) {
        setFont(font.deriveFont(Font.BOLD, font.getSize2D() * 0.9f));
      }
      getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
      getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggle");
      getActionMap().put("toggle", new AbstractAction() {
          public void actionPerformed(ActionEvent ev) {
            setCollapsed(!isCollapsed());
          }
        });
      MouseAdapter mouseListener = new MouseAdapter() {
          private int     pressedY;
          private boolean dragged;

          @Override
          public void mousePressed(MouseEvent ev) {
            if (SwingUtilities.isLeftMouseButton(ev)) {
              this.pressedY = ev.getY();
              this.dragged = false;
              requestFocusInWindow();
              if (headerDragListener != null) {
                headerDragListener.headerPressed(CollapsibleSection.this, ev);
              }
            }
          }

          @Override
          public void mouseDragged(MouseEvent ev) {
            if (SwingUtilities.isLeftMouseButton(ev)) {
              // Ignore small moves to avoid handling a shaky click as a drag
              if (!this.dragged
                  && Math.abs(ev.getY() - this.pressedY) > 3 * SwingTools.getResolutionScale()) {
                this.dragged = true;
              }
              if (this.dragged && headerDragListener != null) {
                headerDragListener.headerDragged(CollapsibleSection.this, ev);
              }
            }
          }

          @Override
          public void mouseReleased(MouseEvent ev) {
            if (SwingUtilities.isLeftMouseButton(ev) && !this.dragged) {
              setCollapsed(!isCollapsed());
            }
          }
        };
      addMouseListener(mouseListener);
      addMouseMotionListener(mouseListener);
      addFocusListener(new FocusListener() {
          public void focusGained(FocusEvent ev) {
            repaint();
          }

          public void focusLost(FocusEvent ev) {
            repaint();
          }
        });
    }

    @Override
    public Dimension getPreferredSize() {
      FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : new Font(Font.DIALOG, Font.BOLD, 11));
      int gap = Math.round(3 * SwingTools.getResolutionScale());
      return new Dimension(metrics.stringWidth(title) + metrics.getHeight() + 4 * gap,
          metrics.getHeight() + 2 * gap);
    }

    @Override
    protected void paintComponent(Graphics g) {
      Graphics2D g2D = (Graphics2D)g.create();
      g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      Color background = CollapsibleSection.this.getBackground();
      Color foreground = CollapsibleSection.this.getForeground();
      // Fill header with a color slightly different from the background
      g2D.setColor(isDark(background) ? background.brighter() : background.darker());
      g2D.fillRect(0, 0, getWidth(), 1);
      g2D.setColor(mix(background, foreground, 0.08f));
      g2D.fillRect(0, 1, getWidth(), getHeight() - 1);

      FontMetrics metrics = g2D.getFontMetrics(getFont());
      int gap = Math.round(3 * SwingTools.getResolutionScale());
      float arrowSize = metrics.getAscent() * 0.5f;
      boolean leftToRight = getComponentOrientation().isLeftToRight();
      float arrowCenterX = leftToRight
          ? gap + metrics.getHeight() / 2f
          : getWidth() - gap - metrics.getHeight() / 2f;
      float arrowCenterY = getHeight() / 2f;
      g2D.setColor(foreground);
      Path2D arrow = new Path2D.Float();
      if (collapsed) {
        // Arrow pointing to the content side
        float direction = leftToRight ? 1 : -1;
        arrow.moveTo(arrowCenterX - direction * arrowSize / 2, arrowCenterY - arrowSize);
        arrow.lineTo(arrowCenterX + direction * arrowSize / 2, arrowCenterY);
        arrow.lineTo(arrowCenterX - direction * arrowSize / 2, arrowCenterY + arrowSize);
      } else {
        // Arrow pointing down
        arrow.moveTo(arrowCenterX - arrowSize, arrowCenterY - arrowSize / 2);
        arrow.lineTo(arrowCenterX, arrowCenterY + arrowSize / 2);
        arrow.lineTo(arrowCenterX + arrowSize, arrowCenterY - arrowSize / 2);
      }
      g2D.setStroke(new java.awt.BasicStroke(1.5f * SwingTools.getResolutionScale(),
          java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
      g2D.draw(arrow);

      g2D.setFont(getFont());
      int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
      int textX = leftToRight
          ? 2 * gap + metrics.getHeight()
          : getWidth() - 2 * gap - metrics.getHeight() - metrics.stringWidth(title);
      g2D.drawString(title, textX, textY);

      if (isFocusOwner()) {
        Color focusColor = UIManager.getColor("Focus.color");
        g2D.setColor(focusColor != null ? focusColor : foreground);
        g2D.setStroke(new java.awt.BasicStroke(1));
        g2D.drawRect(1, 2, getWidth() - 3, getHeight() - 4);
      }
      g2D.dispose();
    }

    private boolean isDark(Color color) {
      return color.getRed() + color.getGreen() + color.getBlue() < 3 * 128;
    }

    private Color mix(Color color1, Color color2, float ratio) {
      return new Color(
          Math.round(color1.getRed() + (color2.getRed() - color1.getRed()) * ratio),
          Math.round(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * ratio),
          Math.round(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * ratio));
    }
  }
}
