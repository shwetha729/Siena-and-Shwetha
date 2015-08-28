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

import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.util.Resources;
import genj.util.Trackable;
import genj.util.swing.Action2;
import genj.util.swing.MenuHelper;
import genj.util.swing.ToolbarWidget;
import genj.view.ActionProvider;
import genj.view.ContextProvider;
import genj.view.SelectionSink;
import genj.view.ToolBar;
import genj.view.View;
import genj.view.ViewContext;
import genj.view.ViewFactory;
import genj.view.ActionProvider.Purpose;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import swingx.docking.DefaultDockable;
import swingx.docking.Docked;

/**
 * A dockable for views
 */
/* package */class ViewDockable extends DefaultDockable implements WorkbenchListener {
  
  private final static Logger LOG = Logger.getLogger("genj.app");
  private final static ContextHook HOOK = new ContextHook();

  private ViewFactory factory;
  private View view;
  private Workbench workbench;
  private boolean ignoreSelectionChanged = false;
  
  private Tools toolbar = new Tools();
  
  
  /**
   * Constructor
   */
  /*package*/ ViewDockable(Workbench workbench, ViewFactory factory) {

    this.workbench = workbench;
    this.factory = factory;
    
    // title
    String title = factory.getTitle();

    // create new View
    view = factory.createView();
    
    // backlink
    view.putClientProperty(ViewDockable.class, this);

    // create the view
    setContent(view);
    setTitle(title);
    setIcon(factory.getImage());
    
    // listen to workbench
    workbench.addWorkbenchListener(this);
    
    // set context
    view.setContext(workbench.getContext(), true);
    
  }
  
  /*package*/ void dispose() {
    
    // don't listen to workbench
    workbench.removeWorkbenchListener(this);

    // clear context for cleanup
    view.setContext(new Context(), true);

  }
  
  public static ViewDockable getDockable(View view) {
    return (ViewDockable)view.getClientProperty(ViewDockable.class);
  }
  
  public Workbench getWorkbench() {
    return workbench;
  }
  
  public void close() {
    workbench.closeView(factory.getClass());
  }
  
  public View getView() {
    return (View)getContent();
  }

  public ViewFactory getViewFactory() {
    return factory;
  }
  
  @Override
  public void docked(final Docked docked) {
    super.docked(docked);
    
    // only if ToolBarSupport and no bar installed
    toolbar.beginUpdate();
    view.populate(toolbar);
    toolbar.endUpdate();

    // done
  }

  /**
   * WorkbenchListener callback - workbench signals selection change
   */
  public void selectionChanged(Workbench workbench, Context context, boolean isActionPerformed) {
    if (!ignoreSelectionChanged || isActionPerformed)
      view.setContext(context, isActionPerformed);
  }
  
  /**
   * WorkbenchListener callback - workbench signals request for commit of in-flight changes
   */
  public void commitRequested(Workbench workbench) {
    view.commit();
  }
  
  /**
   * WorkbenchListener callback - workbench signals closing 
   */
  public void workbenchClosing(Workbench workbench) {
    view.closing();
  }
  
  /**
   * Our hook into keyboard and mouse operated context changes / menu
   */
  private static class ContextHook extends Action2 implements AWTEventListener {

    /** constructor */
    private ContextHook() {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
          public Void run() {
            Toolkit.getDefaultToolkit().addAWTEventListener(ContextHook.this, AWTEvent.MOUSE_EVENT_MASK);
            return null;
          }
        });
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Cannot install ContextHook", t);
      }
    }
    
    /**
     * A Key press initiation of the context menu
     */
    public void actionPerformed(ActionEvent event) {
      // only for jcomponents with focus
      Component focus = FocusManager.getCurrentManager().getFocusOwner();
      if (!(focus instanceof JComponent))
        return;
      // look for ContextProvider and show menu if appropriate
      ViewContext context = new ContextProvider.Lookup(focus).getContext();
      if (context != null) {
        JPopupMenu popup = getContextMenu(context, Workbench.getWorkbench((Component)event.getSource()));
        if (popup != null)
          popup.show(focus, 0, 0);
      }
      // done
    }

    /**
     * A mouse click initiation of the context menu
     */
    public void eventDispatched(AWTEvent event) {

      // a mouse popup/click event?
      if (!(event instanceof MouseEvent))
        return;
      final MouseEvent me = (MouseEvent) event;
      if (!(me.isPopupTrigger() || me.getID() == MouseEvent.MOUSE_CLICKED))
        return;

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          
          // find workbench now (popup menu might go away after this method call)
          final Workbench workbench = Workbench.getWorkbench((Component)me.getSource());
          if (workbench==null)
            return;
          
          // find context at point
          final Component source = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());
          final ContextProvider.Lookup lookup = new ContextProvider.Lookup(source);
          if (lookup.getContext()==null)
            return;

          final Point point = SwingUtilities.convertPoint(me.getComponent(), me.getX(), me.getY(), source);
          
          // a double-click on provider?
          if (lookup.getProvider() == source
              && me.getButton() == MouseEvent.BUTTON1 
              && me.getID() == MouseEvent.MOUSE_CLICKED 
              && me.getClickCount() == 2) {
            SelectionSink.Dispatcher.fireSelection(me.getComponent(), lookup.getContext(), true);
            return;
          }

          // a popup?
          if (me.isPopupTrigger()&&source.isDisplayable()) {

            // cancel any menu
            MenuSelectionManager.defaultManager().clearSelectedPath();

            // show context menu
            JPopupMenu popup = getContextMenu(lookup.getContext(), workbench);
            if (popup != null)
              popup.show(source, point.x, point.y);

          }
        }
      });

      // done
    }

    /**
     * Create a popup menu for given context
     */
    private JPopupMenu getContextMenu(ViewContext context, Workbench workbench) {
      
      // make sure context is valid 
      if (context==null||workbench==null)
        return null;
      
      // make sure any existing popup is cleared
      MenuSelectionManager.defaultManager().clearSelectedPath();
      
      // create a popup
      MenuHelper mh = new MenuHelper();
      JPopupMenu popup = mh.createPopup();

      // popup local actions?
      mh.createItems(context.getActions());
      
      // get and merge all actions
      List<Action2> groups = new ArrayList<Action2>(8);
      List<Action2> singles = new ArrayList<Action2>(8);
      Map<Action2.Group,Action2.Group> lookup = new HashMap<Action2.Group,Action2.Group>();
      for (Action2 action : getProvidedActions(workbench.getProviders(ActionProvider.class), context)) {
        if (action instanceof Action2.Group) {
          Action2.Group group = lookup.get(action);
          if (group!=null) {
            group.add(new ActionProvider.SeparatorAction());
            group.addAll((Action2.Group)action);
          } else {
            lookup.put((Action2.Group)action, (Action2.Group)action);
            groups.add((Action2.Group)action);
          }
        } else {
          singles.add(action);
        }
      }
      
      // add to menu
      mh.createItems(groups);
      mh.createItems(singles);
      
      // done
      return popup;
    }
    
    private Action2.Group getProvidedActions(List<ActionProvider> providers, Context context) {
      Action2.Group group = new Action2.Group("");
      // ask the action providers
      for (ActionProvider provider : providers) 
        provider.createActions(context, Purpose.CONTEXT, group);
      // done
      return group;
    }

    
  } //ContextHook

  public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
  }

  public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
  }
  
  /**
   * Action - close view
   */
  private class ActionCloseView extends Action2 {

    /** constructor */
    protected ActionCloseView() {
      setImage(genj.view.Images.imgClose);
      setTip(Resources.get(this).getString("cc.tip.close_view", factory.getTitle()));
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      workbench.closeView(factory.getClass());
    }
  } // ActionCloseView

  public void viewClosed(Workbench workbench, View view) {
  }
  
  public void viewOpened(Workbench workbench, View view) {
  }

  public void processStarted(Workbench workbench, Trackable process) {
  }

  public void processStopped(Workbench workbench, Trackable process) {
  }

  /**
   * Toolbar proxy
   */
  private class Tools implements ToolBar {
    
    boolean isEmpty = true;
    boolean hasDefaults = false;
    
    public void add(Action action) {
      ToolbarWidget.patch(getDocked().addTool(action));
      isEmpty = false;
    }
    public void add(JComponent component) {
      if (getDocked()==null)
        return;
      getDocked().addTool(component);
      component.setFocusable(false);
      isEmpty = false;
    }
    public void addSeparator() {
      if (getDocked()==null)
        return;
      if (!isEmpty)
        getDocked().addToolSeparator();
    }
    public void beginUpdate() {
      if (getDocked()==null)
        return;
      hasDefaults = false;
      isEmpty = true;
      getDocked().clearTools();
    }
    public void endUpdate() {
      if (getDocked()==null)
        return;
      if (hasDefaults)
        return;
      hasDefaults = true;
      // our way of adding our close tool as last
      // stop toolbar if empty and less than 1024 in pixels?
      try {
        if (isEmpty && Toolkit.getDefaultToolkit().getScreenSize().height<1024)
          return;
      } catch (Throwable t) {
        // ignored
      }
      addSeparator();
      add(new ActionCloseView());
    }

  };

} //ViewDockable