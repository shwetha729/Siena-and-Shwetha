/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package validate;

import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Note;
import genj.gedcom.Property;
import genj.gedcom.Source;
import genj.gedcom.TagPath;
import genj.view.ViewContext;

import java.util.List;

/**
 * Test for property validity
 * @author nmeier
 */
/*package*/ class TestLeftover extends Test {

  /** the report */
  private ReportValidate report;
  
  /**
   * Constructor
   */
  /*package*/ TestLeftover(ReportValidate report) {
    super((String[])null, Entity.class);
    this.report = report;
  }
  
  /**
   * @see validate.Test#test(genj.gedcom.Property, genj.gedcom.TagPath, java.util.List)
   */
  /*package*/ void test(Property prop, TagPath path, List<ViewContext> issues, ReportValidate report) {

    if (prop instanceof Source || prop instanceof Note || prop instanceof Fam) {
      if (!((Entity)prop).isConnected())
        issues.add(new ViewContext(prop).setText(report.translate("warn.disconnected", prop.getPropertyName())));
    }
      
  }

} //TestValid