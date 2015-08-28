import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.Media;
import genj.gedcom.Note;
import genj.gedcom.Repository;
import genj.gedcom.Source;
import genj.gedcom.Submitter;
import genj.option.PropertyOption;
import genj.report.BatchCompatible;
import genj.report.BatchRunner;
import genj.report.Report;
import genj.report.ReportLoader;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.DialogHelper;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;

public class ReportBatches extends Report {

	public String configDir = "";
	public boolean generate = true;

	private final Action2[] NO_YES_ALL_NONE = { //
	new Action2(Action2.TXT_NO), //
			new Action2(Action2.TXT_YES), //
			new Action2(translate("overwrite.all")),//
			new Action2(translate("overwrite.none")),//
	};
	private static final String REPORT_CLASS_KEY = "report.class";
	private static final String ENTITY_KEY = "reports.for.entities";

	/**
	 * Lazy initialization avoids recursion (load reports while this one is
	 * loaded)
	 */
	private static Map<String, Report> reportMap;

	public void start(final Gedcom gedcom) throws Throwable {

		if (generate) {
			createConfigFiles();
			generate = false;
			return;
		}
		for (final File configFile : getFilesFromUser()) {

			final Resources config = new Resources(new FileInputStream(configFile.getPath()));
			final String configPath = configFile.getAbsolutePath();
			final Report report = getConfiguredReport(config);
			if (report == null)
				return;

			for (final String id : getDesiredContext(config)) {

				if (id.toLowerCase().equals("gedcom")) {
					runReport(report, gedcom, configPath);
				} else if (id.matches("[A-Za-z0-9]+")) {
					final Entity entity = gedcom.getEntity(id);
					if (entity != null) {
						runReport(report, entity, id + " " + configPath);
					}
				} else {
					for (final Entity entity : gedcom.getEntities()) {
						if (entity.getId().matches(id)) {
							runReport(report, entity, entity.getId() + " " + configPath);
						}
					}
				}
			}
		}
		println(translate("all.reports.finished"));
	}

	private String[] getDesiredContext(final Resources config) {

		final String string = config.getString(ENTITY_KEY);
		final String message = translate("no.entities", ENTITY_KEY);
		if (string == null) {
			println(message);
			return new String[] {};
		}
		final String[] ids = string.split(" +");
		if (ids == null || ids.length == 0) {
			println(message);
			return new String[] {};
		}
		return ids;
	}

	private Report getConfiguredReport(final Resources config) {

		final String reportClassName = config.getString(REPORT_CLASS_KEY);
		if (reportClassName == null || reportClassName.trim().length() == 0) {
			println(translate("no.report.class", REPORT_CLASS_KEY));
			return null;
		}
		final Report report = getReportMap().get(reportClassName);
		if (report == null) {
			println(translate("wrong.report.class", REPORT_CLASS_KEY, report));
			return null;
		}

		// reload the options
		for (final PropertyOption option : PropertyOption.introspect(report, true)) {
			final String key = (option.getCategory() != null ? option.getCategory() + "." : "") + option.getName();
			option.setValue(config.getString(key));
		}
		return report;
	}

	private void runReport(final Report report, final Object context, final String logMessage) {

		final String msg = logMessage + " --- " + report.getName();
		println();
		println(msg);
		flush();

		try {
			final Object result = BatchRunner.run(report, context, getOut());
			if (result == null)
				;
			else if (result instanceof InterruptedException)
				println(translate("canceled"));
			else if (result instanceof Throwable)
				whoops((Throwable) result);
			else if (result instanceof File || result instanceof URL)
				println(translate("report.result.at", result));
			else {
				println(translate("result.not.implemented", result));
				// TODO perhaps collect list to return by start
				// object = new ContextListWidget((List<Context>)result);
				// see also ReportView.showResult
			}
		} catch (final Throwable exception) {
			LOG.fine(msg + ": " + exception);
			whoops(exception);
		}
	}

	private void whoops(final Throwable exception) {

		final CharArrayWriter buf = new CharArrayWriter(256);
		((Throwable) exception).printStackTrace(new PrintWriter(buf));
		println("*** exception caught" + '\n' + buf);
	}

	private void createConfigFiles() throws FileNotFoundException {
		if (getConfigDirFromUser() == null)
			return;
		println(configDir);
		println();
		for (final String reportName : getReportMap().keySet()) {
			final Report report = getReportMap().get(reportName);
			final String fileName = configDir + File.separator + report.getClass().getName() + ".txt";
			final String format = translate("overwrite.config");
			boolean overwriteAll = false;
			if (!new File(fileName).exists() || overwriteAll)
				createConfig(fileName, report);
			else {
				final String msg = MessageFormat.format(format, fileName, report.getName());
				switch (getOptionFromUser(msg, NO_YES_ALL_NONE)) {
				case 1:
					createConfig(fileName, report);
				case 0:
					break;
				case 2:
					createConfig(fileName, report);
					overwriteAll = true;
					break;
				case 3:
					// break out of the loop
					continue;
				}
			}
		}
	}

	private void createConfig(final String fileName, final Report report) throws FileNotFoundException {

		final String className = report.getClass().getName();
		println(report.getName());
		final PrintStream out = new PrintStream(new FileOutputStream(fileName));
		out.println(REPORT_CLASS_KEY + " = " + className);
		showReportProperties(out, report.getName(), report.getCategory(), report.getAuthor(), report.getVersion(), report.getLastUpdate());
		showSupportedEntities(out, report, new Indi(), new Fam("FAM", ""), new Media("OBJE", ""), new Note("NOTE", ""), new Submitter("SUBM", ""), new Source(
				"SOUR", ""), new Repository("REPO", ""));
		showOptions(out, report);
		out.flush();
		out.close();
	}

	private static void showOptions(final PrintStream out, final Report report) {
		final List<PropertyOption> props = PropertyOption.introspect(report, true);
		String lastPrefix = "";
		for (final PropertyOption prop : props) {
			final String prefix = prop.getCategory() == null ? "" : prop.getCategory() + ".";
			if (!prefix.equals(lastPrefix)) {
				out.println();
				out.println("############ " + report.translateOption(prop.getCategory()));
				lastPrefix = prefix;
			}
			out.println();
			out.println("# " + report.translateOption(prop.getProperty()));
			out.println(prefix + prop.getProperty() + " = " + prop.getValue());
		}
	}

	private static void showReportProperties(final PrintStream out, final String... names) {
		for (final String name : names)
			if (name != null)
				out.println("# " + name);
	}

	private void showSupportedEntities(final PrintStream out, final Report report, final Entity... entities) {

		final Gedcom dummyGedcom = new Gedcom();
		out.println("#");
		for (final Entity entity : entities)
			if (report.accepts(entity) != null) {
				final String nextAvailableID = dummyGedcom.getNextAvailableID(entity.getTag());
				out.println("# " + nextAvailableID + " " + entity.getPropertyName());
			}
		out.println("");
		out.println("# " + translate(ENTITY_KEY));
		out.print(ENTITY_KEY + " = ");
		out.println("");
	}

	private int getOptionFromUser(final String msg, final Action2[] actions) {
		return DialogHelper.openDialog(getName(), DialogHelper.QUESTION_MESSAGE, msg, actions, getOwner());
	}

	private File getConfigDirFromUser() {

		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(configDir).getParentFile());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		chooser.setDialogTitle(translate("new.directory"));
		final int rc = chooser.showDialog(getOwner(), Action2.TXT_OK);

		// check result
		final File result = chooser.getSelectedFile();
		if (rc != JFileChooser.APPROVE_OPTION || result == null)
			return null;

		// keep it
		configDir = result.getAbsolutePath();
		result.mkdirs();
		return result;
	}

	private File[] getFilesFromUser() {

		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		chooser.setDialogTitle(translate("config.files"));
		chooser.setCurrentDirectory(new File(configDir));
		final int rc = chooser.showDialog(getOwner(), Action2.TXT_OK);

		// check result
		final File[] result = chooser.getSelectedFiles();
		if (rc != JFileChooser.APPROVE_OPTION || result == null)
			return new File[] {};

		// keep it
		configDir = chooser.getCurrentDirectory().getAbsolutePath();
		return result;
	}

	private Map<String, Report> getReportMap() {

		if (reportMap != null)
			return reportMap;
		reportMap = new HashMap<String, Report>();
		for (final Report report : ReportLoader.getInstance().getReports()) {
			if (report instanceof BatchCompatible) {
				reportMap.put(report.getClass().getName(), report);
			}
		}
		return reportMap;
	}
}
