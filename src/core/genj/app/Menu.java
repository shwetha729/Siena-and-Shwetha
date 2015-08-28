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
import genj.gedcom.GedcomListener;
import genj.gedcom.GedcomListenerAdapter;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.Trackable;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.util.swing.MacAdapter;
import genj.util.swing.MenuHelper;
import genj.view.ActionProvider;
import genj.view.SelectionSink;
import genj.view.View;
import genj.view.ViewFactory;
import genj.view.ActionProvider.Purpose;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import spin.Spin;

/**
 * Our MenuBar
 */
/*package*/ class Menu extends JMenuBar implements SelectionSink, WorkbenchListener {
  
  private final static Logger LOG = Logger.getLogger("genj.app");
  private final static Resources RES = Resources.get(Menu.class);
  private final static Registry REGISTRY = Registry.get(Menu.class);
  
  private Workbench workbench;
  private Gedcom gedcom;
  private List<Action> actions = new CopyOnWriteArrayList<Action>();
  private GedcomListener callback = new GedcomListenerAdapter() {
    @Override
    public void gedcomWriteLockReleased(Gedcom gedcom) {
      setup(gedcom);
    }
  };
  
  /*package*/ Menu(Workbench workbench) {
    this.workbench = workbench;
    workbench.addWorkbenchListener(this);
    setup(null);
  }
  
  // we need to play delegate for selectionsink since the menu is not a child 
  // of Workbench but the window's root-pane - selections bubbling up the
  // window hierarchy are otherwise running into null-ness
  public void fireSelection(Context context, boolean isActionPerformed) {
    workbench.fireSelection(context, isActionPerformed);
  }
  
  private void remove(Action action) {
    
    if (gedcom!=null&&action instanceof GedcomListener)
      gedcom.removeGedcomListener((GedcomListener)Spin.over(action));
    
    if (action instanceof WorkbenchListener)
      workbench.removeWorkbenchListener((WorkbenchListener)action);
    
    actions.remove(action);
  }
  
  private void add(Action action) {
    // remember
    actions.add(action);
    // connection
    if (gedcom!=null&&action instanceof GedcomListener)
      gedcom.addGedcomListener((GedcomListener)Spin.over(action));
    if (action instanceof WorkbenchListener)
      workbench.addWorkbenchListener((WorkbenchListener)action);
  }
  
  private void setup(Gedcom gedcom) {

    // tear down
    if (this.gedcom!=null)
      this.gedcom.removeGedcomListener(callback);
    for (Action action : actions)
      remove(action);
    this.gedcom = gedcom;
    removeAll();
    revalidate();
    repaint();

    // build up
    if (this.gedcom!=null)
      this.gedcom.addGedcomListener(callback);
    Action2.Group groups = new Action2.Group("ignore");
    
    // File
    Action2.Group file = new ActionProvider.FileActionGroup();
    groups.add(file);
    file.add(workbench.new ActionNew());
    file.add(workbench.new ActionOpen());
    file.add(workbench.new ActionSave(false));
    file.add(workbench.new ActionSave(true));
    file.add(workbench.new ActionClose());
    file.add(new ActionProvider.SeparatorAction());
    int i=0; for (String recent : Registry.get(Workbench.class).get("history", new ArrayList<String>())) try {
      if (gedcom==null||!recent.equals(gedcom.getOrigin().toString()))
        file.add(workbench.new ActionOpen(i++, new URL(recent)));
    } catch (MalformedURLException e) { }
    file.add(new ActionProvider.SeparatorAction());
    if (!MacAdapter.isMac())   // Mac's don't need exit actions in
      file.add(workbench.new ActionExit()); // application menus apparently
    
    // Edit
    groups.add(new ActionProvider.EditActionGroup());
    
    // Views
    Action2.Group views = new ActionProvider.ViewActionGroup();
    groups.add(views);
    for (ViewFactory factory : workbench.getViewFactories()) 
      views.add(workbench.new ActionOpenView(factory));
    views.add(new ActionProvider.SeparatorAction());
    
    // Tools
    Action2.Group tools = new ActionProvider.ToolsActionGroup();
    groups.add(tools);
    
    // merge providers' actions
    Action2.Group provided = new Action2.Group("ignore");
    for (ActionProvider provider : workbench.getProviders(ActionProvider.class)) {
      provider.createActions(workbench.getContext(), Purpose.MENU, provided);
      for (Action2 action : provided) {
        if (action instanceof Action2.Group) {
          groups.add(action);
        } else {
          tools.add(action);
        }
      }
      provided.clear();
    }
    
    Action2.Group edit = new ActionProvider.EditActionGroup();
    edit.add(new ActionProvider.SeparatorAction());
    if (!MacAdapter.isMac())
      edit.add(new ActionOptions());
    groups.add(edit);

    Action2.Group help = new ActionProvider.HelpActionGroup();
    help.add(new ActionSupport());
    help.add(new ActionProvider.SeparatorAction());
    help.add(new ActionLog());
    if (!MacAdapter.isMac())
      help.add(new ActionAbout());
    groups.add(help);
    
    // Build menu
    MenuHelper mh = new MenuHelper() {
      @Override
      protected void set(Action action, JMenuItem item) {
        add(action);
        super.set(new ActionProxy(action), item);
      }
    };
    
    mh.pushMenu(this);
    
    for (Action2 group : groups) {
      Action2.Group subgroup = (Action2.Group)group;
      if (subgroup.size()>0) {
        mh.createMenu(subgroup);
        mh.popMenu();
      }
    }
    
    // Done
  }
  
  // 20060209 don't use a glue component to move help all the way over to the right
  // (Reminder: according to Stephane this doesn't work on MacOS Tiger)
  // java.lang.ArrayIndexOutOfBoundsException: 3 > 2::
  // at java.util.Vector.insertElementAt(Vector.java:557)::
  // at apple.laf.ScreenMenuBar.add(ScreenMenuBar.java:266)::
  // at apple.laf.ScreenMenuBar.addSubmenu(ScreenMenuBar.java:207)::
  // at apple.laf.ScreenMenuBar.addNotify(ScreenMenuBar.java:53)::
  // at java.awt.Frame.addNotify(Frame.java:478)::
  // at java.awt.Window.pack(Window.java:436)::
  // http://lists.apple.com/archives/java-dev/2005/Aug/msg00060.html
  
  public void commitRequested(Workbench workbench) {
  }

  public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
    setup(null);
  }

  public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
    setup(gedcom);
  }

  public void processStarted(Workbench workbench, Trackable process) {
  }

  public void processStopped(Workbench workbench, Trackable process) {
  }

  public void selectionChanged(Workbench workbench, Context context, boolean isActionPerformed) {
    setup(context.getGedcom());
  }

  public void viewClosed(Workbench workbench, View view) {
  }
  
  public void viewOpened(Workbench workbench, View view) {
  }
  
  public void workbenchClosing(Workbench workbench) {
  }
  
  private class ActionProxy implements Action {
    private Action delegate;
    public ActionProxy(Action delegate) {
      this.delegate = delegate;
    }
    public void addPropertyChangeListener(PropertyChangeListener listener) {
      delegate.addPropertyChangeListener(listener);
    }
    public Object getValue(String key) {
      return delegate.getValue(key);
    }
    public boolean isEnabled() {
      return delegate.isEnabled();
    }
    public void putValue(String key, Object value) {
      delegate.putValue(key, value);
    }
    public void removePropertyChangeListener(PropertyChangeListener listener) {
      delegate.removePropertyChangeListener(listener);
    }
    public void setEnabled(boolean b) {
      delegate.setEnabled(b);
    }
    public void actionPerformed(ActionEvent e) {
      delegate.actionPerformed(e);
      setup(gedcom);
    }
  };
  
  
  static class ActionSupport extends Action2 {
    
    private final static ImageIcon IMG = new ImageIcon(ActionAbout.class,"images/About.png");
    
    /** constructor */
    protected ActionSupport() {
      setText("Support");
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      try {
        Desktop.getDesktop().browse(new URI("http://genj.sourceforge.net/wiki/en/support"));
      } catch (Throwable t) {
      }
    }
  }
  
} // Menu

