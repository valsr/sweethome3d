/*
 * GuidelinePanel.java
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.eteks.sweethome3d.model.Guideline;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.GuidelineController;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Guideline editing panel.
 */
public class GuidelinePanel extends JPanel implements DialogView {
  private final GuidelineController controller;
  private JLabel                    orientationLabel;
  private JLabel                    positionLabel;
  private JSpinner                  positionSpinner;
  private JLabel                    colorLabel;
  private ColorButton               colorButton;
  private String                    dialogTitle;

  /**
   * Creates a panel that displays guideline data according to the units set in
   * <code>preferences</code>.
   * @param preferences user preferences
   * @param controller the controller of this panel
   */
  public GuidelinePanel(UserPreferences preferences,
                        GuidelineController controller) {
    super(new GridBagLayout());
    this.controller = controller;
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
  }

  /**
   * Creates and initializes components and spinners model.
   */
  private void createComponents(final UserPreferences preferences,
                                final GuidelineController controller) {
    this.orientationLabel = new JLabel();
    this.positionLabel = new JLabel();
    PropertyChangeListener orientationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateOrientationLabels(preferences, controller.getOrientation());
        }
      };
    controller.addPropertyChangeListener(GuidelineController.Property.ORIENTATION, orientationChangeListener);
    updateOrientationLabels(preferences, controller.getOrientation());

    // Create position spinner bound to POSITION controller property
    final float maximumLength = preferences.getLengthUnit().getMaximumLength();
    final NullableSpinner.NullableSpinnerLengthModel positionSpinnerModel =
        new NullableSpinner.NullableSpinnerLengthModel(preferences, -maximumLength, maximumLength);
    this.positionSpinner = new NullableSpinner(positionSpinnerModel);
    positionSpinnerModel.setNullable(controller.getPosition() == null);
    positionSpinnerModel.setLength(controller.getPosition());
    final PropertyChangeListener positionChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          positionSpinnerModel.setNullable(ev.getNewValue() == null);
          positionSpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(GuidelineController.Property.POSITION, positionChangeListener);
    positionSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(GuidelineController.Property.POSITION, positionChangeListener);
          controller.setPosition(positionSpinnerModel.getLength());
          controller.addPropertyChangeListener(GuidelineController.Property.POSITION, positionChangeListener);
        }
      });

    // Create color label and its button bound to COLOR controller property
    this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
        GuidelinePanel.class, "colorLabel.text"));
    this.colorButton = new ColorButton(preferences);
    if (OperatingSystem.isMacOSX()) {
      this.colorButton.putClientProperty("JButton.buttonType", "segmented");
      this.colorButton.putClientProperty("JButton.segmentPosition", "only");
    }
    this.colorButton.setColorDialogTitle(preferences
        .getLocalizedString(GuidelinePanel.class, "colorDialog.title"));
    this.colorButton.setColor(controller.getColor());
    this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          controller.setColor(colorButton.getColor());
        }
      });
    controller.addPropertyChangeListener(GuidelineController.Property.COLOR, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          colorButton.setColor(controller.getColor());
        }
      });

    this.dialogTitle = preferences.getLocalizedString(GuidelinePanel.class, "modifyGuidelines.title");
  }

  /**
   * Updates the texts of the labels which depend on the orientation of the edited guidelines.
   */
  private void updateOrientationLabels(UserPreferences preferences, Guideline.Orientation orientation) {
    String unitName = preferences.getLengthUnit().getName();
    if (orientation == Guideline.Orientation.HORIZONTAL) {
      this.orientationLabel.setText(preferences.getLocalizedString(GuidelinePanel.class, "horizontalGuidelineLabel.text"));
      this.positionLabel.setText(SwingTools.getLocalizedLabelText(preferences, GuidelinePanel.class, "yLabel.text", unitName));
    } else if (orientation == Guideline.Orientation.VERTICAL) {
      this.orientationLabel.setText(preferences.getLocalizedString(GuidelinePanel.class, "verticalGuidelineLabel.text"));
      this.positionLabel.setText(SwingTools.getLocalizedLabelText(preferences, GuidelinePanel.class, "xLabel.text", unitName));
    } else {
      this.orientationLabel.setText(preferences.getLocalizedString(GuidelinePanel.class, "guidelinesLabel.text"));
      this.positionLabel.setText(SwingTools.getLocalizedLabelText(preferences, GuidelinePanel.class, "positionLabel.text", unitName));
    }
  }

  /**
   * Sets components mnemonics and label / component associations.
   */
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.positionLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
          preferences.getLocalizedString(GuidelinePanel.class, "positionLabel.mnemonic")).getKeyCode());
      this.positionLabel.setLabelFor(this.positionSpinner);
    }
  }

  /**
   * Layouts panel components in panel with their labels.
   */
  private void layoutComponents() {
    int labelAlignment = OperatingSystem.isMacOSX()
        ? GridBagConstraints.LINE_END
        : GridBagConstraints.LINE_START;
    int standardGap = Math.round(5 * SwingTools.getResolutionScale());
    add(this.orientationLabel, new GridBagConstraints(
        0, 0, 2, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
    add(this.positionLabel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, labelAlignment,
        GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
    add(this.positionSpinner, new GridBagConstraints(
        1, 1, 1, 1, 1, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, standardGap, 0), 0, 0));
    add(this.colorLabel, new GridBagConstraints(
        0, 2, 1, 1, 0, 0, labelAlignment,
        GridBagConstraints.NONE, new Insets(0, 0, 0, standardGap), 0, 0));
    add(this.colorButton, new GridBagConstraints(
        1, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    // Keep components at the top when more height is available
    add(new JLabel(), new GridBagConstraints(
        0, 3, 2, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
  }

  /**
   * Displays this panel in a modal dialog box.
   */
  public void displayView(View parentView) {
    if (SwingTools.showConfirmDialog((JComponent)parentView, this, this.dialogTitle,
          ((JSpinner.DefaultEditor)this.positionSpinner.getEditor()).getTextField()) == JOptionPane.OK_OPTION
        && this.controller != null) {
      this.controller.modifyGuidelines();
    }
  }
}
