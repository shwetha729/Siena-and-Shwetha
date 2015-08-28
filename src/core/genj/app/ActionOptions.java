/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2010 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.app;

import genj.option.OptionProvider;
import genj.option.OptionsWidget;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;

import java.awt.event.ActionEvent;

/**
 * show options
 */
/*package*/ class ActionOptions extends Action2 {
  
  private final static Resources RES = Resources.get(Workbench.class);
  
  /** constructor */
  protected ActionOptions() {
    setText(RES.getString("cc.menu.options"));
    setImage(OptionsWidget.IMAGE);
  }

  /** run */
  public void actionPerformed(ActionEvent event) {
    // create widget for options
    OptionsWidget widget = new OptionsWidget(getText());
    widget.setOptions(OptionProvider.getAllOptions());
    // open dialog
    DialogHelper.openDialog(getText(), DialogHelper.INFORMATION_MESSAGE, widget, Action2.okOnly(), event);
    // done
  }
}