import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.PropertyDate;
import genj.gedcom.PropertyEvent;
import genj.gedcom.PropertyPlace;
import genj.gedcom.PropertySex;
import genj.gedcom.PropertySource;
import genj.gedcom.TagPath;
import genj.gedcom.time.PointInTime;
import genj.report.Report;

/**

 


*/
public class ReportMissingInformation extends Report {
	 
	// check all relevant tags by default	
	public boolean checkIndividualSource = true;
	public boolean checkBirthDate = true;
	public boolean checkBirthPlace = true;
	public boolean checkBirthSource = true;
	public boolean checkBaptismDate = true;
	public boolean checkBaptismPlace = true;
	public boolean checkBaptismSource = true;
	public boolean checkDeathDate = true;
	public boolean checkDeathPlace = true;
	public boolean checkDeathSource = true;
	public boolean checkSex = true;
	public boolean checkGiven = true;
	public boolean checkSurname = true;
	public boolean checkOccupation = true;
	public boolean checkOccupationSource = true;
	public boolean formatForScreenDisplay = true;
  
	//translate strings for output  
	private String textTitle = translate("title");
	private String textSubject = translate("subject"); 
	private String textIndividual = translate("individual");
	private String textBirth = translate("birth");
	private String textBaptism = translate("baptism");
	private String textDeath = translate("death");
	private String textDate = translate("date");
	private String textPlace = translate("place");
	private String textSource = translate("source");
	private String textSex = translate("sex");
	private String textGiven = translate("given");
	private String textSurname = translate("surname");
	private String textOccupation = translate("occupation");
	private String textKey = translate("key");

	
	//column widths etc
	private int colId = 6;
	private int colName = 30;
	private int colData = 6;
	private int numDataCols = 0;

	private String formatCol(String tempString, int colWidth, int colAlign) {
		return ( formatForScreenDisplay ? align(tempString, colWidth, colAlign) : "\t" + tempString);
	}
	
  public void start(Indi indi) {
	  
   	 
	  //show column headers
	  displayHeader(indi.getName());
	  //do report
	  checkIndi(indi);
	  
  }
   
  public void checkIndi(Indi indi) {
  
		
	//vars
	PropertyDate tempDate;
	PropertyPlace tempPlace;
	PropertySource tempSource;
	String strDataRow;
	Boolean flagOk1, flagOk2, flagOk3;

	
    //clear any previous data and align
    strDataRow = indi.getId() + (formatForScreenDisplay ? "\t" : "") + formatCol(indi.getName(), colName, 3); 

    //NOTE: the order of the following tests corresponds with the display column order
    
 	//check individual source if required
	if(checkIndividualSource) {		
		// verify existence and validity of source tag
		tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:SOUR"));
		if((tempSource == null) || (!tempSource.isValid())) {
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else { 
			strDataRow = strDataRow + formatCol("ok",colData,1);	
		}
	}

 	//check birth date if required
	if(checkBirthDate) {		
		//read date of birth for validity checking
		tempDate = indi.getBirthDate();
		if((tempDate == null) || (!tempDate.isValid())) {
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else { 
			strDataRow = strDataRow + formatCol("ok",colData,1);	
		}
	}
	
	//check place of birth if required
	if(checkBirthPlace) {
		tempPlace = (PropertyPlace)indi.getProperty(new TagPath("INDI:BIRT:PLAC"));
		if((tempPlace == null)){
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else {
			strDataRow = strDataRow + formatCol("ok",colData,1);	
		}
	}
 
	//check birth source if required
	if(checkBirthSource) {		
		// verify existence of birth event
		if((PropertyEvent)indi.getProperty(new TagPath("INDI:BIRT")) != null) {		
			// verify existence and validity of source tag
			tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:BIRT:SOUR"));
			if((tempSource == null) || (!tempSource.isValid())) {
				strDataRow = strDataRow + formatCol("X",colData,1);
			} else { 
				strDataRow = strDataRow + formatCol("ok",colData,1);	
			}
		} else {
			strDataRow = strDataRow + formatCol("-",colData,1);	
		}
	}
	
	//check baptism and christening date if required
	if(checkBaptismDate) {
		//reset flags
		flagOk1 = true;
		flagOk2 = true;
		
		// bapm date...
		tempDate = (PropertyDate)indi.getProperty(new TagPath("INDI:BAPM:DATE"));		
		if((tempDate == null) || (!tempDate.isValid())) {
			flagOk1 = false;
		}
		//now do chr tag
		tempDate = (PropertyDate)indi.getProperty(new TagPath("INDI:CHR:DATE"));
		if((tempDate == null) || (!tempDate.isValid())) {
			flagOk2 =false;
		}	
	
		//if date found on either tag, flag is true
		if(flagOk1 || flagOk2) {
			strDataRow = strDataRow + formatCol("ok",colData,1);
		} else {
			strDataRow = strDataRow + formatCol("X",colData,1);
		}
	}
	
	//check baptism place if required
	if(checkBaptismPlace) {
		
		flagOk1 = true;
		flagOk2 = true;
		
		//check <bapt> 
		tempPlace = (PropertyPlace)indi.getProperty(new TagPath("INDI:BAPM:PLAC"));
		if((tempPlace == null) || (tempPlace.getValue() == "")) {
			flagOk1 = false;
		}

		//check <chr> 
		tempPlace = (PropertyPlace)indi.getProperty(new TagPath("INDI:CHR:PLAC"));
		if((tempPlace == null) || (tempPlace.getValue().length()==0)) {
			flagOk2 = false;
		}		
			
		if(flagOk1 || flagOk2) {	
			strDataRow = strDataRow + formatCol("ok",colData,1);
		} else { 
			strDataRow = strDataRow + formatCol("X",colData,1);	
		}		
	}

	//check baptism source if required
	if(checkBaptismSource) {		
		flagOk1 = true;
		flagOk2 = true;
		flagOk3 = true;
		// verify existence of baptism event
		if((PropertyEvent)indi.getProperty(new TagPath("INDI:BAPM")) == null) {		
			flagOk1 = false;
		}
		if((PropertyEvent)indi.getProperty(new TagPath("INDI:CHR")) == null) {		
			flagOk2 = false;
		}

		if((!flagOk1) && (!flagOk2)) {
			strDataRow = strDataRow + formatCol("-",colData,1);
		} else {
			if(flagOk1) {
				// verify existence and validity of source tag
				tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:BAPM:SOUR"));
				if((tempSource == null) || (!tempSource.isValid())) flagOk3 = false;
			}
			if(flagOk2) {
				// verify existence and validity of source tag
				tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:CHR:SOUR"));
				if((tempSource == null) || (!tempSource.isValid())) flagOk3 = false;
			}
			if(!flagOk3) {
				strDataRow = strDataRow + formatCol("X",colData,1);
			} else { 
				strDataRow = strDataRow + formatCol("ok",colData,1);	
			}			
		}
	}	
	
	
	//check death date if required
	if(checkDeathDate) {
		tempDate = indi.getDeathDate();
		if((indi.getDeathDate() == null) || (!tempDate.isValid())) {
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else {
		strDataRow = strDataRow + formatCol("ok",colData,1);
		}
	}
	
	//check place of death if required
	if(checkDeathPlace) {
		tempPlace = (PropertyPlace)indi.getProperty(new TagPath("INDI:DEAT:PLAC"));
		if((tempPlace == null)){
			strDataRow = strDataRow + formatCol("X",colData,1);
		}else {
			strDataRow = strDataRow + formatCol("ok",colData,1);	
		}
	}	

	//check death source if required
	if(checkDeathSource) {		
		// verify existence of death event
		if((PropertyEvent)indi.getProperty(new TagPath("INDI:DEAT")) != null) {		
			// verify existence and validity of source tag
			tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:DEAT:SOUR"));
			if((tempSource == null) || (!tempSource.isValid())) {
				strDataRow = strDataRow + formatCol("X",colData,1);
			} else { 
				strDataRow = strDataRow + formatCol("ok",colData,1);	
			}
		} else {
			strDataRow = strDataRow + formatCol("-",colData,1);	
		}
	}
	
	//check gender if required
	if(checkSex) {
		if((indi.getSex() != PropertySex.MALE) && (indi.getSex() != PropertySex.FEMALE)) { 
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else {	
			strDataRow = strDataRow + formatCol("ok",colData,1);
		}
	}
	

	//check given/firstname
	// uses extraction from <name> rather than checking <GIVN>
	if(checkGiven) {
		if(indi.getFirstName() == "") {
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else {
			strDataRow = strDataRow + formatCol("ok",colData,1);
		}
	}
	
	
	//check surname/family name
	// uses extraction from <name> rather than checking <SURN>
	if(checkSurname) {
		if(indi.getLastName() == "") {
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else {
			strDataRow = strDataRow + formatCol("ok",colData,1);
		} 
	}

	
	//check occupation if required
	if(checkOccupation) {		
		// verify existence of occupation
		if(indi.getProperty(new TagPath("INDI:OCCU")) == null) {		
			strDataRow = strDataRow + formatCol("X",colData,1);
		} else { 
			strDataRow = strDataRow + formatCol("ok",colData,1);	
		}
	}

	
	//check occupation source if required
	if(checkOccupationSource) {		
		// verify existence of occupation
		if(indi.getProperty(new TagPath("INDI:OCCU")) != null) {		
			// verify existence and validity of source tag
			tempSource = (PropertySource)indi.getProperty(new TagPath("INDI:OCCU:SOUR"));
			if((tempSource == null) || (!tempSource.isValid())) {
				strDataRow = strDataRow + formatCol("X",colData,1);
			} else { 
				strDataRow = strDataRow + formatCol("ok",colData,1);	
			}
		} else {
			strDataRow = strDataRow + formatCol("-",colData,1);	
		}
	}
	
	//display results
	println(strDataRow);
  }
 
  
  
  
  
  
  public void start(Gedcom gedcom) {
	  
	  //variables
	  Entity[] individuals;
	  int loop;
	  Indi person;

	    
	  //show report header
	  displayHeader(gedcom.getName());
	
	  //grab all
	  individuals = gedcom.getEntities(Gedcom.INDI,"");
      
	  for(loop=0; loop<individuals.length; loop++) {
        
      	//report on each
		person = (Indi)individuals[loop];      
      	checkIndi(person);
        	
      }//for loop
	 
  }
  
  

  
  public void displayHeader(String strSubject) {
  
	  String strColHeader1, strColHeader2;
	  String strUnderLine, strBlankNameHeader;
	  int loop;	  
	  
	  //calculate number of verification columns
	  numDataCols = 0;
		if(checkIndividualSource)	numDataCols += 1;
		if(checkBirthDate)	numDataCols += 1;
		if(checkBirthPlace)	numDataCols += 1;
		if(checkBirthSource)	numDataCols += 1;
		if(checkBaptismDate)	numDataCols += 1;
		if(checkBaptismPlace)	numDataCols += 1;
		if(checkBaptismSource)	numDataCols += 1;
		if(checkDeathDate)	numDataCols += 1;
		if(checkDeathPlace)	numDataCols += 1;
		if(checkDeathSource)	numDataCols += 1;
		if(checkSex)	numDataCols += 1;
		if(checkGiven)	numDataCols += 1;
		if(checkSurname)	numDataCols += 1;
		if(checkOccupation)	numDataCols += 1;
		if(checkOccupationSource)	numDataCols += 1;
	  
	  //print report title
	  println(align(textTitle, (colId + colName + numDataCols*colData), 1));
	  println();
	  
	  println(textSubject + ": " + strSubject);
	  println(textDate + ": " + PointInTime.getNow().toString());
	  println(textKey);
	  println();
	   
	  //create column header labels
	  strBlankNameHeader = " ";
	  for(loop=1; loop<colName-1; loop++)
		strBlankNameHeader += " ";
	  
	  strColHeader1 = "\t" + (formatForScreenDisplay ? strBlankNameHeader : ""); // Id Name
		if(checkIndividualSource)	strColHeader1 += formatCol(textIndividual, colData, 1);
		if(checkBirthDate)	strColHeader1 += formatCol(textBirth, colData, 1);
		if(checkBirthPlace)	strColHeader1 += formatCol(textBirth, colData, 1);
		if(checkBirthSource)	strColHeader1 += formatCol(textBirth, colData, 1);
		if(checkBaptismDate)	strColHeader1 += formatCol(textBaptism, colData, 1);
		if(checkBaptismPlace)	strColHeader1 += formatCol(textBaptism, colData, 1);
		if(checkBaptismSource)	strColHeader1 += formatCol(textBaptism, colData, 1);
		if(checkDeathDate)	strColHeader1 += formatCol(textDeath, colData, 1);
		if(checkDeathPlace)	strColHeader1 += formatCol(textDeath, colData, 1);
		if(checkDeathSource)	strColHeader1 += formatCol(textDeath, colData, 1);
		if(checkSex)	strColHeader1 += formatCol("", colData, 1);
		if(checkGiven)	strColHeader1 += formatCol("", colData, 1);
		if(checkSurname)	strColHeader1 += formatCol("", colData, 1);
		if(checkOccupation)	strColHeader1 += formatCol("", colData, 1);
		if(checkOccupationSource)	strColHeader1 += formatCol(textOccupation, colData, 1);
	  
	  strColHeader2 = "Id\t" + (formatForScreenDisplay ? strBlankNameHeader : ""); // Id Name
		if(checkIndividualSource)	strColHeader2 += formatCol(textSource, colData, 1);
		if(checkBirthDate)	strColHeader2 += formatCol(textDate, colData, 1);
		if(checkBirthPlace)	strColHeader2 += formatCol(textPlace, colData, 1);
		if(checkBirthSource)	strColHeader2 += formatCol(textSource, colData, 1);
		if(checkBaptismDate)	strColHeader2 += formatCol(textDate, colData, 1);
		if(checkBaptismPlace)	strColHeader2 += formatCol(textPlace, colData, 1);
		if(checkBaptismSource)	strColHeader2 += formatCol(textSource, colData, 1);
		if(checkDeathDate)	strColHeader2 += formatCol(textDate, colData, 1);
		if(checkDeathPlace)	strColHeader2 += formatCol(textPlace, colData, 1);
		if(checkDeathSource)	strColHeader2 += formatCol(textSource, colData, 1);
		if(checkSex)	strColHeader2 += formatCol(textSex, colData, 1);
		if(checkGiven)	strColHeader2 += formatCol(textGiven, colData, 1);
		if(checkSurname)	strColHeader2 += formatCol(textSurname, colData, 1);
		if(checkOccupation)	strColHeader2 += formatCol(textOccupation, colData, 1);
		if(checkOccupationSource)	strColHeader2 += formatCol(textSource, colData, 1);
 
	  //display
	  println(strColHeader1);
	  println(strColHeader2);

	  if (formatForScreenDisplay) {
		strUnderLine = "-";
		for(loop=1; loop<(colId+colName+numDataCols*colData)-1; loop++)
			strUnderLine += "-";
		println(strUnderLine);
	  } else {
		println();
	  }
  
  }
} 