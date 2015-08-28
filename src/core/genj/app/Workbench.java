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

import genj.common.ContextListWidget;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.Property;
import genj.gedcom.PropertyXRef;
import genj.gedcom.Submitter;
import genj.gedcom.UnitOfWork;
import genj.io.Filter;
import genj.io.GedcomEncodingException;
import genj.io.GedcomIOException;
import genj.io.GedcomReader;
import genj.io.GedcomReaderContext;
import genj.io.GedcomReaderFactory;
import genj.io.GedcomWriter;
import genj.util.EnvironmentChecker;
import genj.util.Origin;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.SafeProxy;
import genj.util.ServiceLookup;
import genj.util.Trackable;
import genj.util.swing.Action2;
import genj.util.swing.ChoiceWidget;
import genj.util.swing.DialogHelper;
import genj.util.swing.FileChooser;
import genj.util.swing.ImageIcon;
import genj.util.swing.MacAdapter;
import genj.util.swing.DialogHelper.ComponentVisitor;
import genj.view.SelectionSink;
import genj.view.View;
import genj.view.ViewContext;
import genj.view.ViewFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import spin.Spin;
import swingx.docking.DefaultDockable;
import swingx.docking.Dockable;
import swingx.docking.DockingPane;
import swingx.docking.dock.TabbedDock;
import swingx.docking.persistence.XMLPersister;

/**
 * The central component of the GenJ application
 */
public class Workbench extends JPanel implements SelectionSink {
  
  private final static ImageIcon
    IMG_CLOSE     = new ImageIcon(Workbench.class,"images/Close.png"),
    IMG_NEW       = new ImageIcon(Workbench.class,"images/New.png"),
    IMG_OPEN      = new ImageIcon(Workbench.class,"images/Open.png"),
    IMG_EXIT      = new ImageIcon(Workbench.class,"images/Exit.png"),
    IMG_SAVE      = new ImageIcon(Workbench.class,"images/Save.png");

  private final static String 
    ACC_SAVE = "ctrl S", 
    ACC_NEW = "ctrl N", 
    ACC_OPEN = "ctrl O",
    ACC_CLOSE = "ctrl W";

  
  /*package*/ final static Logger LOG = Logger.getLogger("genj.app");
  /*package*/ final static Resources RES = Resources.get(Workbench.class);
  /*package*/ final static Registry REGISTRY = Registry.get(Workbench.class);

  /** members */
  private List<WorkbenchListener> listeners = new CopyOnWriteArrayList<WorkbenchListener>();
  private List<Object> plugins = new ArrayList<Object>();
  private List<ViewFactory> viewFactories = ServiceLookup.lookup(ViewFactory.class);
  private Context context = new Context();
  private DockingPane dockingPane = new WorkbenchPane();
  private Menu menu = new Menu(this);
  private Toolbar toolbar = new Toolbar(this);
  private Runnable runOnExit;
  private StatusBar statusBar = new StatusBar(this);
  
  /**
   * Constructor
   */
  public Workbench(Runnable onExit) {

    // Initialize data
    runOnExit = onExit;
    
    // plugins
    LOG.info("loading plugins");
    for (PluginFactory pf : ServiceLookup.lookup(PluginFactory.class)) {
      LOG.info("Loading plugin "+pf.getClass());
      Object plugin = pf.createPlugin(this);
      plugins.add(plugin);
    }
    LOG.info("/loading plugins");

    // Layout
    setLayout(new BorderLayout());
    add(toolbar, BorderLayout.NORTH);
    add(dockingPane, BorderLayout.CENTER);
    add(statusBar, BorderLayout.SOUTH);

    // hook up close view action
    new ActionCloseView();
    
    // hook up mac actions
    if (MacAdapter.isMac()) {
      MacAdapter.getInstance().setAboutListener(new ActionAbout());
      MacAdapter.getInstance().setQuitListener(new ActionExit());
      MacAdapter.getInstance().setPreferencesListener(new ActionOptions());
    }
    
    
    // Done
  }
  
  @Override
  public void addNotify() {
    super.addNotify();
    // install ourselves into frame
    DialogHelper.visitContainers(this, new DialogHelper.ComponentVisitor() {
      public Component visit(Component parent, Component child) {
        if (parent instanceof JFrame) {
          JFrame frame = (JFrame)parent;
          frame.setJMenuBar(menu);
          frame.getRootPane().putClientProperty(Workbench.class, Workbench.this);
        }
        return null;
      }
    });
  }
  
  @Override
  public void removeNotify() {
    // de-install ourselves from frame
    DialogHelper.visitContainers(this, new DialogHelper.ComponentVisitor() {
      public Component visit(Component parent, Component child) {
        if (parent instanceof JFrame) {
          JFrame frame = (JFrame)parent;
          frame.setJMenuBar(null);
          frame.getRootPane().putClientProperty(Workbench.class, null);
        }
        return null;
      }
    });
    super.removeNotify();
  }
  
  
  /**
   * Find workbench for given component
   * @return workbench or null
   */
  /*package*/ static Workbench getWorkbench(Component component) {
    
    Component result = DialogHelper.visitOwners(component, new ComponentVisitor() {
      public Component visit(Component parent, Component child) {
        if (parent instanceof Workbench)
          return (Workbench)parent;
        if (parent instanceof View) {
          ViewDockable dockable = (ViewDockable) ((View)parent).getClientProperty(ViewDockable.class);
          return dockable.getWorkbench();
        }
        if (parent instanceof JFrame) 
          return (Workbench)((JFrame)parent).getRootPane().getClientProperty(Workbench.class);
        // continue
        return null;
      }
    });
    
    return result instanceof Workbench ? (Workbench)result : null;
  }


  
  /**
   * get current layout
   */
  public void saveLayout(Writer writer) {
    new LayoutPersister(dockingPane, writer).save();
  }
  
  /**
   * set current layout
   */
  public void loadLayout(Reader reader) {
    new LayoutPersister(dockingPane, reader).load();
  }
  
  /**
   * current context
   * @return null or context
   */
  public Context getContext() {
    return context;
  }
  
  /**
   * create a new gedcom file
   */
  public void newGedcom() {
    
    // let user choose a file
    File file = chooseFile(RES.getString("cc.create.title"), RES.getString("cc.create.action"), null);
    if (file == null)
      return;
    if (!file.getName().endsWith(".ged"))
      file = new File(file.getAbsolutePath() + ".ged");
    if (file.exists()) {
      int rc = DialogHelper.openDialog(RES.getString("cc.create.title"), DialogHelper.WARNING_MESSAGE, RES.getString("cc.open.file_exists", file.getName()), Action2.yesNo(), Workbench.this);
      if (rc != 0)
        return;
    }
    
    // close existing
    if (!closeGedcom())
      return;
    
    // form the origin
    Gedcom gedcom;
    try {
      gedcom = new Gedcom(Origin.create(new URL("file:"+file.getAbsolutePath())));
      
      // commit submitter as well
      Submitter submitter = (Submitter) gedcom.createEntity(Gedcom.SUBM);
      submitter.setName(EnvironmentChecker.getProperty("user.name", "?", "user name used as submitter in new gedcom"));
      
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "unexpected exception creating new gedcom", t);
      return;
    }
    
    // done
    setGedcom(gedcom);
  }
  
  /**
   * asks and loads gedcom file
   */
  public boolean openGedcom() {

    // ask user
    File file = chooseFile(RES.getString("cc.open.title"), RES.getString("cc.open.action"), null);
    if (file == null)
      return false;
    REGISTRY.put("last.dir", file.getParentFile().getAbsolutePath());
    
    // close what we have
    if (!closeGedcom())
      return false;
    
    // form origin
    try {
      return openGedcom(new URL("file:"+file.getAbsolutePath()));
    } catch (Throwable t) {
      // shouldn't
      return false;
    }
    // done
  }
  
  /**
   * loads gedcom file
   */
  public boolean openGedcom(URL url) {

    // close what we have
    if (!closeGedcom())
      return false;
    
    // open connection
    final Origin origin = Origin.create(url);

    // Open Connection and get input stream
    final List<ViewContext> warnings = new ArrayList<ViewContext>();
    GedcomReader reader;
    try {

      // .. prepare our reader
      reader = (GedcomReader)Spin.off(GedcomReaderFactory.createReader(origin, (GedcomReaderContext)Spin.over(new GedcomReaderContext() {
        public String getPassword() {
          return DialogHelper.openDialog(origin.getName(), DialogHelper.QUESTION_MESSAGE, RES.getString("cc.provide_password"), "", Workbench.this);
        }
        public void handleWarning(int line, String warning, Context context) {
          warnings.add(new ViewContext(RES.getString("cc.open.warning", new Object[] { new Integer(line), warning}), context));
        }
      })));

    } catch (IOException ex) {
      String txt = RES.getString("cc.open.no_connect_to", origin) + "\n[" + ex.getMessage() + "]";
      DialogHelper.openDialog(origin.getName(), DialogHelper.ERROR_MESSAGE, txt, Action2.okOnly(), Workbench.this);
      return false;
    }
    
    try {
      for (WorkbenchListener l : listeners) l.processStarted(this, reader);
      setGedcom(reader.read());
      if (!warnings.isEmpty()) {
        dockingPane.putDockable("warnings", new GedcomDockable(this,
            RES.getString("cc.open.warnings", context.getGedcom().getName()), 
            IMG_OPEN,
            new JScrollPane(new ContextListWidget(warnings)))
        );
      }
    } catch (GedcomIOException ex) {
      // tell the user about it
      DialogHelper.openDialog(origin.getName(), DialogHelper.ERROR_MESSAGE, RES.getString("cc.open.read_error", "" + ex.getLine()) + ":\n" + ex.getMessage(), Action2.okOnly(), Workbench.this);
      // abort
      return false;
    } finally {
      for (WorkbenchListener l : listeners) l.processStopped(this, reader);
    }
    
    // remember
    List<String> history = REGISTRY.get("history", new ArrayList<String>());
    history.remove(origin.toString());
    history.add(0, origin.toString());
    if (history.size()>5)
      history.remove(history.size()-1);
    REGISTRY.put("history", history);
    
    // done
    return true;
  }
  
  private void setGedcom(Gedcom gedcom) {
    
    // catch anyone trying this without close
    if (context.getGedcom()!=null)
      throw new IllegalArgumentException("context.gedcom!=null");

    // restore context
    try {
      context = Context.fromString(gedcom, REGISTRY.get(gedcom.getName()+".context", gedcom.getName()));
    } catch (GedcomException ge) {
      context = new Context(gedcom);
    }
    
    // fixup context if necessary - start with adam if necessary and available
    if (context.getEntity()==null) {
      Entity adam = null;
      if ("royal92.ged".equals(gedcom.getName()))
    	  adam = gedcom.getEntity("I65"); // Hardcoded Diana Spencer, yay
      if (adam==null)
      	adam = gedcom.getFirstEntity(Gedcom.INDI);
      context = new Context(gedcom, adam!=null ? Collections.singletonList(adam) : null, null);
    } 
    
    // tell everyone
    for (WorkbenchListener listener: listeners)
      listener.gedcomOpened(this, gedcom);
  
    fireSelection(context, true);
    
    // done
  }
  
  /**
   * save gedcom file
   */
  public boolean saveAsGedcom() {
    
    if (context.getGedcom() == null)
      return false;
    
    // ask everyone to commit their data
    fireCommit();
    
    // .. choose file
    Box options = new Box(BoxLayout.Y_AXIS);
    options.add(new JLabel(RES.getString("save.options.encoding")));
    ChoiceWidget comboEncodings = new ChoiceWidget(Gedcom.ENCODINGS, Gedcom.ANSEL);
    comboEncodings.setEditable(false);
    comboEncodings.setSelectedItem(context.getGedcom().getEncoding());
    options.add(comboEncodings);
    options.add(new JLabel(RES.getString("save.options.password")));
    File file = chooseFile(RES.getString("cc.save.title"), RES.getString("cc.save.action"), options);
    if (file == null)
      return false;
  
    // Need confirmation if File exists?
    if (file.exists()) {
      int rc = DialogHelper.openDialog(RES.getString("cc.save.title"), DialogHelper.WARNING_MESSAGE, RES.getString("cc.open.file_exists", file.getName()), Action2.yesNo(), Workbench.this);
      if (rc != 0) 
        return false;
    }
    
    // .. take chosen one & filters
    if (!file.getName().endsWith(".ged"))
      file = new File(file.getAbsolutePath() + ".ged");
    
    Gedcom gedcom = context.getGedcom();
    gedcom.setEncoding((String)comboEncodings.getSelectedItem());
    
    // .. create new origin
    try {
      gedcom.setOrigin(Origin.create(new URL("file", "", file.getAbsolutePath())));
    } catch (Throwable t) {
      LOG.log(Level.FINER, "Failed to create origin for file "+file, t);
      return false;
    }
  
    // save
    if (!saveGedcomImpl(gedcom))
    	return false;
    
    // close and reset
    if (!closeGedcom())
    	return false;

    // new set
    setGedcom(gedcom);
    
    return true;
  }
  
  /**
   * save gedcom file
   */
  public boolean saveGedcom() {

    if (context.getGedcom() == null)
      return false;
    
    // ask everyone to commit their data
    fireCommit();
    
    // do it
    return saveGedcomImpl(context.getGedcom());
    
  }
  
  /**
   * save gedcom file
   */
  private boolean saveGedcomImpl(Gedcom gedcom) {
  
//  // .. open progress dialog
//  progress = WindowManager.openNonModalDialog(null, RES.getString("cc.save.saving", file.getName()), WindowManager.INFORMATION_MESSAGE, new ProgressWidget(gedWriter, getThread()), Action2.cancelOnly(), getTarget());

    try {
      
      // prep files and writer
      GedcomWriter writer = null;
      File file = null, temp = null;
      try {
        // .. resolve to canonical file now to make sure we're writing to the
        // file being pointed to by a symbolic link
        file = gedcom.getOrigin().getFile().getCanonicalFile();

        // .. create a temporary output
        temp = File.createTempFile("genj", ".ged", file.getParentFile());

        // .. create writer
        writer = new GedcomWriter(gedcom, new FileOutputStream(temp));
      } catch (GedcomEncodingException gee) {
        DialogHelper.openDialog(gedcom.getName(), DialogHelper.ERROR_MESSAGE, RES.getString("cc.save.write_encoding_error", gee.getMessage()), Action2.okOnly(), Workbench.this);
        return false;
      } catch (IOException ex) {
        DialogHelper.openDialog(gedcom.getName(), DialogHelper.ERROR_MESSAGE, RES.getString("cc.save.open_error", gedcom.getOrigin().getFile().getAbsolutePath()), Action2.okOnly(), Workbench.this);
        return false;
      }

      // .. write it
      writer.write();
      
      // .. make backup
      if (file.exists()) {
        File bak = new File(file.getAbsolutePath() + "~");
        if (bak.exists()&&!bak.delete())
          throw new GedcomIOException("Couldn't delete backup file " + bak.getName(), -1);
        if (!file.renameTo(bak))
          throw new GedcomIOException("Couldn't create backup for " + file.getName(), -1);
      }

      // .. and now !finally! move from temp to result
      if (!temp.renameTo(file))
        throw new GedcomIOException("Couldn't move temporary " + temp.getName() + " to " + file.getName(), -1);

    } catch (GedcomIOException gioex) {
      DialogHelper.openDialog(gedcom.getName(), DialogHelper.ERROR_MESSAGE, RES.getString("cc.save.write_error", "" + gioex.getLine()) + ":\n" + gioex.getMessage(), Action2.okOnly(), Workbench.this);
      return false;
    }

//  // close progress
//  WindowManager.close(progress);
    
    // .. note changes are saved now
    if (gedcom.hasChanged())
      gedcom.doMuteUnitOfWork(new UnitOfWork() {
        public void perform(Gedcom gedcom) throws GedcomException {
          gedcom.setUnchanged();
        }
      });

    // .. done
    return true;
  }
  
  /**
   * exit workbench
   */
  public void exit() {
    
    // remember current context for exit
    if (context.getGedcom()!=null)
      REGISTRY.put("restore.url", context.getGedcom().getOrigin().toString());
    
    // close
    if (!closeGedcom())
      return;
    
    // tell about it
    for (WorkbenchListener l : listeners)
      l.workbenchClosing(this);
    
    // close all dockets
    for (Object key : dockingPane.getDockableKeys()) 
      dockingPane.removeDockable(key);
    
    // Shutdown
    runOnExit.run();
  }
  
  /**
   * closes gedcom file
   */
  public boolean closeGedcom() {

    // noop?
    if (context.getGedcom()==null)
      return true;
    
    // commit changes
    fireCommit();
    
    // changes?
    if (context.getGedcom().hasChanged()) {
      
      // close file officially
      int rc = DialogHelper.openDialog(null, DialogHelper.WARNING_MESSAGE, RES.getString("cc.savechanges?", context.getGedcom().getName()), Action2.yesNoCancel(), Workbench.this);
      // cancel - we're done
      if (rc == 2)
        return false;
      // yes - close'n save it
      if (rc == 0) 
        if (!saveGedcom())
          return false;

    }
    
    // tell 
    for (WorkbenchListener listener: listeners)
      listener.gedcomClosed(this, context.getGedcom());
    
    // remember context
    REGISTRY.put(context.getGedcom().getName(), context.toString());

    // remember and tell
    context = new Context();
    for (WorkbenchListener listener : listeners) 
      listener.selectionChanged(this, context, true);
    
    // done
    return true;
  }
  
  /**
   * Restores last loaded gedcom file
   */
  @SuppressWarnings("deprecation")
  public void restoreGedcom() {

    String restore = REGISTRY.get("restore.url", (String)null);
    try {
      // no known key means load default
      if (restore==null)
      	// we're intentionally not going through toURI.toURL here since
      	// that would add space-to-%20 conversion which kills our relative
      	// file check operations down the line
        restore = new File("gedcom/royal92.ged").toURL().toString();
      // known key needs value
      if (restore.length()>0)
        openGedcom(new URL(restore));
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "unexpected error", t);
    }
  }
  
  /**
   * Lookup providers
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getProviders(Class<T> type) {
    
    List<T> result = new ArrayList<T>();
    
    // check all dock'd components
    for (Object key : dockingPane.getDockableKeys()) {
      Dockable dockable = dockingPane.getDockable(key);
      if (dockable instanceof DefaultDockable) {
        DefaultDockable vd = (DefaultDockable)dockable;
        if (type.isAssignableFrom(vd.getContent().getClass()) && !result.contains(vd.getContent()))
          result.add((T)vd.getContent());
      }
    }
    
    // check all plugins
    for (Object plugin : plugins) {
      if (type.isAssignableFrom(plugin.getClass())&&!result.contains(plugin))
        result.add((T)plugin);
    }
    
    // check all listeners
    for (WorkbenchListener l : listeners) {
      l = SafeProxy.unwrap(l);
      if (type.isAssignableFrom(l.getClass())&&!result.contains(l))
        result.add((T)l);
    }
    
    // sort by priority
    Collections.sort(result, new Comparator<T>() {
      public int compare(T a1, T a2) {
        Priority P1 = a1.getClass().getAnnotation(Priority.class);
        Priority P2 = a2.getClass().getAnnotation(Priority.class);
        int p1 = P1!=null ? P1.priority() : Priority.NORMAL;
        int p2 = P2!=null ? P2.priority() : Priority.NORMAL;
        return p2 - p1;
      }
    });

    // harden and done
    return SafeProxy.harden(result, LOG);
  }
  
  public void fireCommit() {
    for (WorkbenchListener listener : listeners)
      listener.commitRequested(this);
  }
  
  public void fireSelection(Context context, boolean isActionPerformed) {
    
    // appropriate?
    if (context.getGedcom()!= this.context.getGedcom()) {
      LOG.log(Level.FINER, "context selection on unknown gedcom", new Throwable());
      return;
    }
    
    // following a link?
    if (isActionPerformed && context.getProperties().size()==1) {
      Property p = context.getProperty();
      if (p instanceof PropertyXRef)
        context = new Context(((PropertyXRef) p).getTarget());
    }
    
    // already known?
    if (!isActionPerformed && this.context.equals(context))
      return;
    
    LOG.finer("fireSelection("+context+","+isActionPerformed+")");
    
    // remember 
    this.context = context;
    
    if (context.getGedcom()!=null) 
      REGISTRY.put(context.getGedcom().getName()+".context", context.toString());
    
    // notify
    for (WorkbenchListener listener : listeners) 
      listener.selectionChanged(this, context, isActionPerformed);
    
  } 
  
  private void fireViewOpened(View view) {
    // tell 
    for (WorkbenchListener listener : listeners)
      listener.viewOpened(this, view);
  }

  private void fireViewClosed(View view) {
    // tell plugins
    for (WorkbenchListener listener : listeners)
      listener.viewClosed(this, view);
  }
  
  public void addWorkbenchListener(WorkbenchListener listener) {
    listeners.add(0, SafeProxy.harden(listener));
  }

  public void removeWorkbenchListener(WorkbenchListener listener) {
    listeners.remove(SafeProxy.harden(listener));
  }

  /**
   * access known view factories
   */
  public List<? extends ViewFactory> getViewFactories() {
    return viewFactories;
  }

  /**
   * access a view
   * @return view or null if not open
   */
  public View getView(Class<? extends ViewFactory> factoryClass) {
    ViewDockable dockable = (ViewDockable)dockingPane.getDockable(factoryClass);
    return dockable!=null ? dockable.getView() : null;
  }
  
  /**
   * close a view
   */
  public void closeView(Class<? extends ViewFactory> factory) {
    
    View view = getView(factory);
    if (view==null)
      return;
    
    dockingPane.putDockable(factory, null);

  }

  /*package*/ void closeDockable(Dockable dockable) {
    for (Object key : dockingPane.getDockableKeys())
      if (dockingPane.getDockable(key)==dockable)
        dockingPane.putDockable(key, null);
  }

  /**
   * (re)open a view
   */
  public View openView(Class<? extends ViewFactory> factory) {
    return openView(factory, context);
  }
  
  /**
   * (re)open a view
   */
  public View openView(Class<? extends ViewFactory> factory, Context context) {
    for (ViewFactory vf : viewFactories) {
      if (vf.getClass().equals(factory))
        return openViewImpl(vf, context);
    }
    throw new IllegalArgumentException("unknown factory");
  }
  
  private View openViewImpl(ViewFactory factory, Context context) {
    
    // already open or new
    ViewDockable dockable = (ViewDockable)dockingPane.getDockable(factory.getClass());
    if (dockable != null) {
      // bring forward
      dockingPane.putDockable(factory.getClass(), dockable);
      // done
      return dockable.getView();
    }
    
    // open it & signal current selection
    try {
      dockable = new ViewDockable(Workbench.this, factory);
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "cannot open view for "+factory.getClass().getName(), t);
      return null;
    }
    dockingPane.putDockable(factory.getClass(), dockable);

    fireViewOpened(dockable.getView());
    
    return dockable.getView();
  }

  /**
   * Let the user choose a file
   */
  private File chooseFile(String title, String action, JComponent accessory) {
    FileChooser chooser = new FileChooser(Workbench.this, title, action, "ged", EnvironmentChecker.getProperty(new String[] { "genj.gedcom.dir", "user.home" }, ".", "choose gedcom file"));
    chooser.setCurrentDirectory(new File(REGISTRY.get("last.dir", "user.home")));
    if (accessory != null)
      chooser.setAccessory(accessory);
    if (JFileChooser.APPROVE_OPTION != chooser.showDialog())
      return null;
    // check the selection
    File file = chooser.getSelectedFile();
    if (file == null)
      return null;
    // remember last directory
    REGISTRY.put("last.dir", file.getParentFile().getAbsolutePath());
    // done
    return file;
  }
  
  /**
   * Action - a workbench action
   */
  private class WorkbenchAction extends Action2 implements WorkbenchListener {
    
    public void commitRequested(Workbench workbench) {
    }

    public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
    }

    public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
    }

    public void selectionChanged(Workbench workbench, Context context, boolean isActionPerformed) {
    }
    
    public void viewClosed(Workbench workbench, View view) {
    }
    
    public void viewOpened(Workbench workbench, View view) {
    }

    public void workbenchClosing(Workbench workbench) {
    }
    
    public void processStarted(Workbench workbench, Trackable process) {
      setEnabled(false);
    }

    public void processStopped(Workbench workbench, Trackable process) {
      setEnabled(true);
    }
  }
  
  /**
   * Action - exit
   */
  /*package*/ class ActionExit extends WorkbenchAction {
    
    /** constructor */
    /*package*/ ActionExit() {
      setText(RES, "cc.menu.exit");
      setImage(IMG_EXIT);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      exit();
    }
  }
  
  /**
   * Action - close and exit
   */
  /*package*/ class ActionClose extends WorkbenchAction {
    
    /** constructor */
    /*package*/ ActionClose() {
      setText(RES, "cc.menu.close");
      setImage(IMG_CLOSE);
    }
    
    /** run */
    public void actionPerformed(ActionEvent event) {
      closeGedcom();
    }
  } // ActionExit

  /**
   * Action - new
   */
  /*package*/ class ActionNew extends WorkbenchAction {

    /** constructor */
    /*package*/ ActionNew() {
      setText(RES, "cc.menu.new");
      setTip(RES, "cc.tip.create_file");
      setImage(IMG_NEW);
      install(Workbench.this, ACC_NEW, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** execute callback */
    public void actionPerformed(ActionEvent event) {
      newGedcom();
    }

  } // ActionNew

  /**
   * Action - open
   */
  /*package*/ class ActionOpen extends WorkbenchAction {
    
    private URL url;

    /** constructor - good for button or menu item */
    /*package*/ ActionOpen() {
      setTip(RES, "cc.tip.open_file");
      setText(RES, "cc.menu.open");
      setImage(IMG_OPEN);
      install(Workbench.this, ACC_OPEN, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    
    protected ActionOpen(int m, URL url) {
      this.url = url;
      String txt = ((char)('1'+m)) + " " + url.getFile();
      
      int i = txt.indexOf('/');
      int j = txt.lastIndexOf('/');
      if (i!=j) 
        txt = txt.substring(0, i + 1) + "..." + txt.substring(j);
      setMnemonic(txt.charAt(0));
      setText(txt);
    }

    public void actionPerformed(ActionEvent event) {
      if (url!=null)
        openGedcom(url);
      else
        openGedcom();
    }
  } // ActionOpen

  /**
   * Action - Save
   */
  /*package*/ class ActionSave extends WorkbenchAction {
    /** whether to ask user */
    private boolean saveAs;
    /** gedcom */
    protected Gedcom gedcomBeingSaved;
    /** writer */
    private GedcomWriter gedWriter;
    /** origin to load after successfull save */
    private Origin newOrigin;
    /** filters we're using */
    private Filter[] filters;
    /** progress key */
    private String progress;
    /** exception we might encounter */
    private GedcomIOException ioex = null;
    /** temporary and target file */
    private File temp, file;
    /** password used */
    private String password;

    /**
     * Constructor for saving gedcom 
     */
    /*package*/ ActionSave(boolean saveAs) {
      // remember
      this.saveAs = saveAs;
      // text
      if (saveAs)
        setText(RES.getString("cc.menu.saveas"));
      else {
        setText(RES.getString("cc.menu.save"));
        
        install(Workbench.this, ACC_SAVE, JComponent.WHEN_IN_FOCUSED_WINDOW);
      }
      setTip(RES, "cc.tip.save_file");
      // setup
      setImage(IMG_SAVE);
      setEnabled(context.getGedcom()!=null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (saveAs)
        saveAsGedcom();
      else
        saveGedcom();
    }

  } // ActionSave

  /**
   * Action - Close View (for keyboard binding only)
   *
   */
  private class ActionCloseView extends Action2 {
    public ActionCloseView() {
      install(Workbench.this, ACC_CLOSE, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
      DialogHelper.visitContainers(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), new DialogHelper.ComponentVisitor() {
        public Component visit(Component parent, Component child) {
          // check any tabbed pane we can find for possibly contained views
          if (child instanceof TabbedDock) {
            ((ViewDockable)((TabbedDock)child).getSelectedDockable()).close();
            return child;
          }
          // encountered view?
          if (child instanceof View) {
            ViewDockable.getDockable((View)child).close();
            return child;
          }
          return null;
        }
      });

    }
  }
  
  /**
   * Action - Open View
   */
  /*package*/ class ActionOpenView extends Action2 {
    /** which ViewFactory */
    private ViewFactory factory;

    /** constructor */
    /*package*/ ActionOpenView(ViewFactory vw) {
      factory = vw;
      setText(factory.getTitle());
      setTip(RES.getString("cc.tip.open_view", factory.getTitle()));
      setImage(factory.getImage());
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      openViewImpl(factory, context);
    }
  } // ActionOpenView

  /**
   * layout persist/restore
   */
  private class LayoutPersister extends XMLPersister {
    
    LayoutPersister(DockingPane dockingPane, Reader layout) {
      super(dockingPane, layout, "1");
    }
    
    LayoutPersister(DockingPane dockingPane, Writer layout) {
      super(dockingPane, layout, "1");
    }
    
    @Override
    protected Object parseKey(String key) throws SAXParseException {
      // a view dockable key?
      try {
        return Class.forName(key);
      } catch (ClassNotFoundException e) {
      }
      // a string key then
      return key;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected String formatKey(Object key) throws SAXException {
      if (key instanceof String)
        return (String)key;
      return ((Class<? extends ViewFactory>)key).getName();
    }
    
    @Override
    public void load() {
      try {
        super.load();
      } catch (Exception ex) {
        LOG.log(Level.WARNING, "unable to load layout", ex);
      }
    }
  
    @Override
    public void save() {
      try {
        super.save();
      } catch (Exception ex) {
        LOG.log(Level.WARNING, "unable to save layout", ex);
      }
    }
  }
  
  private class WorkbenchPane extends DockingPane implements WorkbenchListener {
    
    private List<JDialog> dialogs = new ArrayList<JDialog>();
    
    public WorkbenchPane() {
      addWorkbenchListener(this);
    }
    
    private void updateTitle(JDialog dlg, String title) {
      dlg.setTitle(title);
    }
    
    private void updateTitles(String title) {
      for (JDialog dlg : dialogs) 
        updateTitle(dlg, title);
    }
    
    @Override
    protected JDialog createDialog() {
      JDialog dialog = super.createDialog();
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      if (context.getGedcom()!=null)
        updateTitle(dialog, context.getGedcom()!=null ? context.getGedcom().getName() : "");
      dialogs.add(dialog);
      return dialog;
    }
    
    @Override
    protected void dismissDialog(JDialog dialog) {
      super.dismissDialog(dialog);
      dialogs.remove(dialog);
    }

    public void commitRequested(Workbench workbench) {
    }
    
    @Override
    protected Dockable createDockable(Object key) {
      for (ViewFactory vf : viewFactories) {
        if (vf.getClass().equals(key)) {
          ViewDockable vd = new ViewDockable(Workbench.this, vf);
          return vd;
        }
      }
      LOG.finer("can't find view factory for docking key"+key);
      return null;
    }
    
    @Override
    protected void dismissDockable(Dockable dockable) {
      if (dockable instanceof ViewDockable) {
        ViewDockable vd = (ViewDockable)dockable;
        vd.dispose();
        fireViewClosed(vd.getView());
      }
    }

    public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
      updateTitles("");
    }

    public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
      updateTitles(gedcom.getName());
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

} // ControlCenter
