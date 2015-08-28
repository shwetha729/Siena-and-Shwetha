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

import genj.Version;
import genj.util.EnvironmentChecker;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;
import genj.util.swing.ImageIcon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/**
 * About GenJ
 */
/*package*/ class ActionAbout extends Action2 {

  private final static Resources RES = Resources.get(ActionAbout.class);
  private final static ImageIcon IMG = new ImageIcon(ActionAbout.class,"images/About.png");
  
  /** constructor */
  protected ActionAbout() {
    setText(RES, "cc.menu.about");
    setImage(IMG);
  }

  /** run */
  public void actionPerformed(ActionEvent event) {
    DialogHelper.openDialog(RES.getString("cc.menu.about"), DialogHelper.INFORMATION_MESSAGE, new Content(), Action2.okOnly(), event);
  }
  
  private class Content extends JTabbedPane {
    
    private final static int DEFAULT_ROWS = 16, DEFAULT_COLS = 40;
    
    /**
     * Constructor
     */
    Content() {

      JLabel welcome = new JLabel(new ImageIcon(this, "/splash.png"));
      welcome.setBackground(Color.WHITE);
      welcome.setOpaque(true);
      
      addTab("GenealogyJ "+Version.getInstance().getVersionString(), null, welcome);
      addTab(RES.getString("about.authors"), null, new AuthorsPanel());
      addTab(RES.getString("about.copyright"), null, new CopyrightPanel());

    }
    
    /**
     * Helper to read text from a file
     */
    protected void readTextFile(JTextArea ta, String file) {
      try {
        FileInputStream fin = new FileInputStream(file);
        Reader in = new InputStreamReader(fin);
        ta.read(in,null);
        fin.close();
      }
      catch (Throwable t) {
      }
    }

    /**
     * Panel - Authors
     */  
    private class AuthorsPanel extends JScrollPane {

      /** 
       * Constructor
       */  
      protected AuthorsPanel() {

        // create contained text area
        JTextArea text = new JTextArea(DEFAULT_ROWS,DEFAULT_COLS);
        text.setLineWrap(false);
        text.setWrapStyleWord(true);
        text.setEditable(false);

        String dir = EnvironmentChecker.getProperty("user.dir", ".", "get authors.txt");
        
        String path = dir + File.separatorChar + "doc" + File.separatorChar + "authors.txt";
        
        readTextFile(text, path);

        // setup looks
        setViewportView(text);      
        
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // done
        
      }      
    } // AuthorsPanel

    /**
     * Panel - Copyright
     */  
    private class CopyrightPanel extends JPanel {

      /** 
       * Constructor
       */  
      protected CopyrightPanel()  {
        
        super(new BorderLayout());
        
        add(getNorth(), BorderLayout.NORTH);
        add(getCenter(), BorderLayout.CENTER);
      
      }
      
      /**
       * Create the north 
       */
      private JComponent getNorth() {
        
        JTextArea text = new JTextArea(RES.getString("app.disclaimer"),3,DEFAULT_COLS);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createTitledBorder(RES.getString("about.copyright")),
          new EmptyBorder(3, 3, 3, 3)
        ));
        panel.add(text, BorderLayout.CENTER);
        
        return panel;
      }

      /**
       * Create the center
       */
      private JComponent getCenter() {
            
        // the text    
        JTextArea text = new JTextArea(DEFAULT_ROWS,DEFAULT_COLS);
        String dir = EnvironmentChecker.getProperty("user.dir",".","read gpl.txt");
        
        String path = dir + File.separatorChar + "doc" + File.separatorChar + "gpl.txt";
        readTextFile(text, path);
        text.setLineWrap(false);
        text.setEditable(false);
        text.setBorder(new EmptyBorder(3, 3, 3, 3));
        
        // a scroller
        JScrollPane scroll = new JScrollPane(text);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createTitledBorder(RES.getString("about.license")));
        
        // done
        return scroll;
      }
    
    } // CopyrightPanel
    
    private class Log extends Action2 {
      Log() {
        setText("Log");
      }
      public void actionPerformed(ActionEvent event) {
        try {
          Desktop.getDesktop().open(App.LOGFILE);
        } catch (Throwable t) {
          Logger.getLogger("genj.io").log(Level.INFO, "can't open logfile", t);
        }
      }
    }
    
    
  } //Content
  
}