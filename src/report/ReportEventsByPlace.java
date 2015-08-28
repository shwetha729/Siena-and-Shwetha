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
import genj.gedcom.Note;
import genj.gedcom.Property;
import genj.gedcom.PropertyDate;
import genj.gedcom.PropertyEvent;
import genj.gedcom.PropertyMultilineValue;
import genj.gedcom.PropertyPlace;
import genj.report.Report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportEventsByPlace extends Report {

	public boolean sortEventTypes = true;
	public boolean showNotes = false;
	public boolean showSources = false;
	public boolean showEventNotes = true;
	public boolean showEventSources = true;
	public boolean showLiving = true;
	public boolean showDeceased = true;
	public String showEvents = "BIRT,BAPM,BAPL,CHR,CHRA,MARR,DIV,EMIG,IMMI,NATU,DEAT";

	public void start(final Gedcom gedcom) throws IOException {

		if (!showLiving && !showDeceased) {
			println(translate("neither_living_nor_deceased"));
			return;
		}
		final Map<String, List<Event>> eventsByPlace = collectEvents(gedcom);
		if (eventsByPlace.isEmpty()) {
			println(translate("no_events"));
		}
		for (final String placeName : eventsByPlace.keySet()) {
			println(placeName);
			for (final Event event : sort(eventsByPlace, placeName)) {
				final Entity entity = event.getEntity();
				final String date = event.getDate().getDisplayValue();
				println("\t" + event.getPropertyName() + " " + date + " : " + entity);
				printProperties("\t\t", event.getProperties("NOTE"), showEventNotes);
				printProperties("\t\t", event.getProperties("SOUR"), showEventSources);
				printProperties("\t\t", entity.getProperties("NOTE"), showNotes);
				printProperties("\t\t", entity.getProperties("SOUR"), showSources);
			}
		}
	}

	private Map<String, List<Event>> collectEvents(final Gedcom gedcom) {

		final String[] tags = showEvents.split(",");
		final Map<String, List<Event>> eventsByPlace = new TreeMap<String, List<Event>>();
		for (final Entity entity : collectEntitties(gedcom)) {
			for (final String tag : tags) {
				for (final Property eventProperty : entity.getProperties(tag.trim(), true)) {
					if (eventProperty instanceof PropertyEvent) {
						final Event event = new Event((PropertyEvent) eventProperty);
						if (event.getDate() != null) {
							for (final String placeName : event.getPlaceNames()) {
								if (!eventsByPlace.containsKey(placeName)) {
									eventsByPlace.put(placeName, new ArrayList<Event>());
								}
								eventsByPlace.get(placeName).add(event);
							}
						}
					}
				}
			}
		}
		return eventsByPlace;
	}

	private List<Entity> collectEntitties(final Gedcom gedcom) {

		final List<Entity> entities = new ArrayList<Entity>();
		for (final Indi indi : gedcom.getIndis()) {
			if (showDeceased && indi.isDeceased()) {
				entities.add(indi);
			}
			if (showLiving && !indi.isDeceased()) {
				entities.add(indi);
			}
		}
		for (final Fam fam : gedcom.getFamilies()) {
			if (showDeceased && showLiving) {
				entities.add(fam);
			} else {
				for (final Indi indi : fam.getSpouses()) {
					if (!showLiving && !indi.isDeceased()) {
						continue;
					}
					if (!showDeceased && indi.isDeceased()) {
						continue;
					}
					entities.add(fam);
				}
			}
		}
		return entities;
	}

	private Event[] sort(final Map<String, List<Event>> eventsByPlace, final String placeName) {

		final List<Event> eventList = eventsByPlace.get(placeName);
		Event[] events = {};
		events = eventList.toArray(events);
		Arrays.sort(events);
		return events;
	}

	private void printProperties(final String indent, final Property[] properties, final boolean show) {

		if (properties == null || properties.length == 0 || !show)
			return;
		for (final Property property:properties) {
			if (property instanceof Note) {
				final PropertyMultilineValue delegate = ((Note)property).getDelegate();
				for (final String line:delegate.getLines()){
					println(indent +  line);
				}
			} else {
				println(indent +  property.toString());
			}
		}
	}

	/**
	 * Wrapper for {@link PropertyEvent}, enables sort.
	 * 
	 */
	private class Event implements Comparable<Event> {

		private final PropertyEvent event;

		public Event(final PropertyEvent property) {
			event = property;
		}

		public Property[] getProperties(final String tag) {

			return event.getProperties(tag, true);
		}

		public int compareTo(final Event arg) {
			final int result = event.getTag().compareToIgnoreCase(arg.getTag());
			if (result == 0 || !sortEventTypes)
				return event.getDate().compareTo(arg.getDate());
			return result;
		}

		private String getTag() {
			return event.getTag();
		}

		public String getPropertyName() {
			return event.getPropertyName();
		}

		public PropertyDate getDate() {
			return event.getDate();
		}

		public Entity getEntity() {
			return event.getEntity();
		}

		public List<String> getPlaceNames() {
			final List<String> names = new ArrayList<String>();
			for (final Property placeProperty : event.getProperties("PLAC", true)) {
				if ((placeProperty instanceof PropertyPlace)) {
					names.add(placeProperty.getValue());
				}
			}
			return names;
		}
	}
}
