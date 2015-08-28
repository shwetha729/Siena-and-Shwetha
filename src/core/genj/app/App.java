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
package genj.app;

import genj.Version;
import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.option.OptionProvider;
import genj.util.EnvironmentChecker;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;
import genj.util.swing.MacAdapter;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main Class for GenJ Application
 */
public class App {
  
  /*package*/ static Logger LOG;
  /*package*/ static File LOGFILE; 
  private final static Resources RESOURCES = Resources.get(App.class);
  private final static Registry REGISTRY = Registry.get(App.class);
  
  private static Workbench workbench;
  private static JFrame frame;
  
  /**
   * GenJ Main Method
   */
  public static synchronized void main(final String[] args) {
    
    // already up?
    if (workbench!=null)  
      return;
    
    try {
      
      // prepare our master log and own LogManager for GenJ
      LOG = Logger.getLogger("genj");
      
      // allow command line override of debug level - set non-genj level a tad higher
      Logger root = Logger.getLogger("");
      try {
        Level level = Level.parse(EnvironmentChecker.getProperty("genj.debug.level", "INFO", "log-level for GenJ"));
        LOG.setLevel(level);
        if (Integer.MAX_VALUE!=level.intValue())
          root.setLevel(new Level("genj.debug.level+1", level.intValue()+1) {} );
      } catch (Throwable t) {
      }

      // prepare console logging
      Handler[] handlers = root.getHandlers();
      for (int i=0;i<handlers.length;i++) root.removeHandler(handlers[i]);
      BufferedHandler bufferedLogHandler = new BufferedHandler();
      root.addHandler(bufferedLogHandler);
      root.addHandler(new FlushingHandler(new StreamHandler(System.out, new LogFormatter())));
      System.setOut(new PrintStream(new LogOutputStream(Level.INFO, "System", "out")));
      System.setErr(new PrintStream(new LogOutputStream(Level.WARNING, "System", "err")));
  
      LOG.info("Main");
      
      // prepare our registry
      REGISTRY.setFile(new File(EnvironmentChecker.getProperty("user.home.genj", ".", "calculate dir for registry"), "genj.properties"));
  
      // initialize options first
      OptionProvider.getAllOptions();
      
      // Setup File Logging and check environment
      LOGFILE = EnvironmentChecker.getLog();
      Handler handler = new FileHandler(LOGFILE.getAbsolutePath(), Options.getInstance().getMaxLogSizeKB()*1024, 1, true);
      handler.setLevel(Level.ALL);
      handler.setFormatter(new LogFormatter());
      LOG.addHandler(handler);
      root.removeHandler(bufferedLogHandler);
      bufferedLogHandler.flush(handler);
    
      // Startup Information
      LOG.info("version = "+Version.getInstance().getBuildString());
      LOG.info("date = "+new Date());
      EnvironmentChecker.log();
      
      // patch up GenJ for Mac if applicable
      if (MacAdapter.isMac()) {
        MacAdapter.getInstance().install("GenealogyJ");
        LOG.info("MacAdapter active");
      }
      
      // check VM version
      if (!EnvironmentChecker.isJava16()) {
        if (EnvironmentChecker.getProperty("genj.forcevm", null, "Check force of VM")==null) {
          LOG.severe("Need Java 1.6 to run GenJ");
          System.exit(1);
          return;
        }
      }

      LOG.info("/Main");
      
      // start awt Startup
      SwingUtilities.invokeAndWait(new Startup(args));
      
    } catch (Throwable t) {
      throw new Error(t);
    }
    
  }
  
  /**
   * Our startup code
   */
  private static class Startup implements Runnable {
    
    private String[] args;

    Startup(String[] args) {
      this.args = args;
    }
    
    public void run() {
      
        // Log is up
        LOG.info("Startup");
        
        // patch JPopupMenu consuming events on close
        UIManager.put("PopupMenu.consumeEventOnClose", new Boolean(false));
        
        // setup control center
        workbench = new Workbench(new Shutdown());
  
        // show it
        frame = new JFrame() {
          @Override
          public void dispose() {
            REGISTRY.put("frame", this);
            super.dispose();
          }
        };
        frame.setTitle(RESOURCES.getString("app.title"));
        frame.setIconImage(Gedcom.getImage().getImage());
        frame.getContentPane().add(workbench);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            workbench.exit();
          }
        });
        REGISTRY.get("frame", frame);
        frame.setVisible(true);
        
        // Disclaimer - check version and registry value
        String version = Version.getInstance().getVersionString();
        if (!version.equals(REGISTRY.get("disclaimer",""))) {
          // keep it      
          REGISTRY.put("disclaimer", version);
          // show disclaimer
          DialogHelper.openDialog("Disclaimer", DialogHelper.INFORMATION_MESSAGE, RESOURCES.getString("app.disclaimer"), Action2.okOnly(), frame);    
        }

        // connect
        workbench.addWorkbenchListener(new WorkbenchAdapter() {
          @Override
          public void selectionChanged(Workbench workbench, Context context, boolean isActionPerformed) {
            if (context.getEntities().size()==1)
              frame.setTitle(context.getGedcom().getName()+" - "+context.getEntity()+" - "+RESOURCES.getString("app.title"));
            else if (context.getGedcom()!=null)
              frame.setTitle(context.getGedcom().getName()+" - "+RESOURCES.getString("app.title"));
            else
              frame.setTitle(RESOURCES.getString("app.title"));
          }
          @Override
        	public void gedcomClosed(Workbench workbench, Gedcom gedcom) {
            frame.setTitle(RESOURCES.getString("app.title"));
        	}
        	@Override
        	public void gedcomOpened(Workbench workbench, Gedcom gedcom) {
            frame.setTitle(gedcom.getName()+" - "+RESOURCES.getString("app.title"));
        	}
        });

        // load
        if (args.length==0)
          workbench.restoreGedcom();
        else try {
          workbench.openGedcom(new URL("file:"+new File(args[0]).getAbsolutePath()));
        } catch (MalformedURLException e) {
        }
        
        // done
        LOG.info("/Startup");
      
    }
    
  } //Startup

  /**
   * Our shutdown code
   */
  private static class Shutdown implements Runnable {
    
    /**
     * do the shutdown
     */
    public void run() {
      LOG.info("Shutdown");
      // close window
      frame.dispose();
	  // persist options
	  OptionProvider.persistAll();
	  // Store registry 
	  Registry.persist();      
	  // done
      LOG.info("/Shutdown");
      // let VM do it's thing
      System.exit(0);
      // done
    }
    
  } //Shutdown
  
  /**
   * a log handler that buffers 
   */
  private static class BufferedHandler extends Handler {
    
    private List<LogRecord> buffer = new ArrayList<LogRecord>();

    @Override
    public void close() throws SecurityException {
      // noop
    }

    @Override
    public void flush() {
      
    }
    
    private void flush(Handler other) {
      for (LogRecord record : buffer)
        other.publish(record);
      buffer.clear();
    }

    @Override
    public void publish(LogRecord record) {
      buffer.add(record);
    }
    
  }

  /**
   * a log handler that flushes on publish
   */
  private static class FlushingHandler extends Handler {
    private Handler wrapped;
    private FlushingHandler(Handler wrapped) {
      this.wrapped = wrapped;
      wrapped.setLevel(Level.ALL);
      setLevel(Level.ALL);
    }
    public void publish(LogRecord record) {
      wrapped.publish(record);
      flush();
    }
    public void flush() {
      wrapped.flush();
    }
    public void close() throws SecurityException {
      flush();
      wrapped.close();
    }
  }
  
  /**
   * Our own log format
   */
  private static class LogFormatter extends Formatter {
    public String format(LogRecord record) {
      StringBuffer result = new StringBuffer(80);
      result.append(record.getLevel());
      result.append(":");
      result.append(record.getSourceClassName());
      result.append(".");
      result.append(record.getSourceMethodName());
      result.append(":");
      String msg = record.getMessage().replace('\n', ',');
      Object[] parms = record.getParameters();
      if (parms==null||parms.length==0)
        result.append(msg);
      else 
        result.append(MessageFormat.format(msg, parms));
      result.append(System.getProperty("line.separator"));

      if (record.getThrown()!= null) {
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            record.getThrown().printStackTrace(pw);
        } catch (Throwable t) {
        }
        pw.close();
        result.append(sw.toString());
      }      
      
      return result.toString();
    }
  }
  
  /**
   * Our STDOUT/ STDERR log outputstream
   */
  private static class LogOutputStream extends OutputStream {
    
    private char[] buffer = new char[256];
    private int size = 0;
    private Level level;
    private String sourceClass, sourceMethod;
    
    /**
     * Constructor
     */
    public LogOutputStream(Level level, String sourceClass, String sourceMethod) {
      this.level = level;
      this.sourceClass = sourceClass;
      this.sourceMethod = sourceMethod;
    }
    
    /**
     * collect up to limit characters 
     */
    public void write(int b) throws IOException {
      if (b!='\n') {
       buffer[size++] = (char)b;
       if (size<buffer.length) 
         return;
      }
      flush();
    }

    /**
     * 
     */
    public void flush() throws IOException {
      if (size>0) {
        LOG.logp(level, sourceClass, sourceMethod, String.valueOf(buffer, 0, size).trim());
        size = 0;
      }
    }
  }
    
} //App
