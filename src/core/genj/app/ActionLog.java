/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2009 Nils Meier <nils@meiers.net>
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

import genj.gedcom.PropertyFile;
import genj.util.EnvironmentChecker;
import genj.util.swing.Action2;

import java.awt.Desktop;
import java.awt.event.ActionEvent;

/**
 * Show log file
 */
/*package*/ class ActionLog extends Action2 {
  
  /** constructor */
  protected ActionLog() {
    setText("Log");
    setImage(PropertyFile.DEFAULT_IMAGE);
  }

  /** run */
  public void actionPerformed(ActionEvent event) {
    try {
      Desktop.getDesktop().open(EnvironmentChecker.getLog());
    } catch (Throwable t) {
    }
  }
}