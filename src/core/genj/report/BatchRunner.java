package genj.report;

import java.io.PrintWriter;

public class BatchRunner {
	
	public static Object run(Report report, Object context, PrintWriter printWriter) throws Throwable{
		report.setOut(printWriter);
		return report.start(context);
	}
}
