/*
  This file is part of KernelGenius.
  
  Authors: Monty Zukoski, Thierry Lepley
*/

/* Management of preprocessor directives in the compiler. */

package parser;
import java.util.*;

public class PreprocessorInfoChannel
{
  Map<Integer,List<Object>> lineMap = new HashMap<Integer,List<Object>>(); // indexed by Token number
  int firstValidTokenNumber = 0;
  int maxTokenNumber = 0;

  public void addLineForTokenNumber( Object line, Integer toknum) {
      if ( lineMap.containsKey( toknum ) ) {
	List<Object> lines = lineMap.get( toknum );
	lines.add(line);
      }
      else {
	List<Object> lines = new ArrayList<Object>();
	lines.add(line);
	lineMap.put(toknum, lines);
	if ( maxTokenNumber < toknum.intValue() ) {
	  maxTokenNumber = toknum.intValue();
	}
      }
    }

  public int getMaxTokenNumber() {
    return maxTokenNumber;
  }
  
  public List<Object> extractLinesPrecedingTokenNumber( Integer toknum ) {
    List<Object> lines = new ArrayList<Object>();
    if (toknum == null) return lines;
    //	System.out.println("token = " + toknum.toString());
    for (int i = firstValidTokenNumber; i < toknum.intValue(); i++){
      List<Object> tokenLineVector = lineMap.remove(i);
      if (tokenLineVector != null) {
	lines.addAll(tokenLineVector);
      }
    }
    firstValidTokenNumber = toknum;
    return lines;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("PreprocessorInfoChannel:\n");
    for (int i = 0; i <= maxTokenNumber + 1; i++){
      Integer inti = new Integer(i);
      if ( lineMap.containsKey( inti ) ) {
	List<Object> tokenLineVector = lineMap.get( inti );
	if ( tokenLineVector != null) {
	  for(Object o:tokenLineVector) {
	    sb.append(inti + ":" + o + '\n');
	  }
	}
      }
    }
    return sb.toString();
  }
}



