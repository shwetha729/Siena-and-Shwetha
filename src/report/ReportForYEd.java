/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.PropertyEvent;
import genj.gedcom.PropertySex;
import genj.gedcom.time.Delta;
import genj.gedcom.time.PointInTime;
import genj.report.BatchCompatible;
import genj.report.Options;
import genj.report.Report;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

public class ReportForYEd extends Report implements BatchCompatible{

	public class Events {

		public boolean showDates = true;
		public boolean showPlaces = true;
		public String place_display_format = "";

		private String format(final String symbol, final PropertyEvent event) {

			if (event == null || !(showDates || showPlaces))
				return "";
			final Property date = event.getDate(true);
			final Property place = event.getProperty("PLAC");
			if (date == null && place == null)
				return "";
			final String string = (date == null || !showDates ? "" : date
					.getDisplayValue())
					+ " "
					+ (place == null || !showPlaces ? "" : place.format(
							place_display_format).replaceAll("^(,|(, ))*", "")
							.trim());
			if (string.trim().equals(""))
				return "";
			return symbol + " " + string;
		}
	}

	public class Gender {
		public String unknown = "";
		public String male = "\u2642";
		public String female = "\u2640";
		private final String[] snippets = new String[3];

		public Gender() {
			snippets[PropertySex.UNKNOWN] = unknown;
			snippets[PropertySex.MALE] = male;
			snippets[PropertySex.FEMALE] = female;
		}

		private String format(final Indi indi) {
			return snippets[indi.getSex()];
		}
	}

	public class Images {

		public int maxWidth = 80;
		public int maxHeight = 80;
		// with single quotes, the place holders don't function
		public String format = "<img src=\"file://{0}\" width=\"{1}\" height=\"{2}\">";

		private String format(final Entity entity) {

			if (format == null || format.equals(""))
				return null;

			final String fileName = getFileName(entity);
			if (fileName == null)
				return null;

			final Dimension dim = getDimension(getImage(fileName), new Dimension(maxWidth, maxHeight));
			final String extension = fileName.toLowerCase().replaceAll(".*\\.", "");
			if (imageExtensions.contains(extension))
				return MessageFormat.format(format, fileName, dim.getWidth(), dim.getHeight());
			return null;
		}
	}

	public class Filter {

		public String tag = "_YED";
		public String content = "";
		public boolean active = true;
		public boolean descendants = true;
		public boolean ancestors = true;
	}

	public class Links {

		public String indi = translate("indiUrlDefault");
		public String family = translate("familyUrlDefault");

		private String format(final String id, final String urlFormat) {

			if (urlFormat == null)
				return "";
			final String link = MessageFormat.format(urlFormat, id);
			return MessageFormat.format(XML_LINK_CONTAINER, link);
		}
	}

	public boolean showOccupation = true;
	public String imageExtensions = "jpg jpeg gif png";
	public Images indiImages = new Images();
	public Images famImages = new Images();
	public Events events = new Events();
	public Links links = new Links();
	public Filter filter = new Filter();
	public Gender gender = new Gender();

	private final String XML_LINK_CONTAINER = getString("LinkContainer");
	private final String XML_POPUP_CONTAINER = getString("PopUpContainer");
	private final String XML_FAMILY = getString("FamilyNode");
	private final String XML_INDI = getString("IndiNode");
	private final String XML_EDGE = getString("Edge");
	private final String XML_HEAD = getString("XmlHead");
	private final String XML_TAIL = getString("XmlTail");

	private static final String INDI_COLORS[] = createIndiColors();
	private static final Options OPTIONS = Options.getInstance();

	private int edgeCount = 0;
	private File reportFile;

	/** main */
	public void start(final Gedcom gedcom) throws IOException {

		if (!filter.active) {
			generateReport(gedcom.getFamilies(), gedcom.getIndis());
			return;
		}
		final Collection<Indi> indis = new HashSet<Indi>();
		final Collection<Fam> fams = new HashSet<Fam>();
		
		for (final Fam fam : gedcom.getFamilies()) {
			final String value = fam.getPropertyValue(filter.tag);
			if (value != null && value.contains(filter.content)) {
				fams.add(fam);
				indis.add(fam.getHusband());
				indis.add(fam.getWife());
				if (filter.descendants) {
					collectDecendants(indis, fams, fam.getHusband());
					collectDecendants(indis, fams, fam.getWife());
				}
				if (filter.ancestors) {
					collectAncestors(indis, fams, fam.getHusband());
					collectAncestors(indis, fams, fam.getWife());
				}
			}
		}
		
		for (final Indi indi : gedcom.getIndis()) {
			final String value = indi.getPropertyValue(filter.tag);
			if (value != null && value.contains(filter.content)) {
				indis.add(indi);
				for (final Fam fam : indi.getFamiliesWhereSpouse()) {
					fams.add(fam);
				}
				if (filter.descendants) {
					collectDecendants(indis, fams, indi);
				}
				if (filter.descendants) {
					collectAncestors(indis, fams, indi);
				}
			}
		}
		indis.remove(null);
		generateReport(fams, indis);
	}

	/** main */
	public void start(final Indi indi) throws IOException {

		final Collection<Indi> indis = new HashSet<Indi>();
		final Collection<Fam> fams = new HashSet<Fam>();
		buildCollections(indi, indis, fams);
		generateReport(fams, indis);
	}

	/** main */
	public void start(final Fam fam) throws IOException {

		final Collection<Indi> indis = new HashSet<Indi>();
		final Collection<Fam> fams = new HashSet<Fam>();
		buildCollections(fam.getHusband(), indis, fams);
		buildCollections(fam.getWife(), indis, fams);
		generateReport(fams, indis);
	}

	private void buildCollections(final Indi indi,
			final Collection<Indi> indis, final Collection<Fam> fams)
			throws FileNotFoundException, IOException {
		collectAncestors(indis, fams, indi);
		collectDecendants(indis, fams, indi);
	}

	private void collectAncestors(final Collection<Indi> indis,
			final Collection<Fam> fams, final Indi indi) {

		if (indi == null)
			return;
		indis.add(indi);
		for (final Fam fam : indi.getFamiliesWhereChild()) {
			fams.add(fam);
			collectAncestors(indis, fams, fam.getHusband());
			collectAncestors(indis, fams, fam.getWife());
		}
	}

	/** also sons-in-law and doughters-in-law are also added to the collection */
	private void collectDecendants(final Collection<Indi> indis,
			final Collection<Fam> fams, final Indi indi) {

		if (indi == null)
			return;
		indis.add(indi); // (un)married children
		for (final Fam fam : indi.getFamiliesWhereSpouse()) {
			// married children and their spouses
			indis.add(fam.getHusband());
			indis.add(fam.getWife());
			fams.add(fam);
			for (final Indi child : fam.getChildren()) {
				collectDecendants(indis, fams, child);
			}
		}
	}

	private static final PointInTime pit = new PointInTime(1, 1, 2200);

	/** Start after collecting the entities for the report */
	private void generateReport(final Collection<Fam> families,
			final Collection<Indi> indis) throws FileNotFoundException,
			IOException {
		println(MessageFormat.format("{0} persons {1} families",
				indis.size(), families.size()) );

		final List<Indi> sortedIndis = sortByAge(indis);

		final Writer out = createWriter();
		if (out == null)
			return;
		println("creating: " + reportFile.getAbsoluteFile());

		out.write(XML_HEAD + "\n");
		for (final Indi indi : sortedIndis) {
			out.write(createNode(indi) + "\n");
		}
		for (final Fam fam : families) {
			out.write(createNode(fam) + "\n");
		}
		for (final Indi indi : sortedIndis) {
			out.write(createIndiToFam(indi, families) + "\n");
			out.write(createFamToIndi(indi, families) + "\n");
		}
		out.write(XML_TAIL + "\n");

		out.flush();
		out.close();
		println("ready");
	}

	private List<Indi> sortByAge(final Collection<Indi> indis) {
		// hoping this could influence yEd's layout, but it seems not
		final List<Indi> sortedIndis = new ArrayList<Indi>(indis);
		Collections.sort(sortedIndis, new Comparator<Indi>() {

			@Override
			public int compare(final Indi i1, final Indi i2) {

				if (i1 == null && i2==null)
					return 0;
				if (i1 == null)
					return 1;
				if (i2 == null)
					return -1;
				
				final Delta p1 = i1.getAge(pit);

				final Delta p2 = i2.getAge(pit);
				// null?
				if (p1 == p2)
					return 0;
				if (p1 == null)
					return 1;
				if (p2 == null)
					return -1;

				// let p's compare themselves
				return -p1.compareTo(p2);
			}
		});
		return sortedIndis;
	}

	private static String[] createIndiColors() {

		final String[] result = new String[3];
		result[PropertySex.MALE] = "#CCCCFF";
		result[PropertySex.FEMALE] = "#FF99CC";
		result[PropertySex.UNKNOWN] = "#CCCCCC";
		return result;
	}

	private String createIndiToFam(final Indi indi,
			final Collection<Fam> families) {

		if (indi==null) return "";
		String s = "";
		for (final Fam fam : indi.getFamiliesWhereSpouse()) {
			if (families.contains(fam))
				s += MessageFormat.format(XML_EDGE, edgeCount++, indi.getId(),
						fam.getId());
		}
		return s;
	}

	private String createFamToIndi(final Indi indi,
			final Collection<Fam> families) {

		if (indi==null) return "";
		String s = "";
		for (final Fam fam : indi.getFamiliesWhereChild()) {
			if (families.contains(fam))
				s += MessageFormat.format(XML_EDGE, edgeCount++, fam.getId(),
						indi.getId());
		}
		return s;
	}

	private String createNode(final Fam family) {

		if (family==null) return "";
		final String id = family.getId();
		final String label = createLabel(family);
		final String height = label.contains("<html>") ? "42.0" : "27.0";
		return MessageFormat.format(XML_FAMILY, id, escape(label), links
				.format(id, links.family), createPopUpContainer(label), height);
	}

	private String createNode(final Indi indi) {
		if (indi==null) return "";
		final String id = indi.getId();
		final String label = createLabel(indi);
		return MessageFormat.format(XML_INDI, id, escape(label), links.format(
				id, links.indi), INDI_COLORS[indi.getSex()],
				createPopUpContainer(label));
	}

	private String createLabel(final Fam family) {

		if (family==null) return "";
		final String image = famImages.format(family);
		final String mariage = events.format(OPTIONS.getMarriageSymbol(),
				(PropertyEvent) family.getProperty("MARR"));
		final String divorce = events.format(OPTIONS.getDivorceSymbol(),
				(PropertyEvent) family.getProperty("DIV"));

		if (mariage.equals("") && divorce.equals("") && image == null)
			return "   ";
		final String format;
		if (image != null) {
			format = "<html><table><tr><td><p>{0}<br>{1}</p></td><td>{2}</td></tr></table></body></html>";
		} else if (divorce.equals("") || mariage.equals("")) {
			format = "{0}{1}";
		} else {
			format = "<html><body>{0}<br>{1}</body></html>";
		}
		return wrap(format, mariage, divorce, image);
	}

	private String createLabel(final Indi indi) {

		if (indi==null) return "";
		final String image = indiImages.format(indi);
		final String sex = gender.format(indi);
		final String name = indi.getPropertyDisplayValue("NAME");
		final String occu = indi.getPropertyDisplayValue("OCCU");

		String birth = events.format(OPTIONS.getBirthSymbol(),
				(PropertyEvent) indi.getProperty("BIRT"));
		if (birth.equals(""))
			birth = events.format(OPTIONS.getBaptismSymbol(),
					(PropertyEvent) indi.getProperty("BAPM"));
		String death = events.format(OPTIONS.getDeathSymbol(),
				(PropertyEvent) indi.getProperty("DEAT"));
		if (death.equals(""))
			death = events.format(OPTIONS.getBurialSymbol(),
					(PropertyEvent) indi.getProperty("BURI"));

		final String format;
		if (image != null) {
			format = "<html><table><tr><td><p>{5} {0}<br>{1}<br>{2}<br>{3}</p></td><td>{4}</td></tr></table></body></html>";
		} else if (showOccupation && occu != null && !occu.trim().equals("")) {
			format = "<html><body><p>{5} {0}<br>{1}<br>{2}<br>{3}</p></body></html>";
		} else if (birth.equals("") && death.equals("")) {
			format = "{5} {0}";
		} else if (birth.equals("") ) {
			format = "<html><body><p>{5} {0}<br>{2}</p></body></html>";
		} else if (death.equals("") ) {
			format = "<html><body><p>{5} {0}<br>{1}</p></body></html>";
		} else {
			format = "<html><body><p>{5} {0}<br>{1}<br>{2}</p></body></html>";
		}
		return wrap(format, name, birth, death, occu, image, sex);
	}

	private String wrap(final String format, final Object... args) {

		return MessageFormat.format(format, args).replaceAll("'", "\"");
	}

	private String escape(final String content) {

		return content.replaceAll(">", "&gt;").replaceAll("<", "&lt;");
	}

	/**
	 * @param content
	 *            text only, no HTML
	 */
	private String createPopUpContainer(final String content) {

		if (content == null)
			return "";
		return MessageFormat.format(XML_POPUP_CONTAINER, content);
	}

	private String getString(final String key) {

		return getResources().getString(key);
	}

	private Writer createWriter() throws FileNotFoundException {

		final String extension = "graphml";
		reportFile = getFileFromUser(translate("name"), translate("save"),
				true, extension);
		if (reportFile == null)
			return null;
		if (!reportFile.getName().toLowerCase().endsWith("." + extension)) {
			reportFile = new File(reportFile.getPath() + "." + extension);
		}
		final FileOutputStream fileOutputStream = new FileOutputStream(
				reportFile);
		final OutputStreamWriter streamWriter = new OutputStreamWriter(
				fileOutputStream, Charset.forName("UTF8"));
		return new BufferedWriter(streamWriter);
	}
	
	private String getFileName(final Entity entity) {
		final Property property = entity.getPropertyByPath(entity.getPath() + ":OBJE:FILE");
		if (property == null)
			return null;
		return property.getGedcom().getOrigin().getFile(property.getValue()).getAbsoluteFile().getPath();
	}

	private Dimension getDimension(final BufferedImage image, final Dimension max) {
		final Dimension actual = new Dimension(image.getWidth(), image.getHeight());
		double width = image.getWidth();
		double height = image.getHeight();
		if (width > max.getWidth()) {
			float delta = ((float) max.getWidth()) / image.getWidth();
			width = max.getWidth();
			height *= delta;
		} else 
		if (height > max.getHeight()) {
			float delta = ((float) max.getHeight()) / image.getHeight();
			height = max.getHeight();
			width *= delta;
		}
		return new Dimension((int) width, (int) height);
	}
	
	private BufferedImage getImage(final String value) {
		final File file = new File(value);
		try {
			return ImageIO.read(file);
		} catch (final IOException e) {
			println("oops can't load image: "+file.getAbsolutePath());
		}
		return null;
	}
}
