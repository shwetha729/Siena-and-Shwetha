/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.report.Report;
import genj.view.ViewContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Compute the common ancestor of two individuals
 *
 */
public class ReportCommonAncestor extends Report {

  /**
   * special treatmen context argument check
   */
  public String accepts(Object context) {
    // an indi is fine
    if (context instanceof Indi)
      return getName();
    // an array of indis is fine as well
    if (context instanceof Indi[]) {
      Indi[] indis = (Indi[])context;
      if (indis.length==2)
        return getName();
    }
    // no go
    return null;
  }

  /**
   * our main method for an argument individual
   */
  public List<ViewContext> start(Indi indi) {
    // ask for other
    Indi other = (Indi)getEntityFromUser(translate("select"), indi.getGedcom(), Gedcom.INDI);
    if (other==null)
      return null;
    // continue
    return start(new Indi[] { indi, other});
  }

  /**
   * our main method for an argument of a bunch of individuals
   */
  public List<ViewContext> start(Indi[] indis) {

    // first and second
    Indi indi = indis[0];
    Indi other = indis[1];

    // Recurse into indi
    Indi ancestor = getCommonAncestor(indi, other);

    // nothing to show?
    if (ancestor==null) {
      getOptionFromUser(translate("nocommon"), Report.OPTION_OK);
      return null;
    }

    // prepare the result
    List<ViewContext> result = new ArrayList<ViewContext>();
    result.add(new ViewContext(translate("result.first", indi), new Context(indi)));
    result.add(new ViewContext(translate("result.second", other), new Context(other)));
    result.add(new ViewContext(translate("result.ancestor", ancestor), new Context(ancestor)));
    return result;
  }

  private Indi getCommonAncestor(Indi indi, Indi other) {
    // check father and mother of indi
    Indi father = indi.getBiologicalFather();
    if (father!=null) {
      if (father.isAncestorOf(other))
        return father;
      Indi ancestor = getCommonAncestor(father, other);
      if (ancestor!=null)
        return ancestor;
    }
    Indi mother = indi.getBiologicalMother();
    if (mother!=null) {
      if (mother.isAncestorOf(other))
        return mother;
      Indi ancestor = getCommonAncestor(mother, other);
      if (ancestor!=null)
        return ancestor;
    }
    // none found
    return null;
  }

}
