package validate;

import genj.gedcom.Fam;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.view.ViewContext;

import java.util.List;

public class TestIncestus extends Test {

	public TestIncestus() {
		super((String[])null, Fam.class);
	}
	
	@Override
	void test(Property prop, TagPath path, List<ViewContext> issues, ReportValidate report) {

		Fam fam = (Fam)prop;
		Indi husband = fam.getHusband();
		Indi wife = fam.getWife();
		
		if (husband==null||wife==null)
			return;
		
		if (husband.getParents().contains(wife)||wife.getParents().contains(husband)||husband.isSiblingOf(wife))
			issues.add(new ViewContext(fam).setText(report.translate("warn.incestus", husband.toString(), wife.toString())));

	}

}
