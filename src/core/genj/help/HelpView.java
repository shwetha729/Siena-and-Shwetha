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
package genj.help;

import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.view.ToolBar;
import genj.view.View;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

/**
 * help in workbench
 */
public class HelpView extends View {
  
  public final static ImageIcon IMG = new ImageIcon(HelpView.class,"Help.png");

  private HelpWidget content = new HelpWidget();
  private Back back;
  private Forward forward;
  
  public HelpView() {
    super(new BorderLayout());
    add(content, BorderLayout.CENTER);
    
    back = new Back();
    forward = new Forward();
   
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (content.getPage()==null)
          content.setPage(HelpWidget.MANUAL);
      }
    });
  }

  public void setPage(String page) {
    content.setPage(page);
  }
  
  @Override
  public void populate(ToolBar toolbar) {
    toolbar.add(back);
    toolbar.add(forward);
  }
  
  private class Back extends Action2 implements PropertyChangeListener {
    
    private ArrayList<String> urls = new ArrayList<String>();
    public Back() {
      setImage(new ImageIcon(this, "Back.png"));
      content.addPropertyChangeListener("url", this);
      setEnabled(false);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      String old = (String)evt.getOldValue();
      if (old!=null) {
        if (urls.isEmpty() || !urls.get(urls.size()-1).equals(old)) 
          urls.add(old);
        setEnabled(true);
      }
      forward.clear();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      String old = content.getPage();
      String page = urls.get(urls.size()-1);
      content.removePropertyChangeListener("url", this);
      content.setPage(page);
      content.addPropertyChangeListener("url", this);
      urls.remove(urls.size()-1);
      setEnabled(!urls.isEmpty());
      if (old!=null)
        forward.push(old);
    }
  }
  
  private class Forward extends Action2 {
    
    private ArrayList<String> pages = new ArrayList<String>();
    
    Forward() {
      setImage(new ImageIcon(this, "Forward.png"));
      setEnabled(false);
    }
    
    void clear() {
      pages.clear();
      setEnabled(false);
    }
    
    void push(String page) {
      pages.add(page);
      setEnabled(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      content.setPage(pages.remove(pages.size()-1));
      setEnabled(!pages.isEmpty());
    }
  }
}
