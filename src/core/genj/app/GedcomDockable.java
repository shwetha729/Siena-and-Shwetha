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

import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.util.Trackable;
import genj.util.swing.ImageIcon;
import genj.view.View;

import javax.swing.JComponent;

import swingx.docking.DefaultDockable;
import swingx.docking.Docked;

/**
 * A dockable for temporary content - auto-closes when gedcom is closed
 */
/*package*/ class GedcomDockable extends DefaultDockable implements WorkbenchListener {
  
  private Workbench workbench;
  
  /*package*/ GedcomDockable(Workbench workbench, String title, ImageIcon img, JComponent content) {
    this.workbench = workbench;
    setContent(content);
    setTitle(title);
    setIcon(img);
  }
  
  @Override
  public void docked(Docked docked) {
    super.docked(docked);
    workbench.addWorkbenchListener(this);
  }
  
  @Override
  public void undocked() {
    super.undocked();
    workbench.removeWorkbenchListener(this);
  }

  public void commitRequested(Workbench workbench) {
  }

  public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
    workbench.closeDockable(this);
  }

  public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
  }

  public void processStarted(Workbench workbench, Trackable process) {
  }

  public void processStopped(Workbench workbench, Trackable process) {
  }

  public void selectionChanged(Workbench workbench, Context context, boolean isActionPerformed) {
  }

  public void viewClosed(Workbench workbench, View view) {
  }
  
  public void viewOpened(Workbench workbench, View view) {
  }

  public void workbenchClosing(Workbench workbench) {
  }
}