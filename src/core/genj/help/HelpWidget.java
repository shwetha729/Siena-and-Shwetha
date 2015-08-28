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
package genj.help;

import genj.io.CachingStreamHandler;
import genj.util.Resources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;

/**
 * A bridge to javax Help System
 */
class HelpWidget extends JPanel {

  private final static Logger LOG = Logger.getLogger("genj.help");
  private final static Resources RESOURCES = Resources.get(HelpWidget.class);
  private final static CachingStreamHandler CACHE = new CachingStreamHandler("help");
  private final static String BASE_URL = "http://genj.sourceforge.net/wiki/%s/manual/";
  
  final static String WELCOME = "welcome";
  final static String MANUAL = "overview";
  final static String EXPORT = "?do=export_xhtmlbody";

  private String base;
  private JEditorPane content;
  private String page = null;

  /**
   * Constructor
   */
  public HelpWidget() {
    
    // setup
    HTMLDocument doc = new HTMLDocument();
    doc.setAsynchronousLoadPriority(1);
    
    content = new JEditorPane();
    content.setBackground(Color.WHITE);
    content.setEditable(false);
    content.setEditorKit(new Kit());
    content.setDocument(doc);
    content.addHyperlinkListener(new Hyperlinker());
    
    // layout
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, new JScrollPane(content));
    
    // calculate base
    String lang = Locale.getDefault().getLanguage();
    if (!("en".equals(lang)||"de".equals(lang)||"fr".equals(lang)))
      lang = "en";
    base = String.format(BASE_URL, lang);

    // done
  }
  
  String getPage() {
    return page;
  }
  
  void setPage(String page) {
    try {
      String old = this.page;
      this.page = page;
      content.setPage(new URL(null, base+page+EXPORT, CACHE));
      firePropertyChange("url", old, page);
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "can't set help content", t);
    }
  }
  
  /**
   * @see javax.swing.JComponent#getPreferredSize()
   */
  public Dimension getPreferredSize() {
    return new Dimension(480,480);
  }
  
  /**
   * our editor kit w/custom factory
   */
  private static class Kit extends HTMLEditorKit {
    
    private static Factory factory = new Factory();
    
    @Override
    public ViewFactory getViewFactory() {
      return factory;
    }
  
    private static class Factory extends HTMLFactory {
      @Override
      public View create(Element elem) {
        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
          // patch img border=0
          if (o==HTML.Tag.IMG) {
            MutableAttributeSet atts = (MutableAttributeSet)elem.getAttributes();
            atts.addAttribute(HTML.Attribute.BORDER, "0");
            atts.addAttribute(HTML.Attribute.ALIGN, "middle");
            ImageView img = new ImageView(elem);
            return img;
          }
        }
        // fallback
        return super.create(elem);
      }
    }
  }

  /**
   * link mgmt
   */
  private class Hyperlinker implements HyperlinkListener {
    public void hyperlinkUpdate(HyperlinkEvent e) {
      
      // clicks only
      if (e.getEventType()!=EventType.ACTIVATED)
        return;
      
      String s = e.getDescription();
      
      // a genj action?
      if (s.startsWith("genj:")) {
        LOG.info("Click on "+s);
        return;
      }
      
      // an internal link?
      URL url = e.getURL();
      s = url.toString();
      if (s.startsWith(base)) {
        // don't go where there's a ? already
        if (s.indexOf('?')>0)
          return;
        setPage(s.substring(base.length()));
        return;
      }      
      
      // try an external link
      if (url!=null) try {
        Desktop.getDesktop().browse(url.toURI());
      } catch (Throwable t) {
        LOG.info("can't open external url "+s);
      }
     
      // done
    }
  }
    
} //HelpWidget
