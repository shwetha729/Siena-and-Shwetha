/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
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
package genj.edit.actions;

import genj.gedcom.Context;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Swap HUSB/WIFE for family
 */
public class SwapSpouses extends AbstractChange {
  
  /** fam */
  private List<Fam> fams;
  
  /**
   * Constructor
   */
  public SwapSpouses(Fam family) {
    super(family.getGedcom(), family.getImage(false), resources.getString("swap.spouses"));
    fams = Collections.singletonList(family);
  }
  
  /**
   * Constructor
   */
  public SwapSpouses(List<Fam> families) {
    super(families.get(0).getGedcom(), families.get(0).getImage(false), resources.getString("swap.spouses"));
    fams = new ArrayList<Fam>(families);
  }
  
  /**
   * @see genj.edit.actions.AbstractChange#change()
   */
  protected Context execute(Gedcom gedcom, ActionEvent event) throws GedcomException {
	  for (Fam fam : fams)
		  fam.swapSpouses();
	  return null;
  }

} //SwapSpouses
