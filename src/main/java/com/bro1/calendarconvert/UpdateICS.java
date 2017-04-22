package com.bro1.calendarconvert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.CompatibilityHints;


/**
 * When a outlook export calendar contains a recurring event that has been modified it will contain 2 entries for the day with the same UID,
 * Google calendar does not know how to import this properly and usually only imports the details that have been modified but not the rest.
 * 
 * This program fixes it by copying relevant properties to the specific instance from the recurring item and removes the recurring item from 
 * the calendar.
 */
public class UpdateICS {

	public static void main(String[] args) throws Exception {

		UpdateICS updateICS = new UpdateICS();
		if (args.length < 1) {
			System.out.println("Please provide the name of the file to be reworked");
			return;
		}
		updateICS.update(args[0]);
		
	}
	
	
	private void update(String fileName) throws Exception {

		
		Map<String, VEvent> m = new HashMap<>();
		
		// Iterate through XML files in a specified directory
		File inputFile = new File(fileName);		
		FileInputStream fin = new FileInputStream(inputFile);

		CalendarBuilder builder = new CalendarBuilder();

		Calendar calendar = builder.build(fin);
		
		// Construct a map of calendar entries
		
		ComponentList calendarComponentList = calendar.getComponents();
		ListIterator iterator = calendarComponentList.listIterator();
		while (iterator.hasNext()) {
						
			Object calEntryObject = iterator.next();
			
			if (calEntryObject instanceof VEvent) {				
				VEvent event = (VEvent) calEntryObject;
				Property propertyUID = event.getProperty("UID");
				Property propertySummary = event.getProperty("SUMMARY");								
				m.put(propertyUID.getValue() + (propertySummary != null ? "SUMMARY": "NOSUMMARY"), event);				
			}
		}
		
		
		for(String key : m.keySet()) {
			VEvent event = m.get(key);
							
			if (key.endsWith("NOSUMMARY")) {			
				// copy some properties from the summary one and remove the summary item from the calendar
				
				Property puid = event.getProperty("UID");
				VEvent recurev = m.get(puid.getValue()+"SUMMARY");

				
				System.out.println("Copying summary");
				Property psum = recurev.getProperty("SUMMARY");
				Property psumc = psum.copy();
				event.getProperties().add(psumc);
				
				
				if (event.getProperty("DESCRIPTION") == null) {
					Property pdesc = recurev.getProperty("DESCRIPTION");
					if (pdesc != null) {
						System.out.println("Copying description");
						Property pdescc = pdesc.copy();
						event.getProperties().add(pdescc);
					}
				}
				
				
				if (event.getProperty("LOCATION") == null) {				
					Property ploc = recurev.getProperty("LOCATION");
					if (ploc != null) {
						System.out.println("Copying location");
						Property plocc = ploc.copy();
						event.getProperties().add(plocc);
					}
				}
				
				
				
				calendarComponentList.remove(recurev);
			}
			
			
		}
				
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_OUTLOOK_COMPATIBILITY, true);
		
		
		FileOutputStream fout;
		if (inputFile.getParent() != null) {
			fout = new FileOutputStream(inputFile.getParent() + File.separator +  "fixed-"  + inputFile.getName());
		} else {
			fout = new FileOutputStream("fixed-"  + inputFile.getName());
		}

		CalendarOutputter outputter = new CalendarOutputter();
		outputter.output(calendar, fout);			
		
	}	

}
