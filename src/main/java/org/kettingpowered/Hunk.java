package org.kettingpowered;

import java.util.ArrayList;
import java.util.List;

public class Hunk {
    String origLineNumber;
    int origLines;
    String modifiedLineNumber;
    int modifiedLines;
    List<String> contents = new ArrayList<>();
    
    public Hunk(String hunkHeader){
        int firstComma = hunkHeader.indexOf(',');
        this.origLineNumber = hunkHeader.substring(3, firstComma);
        
        String post_first_line_number = hunkHeader.substring(firstComma+1);
        int space = post_first_line_number.indexOf(' ');
        String origLines = hunkHeader.substring(firstComma+1, firstComma+1+space);
        this.origLines = Integer.parseInt(origLines);
        
        String second_line_number = post_first_line_number.substring(space+1);
        int secondComma = second_line_number.indexOf(',');
        int end_space = second_line_number.indexOf(' ');
        this.modifiedLineNumber = second_line_number.substring(0,secondComma);
        String modifiedLinesStr = second_line_number.substring(secondComma+1, end_space);
        this.modifiedLines = Integer.parseInt(modifiedLinesStr);
    }
    
    public void add_content_line(String str) {
        contents.add(str);
    }
    
    public void decrement_original_line(){
        origLines--;
    }        
    public void decrement_modified_line(){
        modifiedLines--;
    }    
    public void increment_modified_line(){
        modifiedLines--;
    }
    
    public void recalc_linesChanged(){
        origLines = (int) contents.parallelStream().filter(line -> line.charAt(0) == ' ' || line.charAt(0) == '-').count();
        modifiedLines = (int) contents.parallelStream().filter(line -> line.charAt(0) == ' ' || line.charAt(0) == '+').count();
    }
    
    @Override
    public String toString(){
        return String.format("@@ %s,%d %s,%d @@%n%s%n", origLineNumber, origLines, modifiedLineNumber, modifiedLines, String.join("\n", contents));
    }
}
