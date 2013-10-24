/*
  This file is part of KernelGenius.

  Copyright (C) 2013 STMicroelectronics

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, write to the Free
  Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
  Boston, MA 02110-1301 USA.
  
  Authors: Thierry Lepley
*/

/* Filter programmable algorithmic node */

package ir.algorithms;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import codegen.OpenCL.CLGenVarNames;

import parser.TNode;
import common.CompilerError;
import driver.options.CodegenOptions;
import ir.base.DataEdge;
import ir.base.FunctionNode;
import ir.base.BorderMode;
import ir.base.KernelData;
import ir.literals.Literal;
import ir.literals.c.IntegerLiteral;
import ir.literals.c.ArrayLiteral;
import ir.types.c.Array;
import ir.types.kg.MatrixSize;
import ir.types.kg.MatrixIndexes;
import ir.types.kg.KernelDataCoordinate;
import ir.types.kg.TypedMatrixIndexes;


public class Filter extends FunctionNode {
  
  // Index semantics 
  enum IndexType {NO, C, MATH};
  IndexType indexing=IndexType.NO;
  
  // Function to apply to the input
  String function=null;  // Function raw string
  TNode functionNode=null; // AST node for the function
  List<Object> functionList=new LinkedList<Object>(); // Processed function

  // Temporary build information
  Map<KernelData,MatrixIndexes> readBoundProperty=new  HashMap<KernelData,MatrixIndexes>();
  Map<KernelData,MatrixIndexes> readBoundFromFunctionProperty=new  HashMap<KernelData,MatrixIndexes>();
  MatrixIndexes writeBoundProperty=null;
  MatrixIndexes writeBoundFromFunctionProperty=null;
  
  //==================================================================
  // Setters specific to Filter
  //==================================================================


  public void setIndexing(IndexType it, TNode tn, CompilerError ce) {
    if (indexing!=IndexType.NO) {
      ce.raiseWarning(tn,"redefining the indexing property");
    }
    indexing=it;
  }
 
 
  public void setProperty(String prop, List<KernelData> paramList, TNode tn, CompilerError ce) {
    raiseUnknownIdentifierPropertyError(prop,tn,ce);
  }

  public void setPropertyWithIdentifier(String prop, List<KernelData> paramList, String ident, TNode tn, CompilerError ce) {
    if (prop.equals("border")) {
      if (paramList.size()==0) {
        // Applies to all inputs
        setPropertyWithIdentifier(prop,getInputDataList(),ident,tn,ce);
      }
      else {
        try {
          BorderMode bm=new BorderMode(ident);
          for(KernelData kd:paramList) {       
            setBorderProperty(kd,bm,tn,ce);
          } 
        } catch (Exception e) {
          raiseUnknownPropertyValueError(prop,ident,tn,ce);
        }
      }
    }
    else if (prop.equals("indexing")) {
      if (ident.equals("c")) {
        setIndexing(IndexType.C,tn,ce);
      }
      else if (ident.equals("math")) {
        setIndexing(IndexType.MATH,tn,ce);
      }
      else {
        raiseUnknownPropertyValueError(prop,ident,tn,ce);
      }

    }
    else {
      raiseUnknownIdentifierPropertyError(prop,tn,ce);
    }
  }
  
  public void setPropertyWithLiteral(String prop, List<KernelData> paramList, Literal l, TNode tn, CompilerError ce) {
    if (prop.equals("border")) {
      if (paramList.size()==0) {
        // Applies to all inputs
        setPropertyWithLiteral(prop,getInputDataList(),l,tn,ce);
      }
      else {
        try {
          BorderMode bm=new BorderMode(l,tn);
          for(KernelData kd:paramList) {       
            setBorderProperty(kd,bm,tn,ce);
          } 
        } catch (Exception e) {
          raisePropertyError(e.getMessage(),prop,tn,ce);
        }
      }
      // TODO: should check the compatibility with the related input
    }
    else if (prop.equals("stride_in") || prop.equals("stride_out")) {
      if (paramList.size()!=0) {
        raiseParamPropertyError(prop,tn,ce);
      }
       if (!(l instanceof ArrayLiteral)) {
        ce.raiseError(tn,"The stride property must be an array");
        return;
      }
      ArrayLiteral al=(ArrayLiteral)l;

      List<Integer> indexList=new LinkedList<Integer>();
      for(int i=0;i<al.getSize();i++) {
        Literal il=al.getAtIndex(i);
        if (!(il instanceof IntegerLiteral)) {
          ce.raiseError(tn,"The stride property must be an array of integers");
          return;
        }
        int value=(int)((IntegerLiteral)il).getValue();
        if (value<=0) {
          ce.raiseError(tn,Integer.toString(value)+" is not a correct stride value");
        }
        indexList.add(value);
      }
      
      if (prop.equals("stride_in")) {
        setInputStride(new MatrixSize(indexList),tn,ce);
      }
      else {
        setOutputStride(new MatrixSize(indexList),tn,ce);
      }
    }
    else {
      raiseUnknownLiteralPropertyError(prop,tn,ce);
    }
  }
  
  public void setPropertyWithArrayRange(String prop, List<KernelData> paramList, Array array, TNode tn, CompilerError ce) {
    if (prop.equals("read_bound")) {
      if (paramList.size()==0) {
        // Applies to all inputs
        setPropertyWithArrayRange(prop,getInputDataList(),array,tn,ce);
      }
      else {
        for(KernelData kd:paramList) {
          // Property for a particular input
          if (readBoundProperty.containsKey(kd)) {    
            ce.raiseWarning(tn,"redefining the '"+prop+"' property for input '"+kd.getName()+"'");
          }
          readBoundProperty.put(kd,new MatrixIndexes(array));
        }
      }
    }
    else if (prop.equals("write_bound")) {
      if (paramList.size()==0) {
        // Applies to the output
        writeBoundProperty=new MatrixIndexes(array);
      }
      else {
        ce.raiseError(tn,"Property'"+prop+"' only applies to output");
      }
    }
   
    
    else {
      raiseUnknownArrayRangePropertyError(prop,tn,ce);
    }
  }

  protected void setFunction(String f, TNode tn) {
    // Should check the coherence of the function or better parse it
    function=f;
    functionNode=tn;
  }
 
  public void setPropertyWithString(String prop, List<KernelData> paramList, String s, TNode tn, CompilerError ce) {
    if (prop.equals("function")) {
      setFunction(s,tn);
    }
    else if (prop.equals("border")) {
      if (paramList.size()==0) {
        // Applies to all inputs
        setPropertyWithString(prop,getInputDataList(),s,tn,ce);
      }
      else {
        try {
          BorderMode bm=new BorderMode(s,tn,ce);
          for(KernelData kd:paramList) {       
            setBorderProperty(kd,bm,tn,ce);
          } 
        } catch (Exception e) {
          raisePropertyError(e.getMessage(),prop,tn,ce);
        }
      }     
    }
    else {
      raiseUnknownStringPropertyError(prop,tn,ce);
    }
  }

  
  //==================================================================
  // Check
  //==================================================================

  protected boolean completeAndCheckNode(CompilerError ce) {
    boolean error=false;

    if (function==null) {
      ce.raiseError(getNameNode(), "missing function definition for Filter '"+getName()+"'");
      // No need to continue
      return true;
    }

    if (indexing==IndexType.NO) {
      ce.raiseWarning(1,getNameNode(),getName()+": taking the C indexing semantics as default (last dimension first :t[z][y][x])");
      indexing=IndexType.C;
    }

    //================================================================================
    // Process the function, determine iterative inputs and the read and write pattern
    // for each iterative input
    if (processFunction(function,functionList,functionNode,ce)) {
      // The definition may be incomplete, can not continue
      return true;
    }
    //================================================================================
 
    // If there is no iterative input, it may be a mistake
    if (getNbInputEdges()<1) {
      ce.raiseWarning(1,getNameNode(),getName()+": no iterative input");
    }
    
    
    // Set Write patterns   
    
    {
      // Get the two possible write pattern definitions
      MatrixIndexes pFunc=writeBoundFromFunctionProperty;
      MatrixIndexes pBound=writeBoundProperty;

      // Set the edge with the correct information and check for consistent declarations
      if (pFunc.hasStaticSize()) {
        setWritePattern(pFunc);
        if (pBound!=null) {
          ce.raiseWarning(getNameNode(),getName()+": the write access pattern of parameter '"+
              getName()+"' is fully defined from .function, the .write_bound("+getName()+") property is then ignored");
          // TODO: remove the bound property information
        }
      }
      else {
        // The write access pattern must be defined as a bound property
        if (pBound==null) {
          ce.raiseError(getNameNode(),getName()+": the write access pattern of parameter '"+
              getName()+"' can not be computed from .function, the .write_bound("+getName()+") property is then required");
        }
        else {
          setWritePattern(pBound);
        }
      }
    }

    
    // Set Read patterns
    for(DataEdge de:getInputEdgeList()) {     
      KernelData kd=de.getSourceData();
      
      // Get the two possible read pattern definitions
      MatrixIndexes pFunc=readBoundFromFunctionProperty.get(kd);
      MatrixIndexes pBound=readBoundProperty.get(kd);

      // Set the edge with the correct information and check for consistent declarations
      if (pFunc.hasStaticSize()) {
        de.setReadPattern(pFunc);
        if (pBound!=null) {
          ce.raiseWarning(getNameNode(),getName()+": the read access pattern of parameter '"+
              kd.getName()+"' is fully defined from .function, the .read_bound("+kd.getName()+") property is then ignored");
          // TODO: remove the bound property information
        }
      }
      else {
        // The read access pattern must be defined as a bound property
        if (pBound==null) {
          ce.raiseError(getNameNode(),getName()+": the read access pattern of parameter '"+
              kd.getName()+"' can not be computed from .function, the .read_bound("+kd.getName()+") property is then required");
          error=true;
        }
        else {
          de.setReadPattern(pBound);      
        }
      }
    }

    // Check that read bound properties are not set on non iterative inputs
    for(KernelData kd:readBoundProperty.keySet()) {
      if (!kd.isIterative()) {
        ce.raiseError(getNameNode(),getName()+
            ": read bound property specified on a non iterative input '"+kd.getName()+"'");
        error=true;
      }
    }

    // Check that all iterative inputs have 
    //  1- the same dimensions since they share the same stride
    //  2- the same read pattern
    DataEdge de0=getFirstInputEdge();
    MatrixIndexes m=de0.getSourceData().getMatrixType();
    MatrixIndexes rp=de0.getReadPattern();
    for(DataEdge de:getInputEdgeList()) {
      if (!de.getSourceData().getMatrixType().isSame(m)) {
        ce.raiseError(getNameNode(),"node "+getName()+"iterative inputs '"+
            de0.getSourceData().getName()+
            "' and '"+de.getSourceData().getName()+
            "' have different size");
        error=true;
      }
      if (!de.getReadPattern().isSame(rp)) {
        ce.raiseError(getNameNode(),"node "+getName()+"iterative inputs '"+
            de0.getSourceData().getName()+
            "' and '"+de.getSourceData().getName()+
            "' have different access pattern");
        error=true;
      }    
    }


    // Input stride property
    if (getInputStridePattern()==null) {
      ce.raiseWarning(1,getNameNode(),getName()+": taking default {1} input stride");
      setInputStrideNoCheck(MatrixSize.MatrixSingleElement);
    }
    
    // Output stride property
    if (getOutputStridePattern()==null) {
      ce.raiseWarning(1,getNameNode(),getName()+": taking default {1} output stride");
      setOutputStrideNoCheck(MatrixSize.MatrixSingleElement);
    }
    
    // For border management, we need position information in the compute
    // function
    setNeedPosition();
    
    return error;
  }
 
  // Process the string function and compute the input read pattern
  int relativeLine;
  void updateRelativeLine(String s) {
    int index=0;
    int i;
    while ((i=s.indexOf("\n",index))>=0) {
      index=i+1;
      relativeLine++;
    }
  }
  void resetRelativeLine() {
    relativeLine=0;
  }
  int getRelativeLine() {
    return relativeLine;
  }
  
  
  //------------------------------------------------------------------------
  // Process the native function provided by the user and analyze array
  // accesses to extract read/write access patterns
  //------------------------------------------------------------------------
  protected boolean processFunction(final String s, List<Object> fList, 
                                    TNode fNode, CompilerError ce) {
    
    // Note: The Integer is null in case the index is not fixed
    LinkedList<Integer> outputMin=new LinkedList<Integer>();
    LinkedList<Integer> outputMax=new LinkedList<Integer>();
    Map<KernelData, LinkedList<Integer>> inputMin=new HashMap<KernelData, LinkedList<Integer>>();
    Map<KernelData, LinkedList<Integer>> inputMax=new HashMap<KernelData, LinkedList<Integer>>();

    LinkedList<Object> coordNormal=new LinkedList<Object>();
    LinkedList<Object> coord=new LinkedList<Object>();
    LinkedList<KernelDataCoordinate> outputAccessList=new LinkedList<KernelDataCoordinate>();
        
    Set<KernelData> iterationSet=new HashSet<KernelData>();
    
    boolean error=false;
 
    int i,i1,i2;
    int j=0;

    resetRelativeLine();

    // Get the first token
    i1=s.indexOf("$"); i2=s.indexOf("@");
    if (i1<0) {i=i2;} else { if (i2<0) {i=i1;} else { if (i1<i2) {i=i1;} else {i=i2;} } }
    
    while (i>=0) {
      // Get the string before
      String subS=s.substring(j, i);
      fList.add(subS);
      updateRelativeLine(subS);
      
      // Read or write ?
      boolean isReadMarker=s.charAt(i) == '$' ;

      // Take the end of the input reference
      j = getEndOfReference(s,i+1,fNode,ce);
      final String variableAccessReferenceFull = s.substring(i + 1, j);
      final String variableAccessReference = variableAccessReferenceFull.trim();

      // Get and check the accessed variable name
      LinkedList<Integer> min = null;
      LinkedList<Integer> max = null;     
      int a = variableAccessReference.indexOf("[");
      String name;
      if (a<0) {
        name=variableAccessReference;
      }
      else {
        name=variableAccessReference.substring(0, a).trim();
      }
      KernelData kdReference = getInputData(name);
      if (kdReference == null) {
        // It may be the output
        if (name.equals(getName())) {
          if (isReadMarker) {
            ce.raiseError(fNode, getRelativeLine(), "read of output '"+name+"' is forbidden");  
            error=true;
         }
          kdReference=this;
        }
        else {
          // Error, unknown variable
          ce.raiseError(fNode, getRelativeLine(), "unknown reference '"+name+"'");
          error=true;
        }

        min = outputMin;
        max = outputMax;
      }
      else {
        // It is an input (that becomes an iterative input)
        
        // Inputs can only be read
        if (!isReadMarker) {
          ce.raiseError(fNode, getRelativeLine(), "write of node input '"+name+"' is forbidden");  
          error=true;
        }

        if (!iterationSet.contains(kdReference)) {
          iterationSet.add(kdReference);          
          inputMin.put(kdReference, new LinkedList<Integer>());
          inputMax.put(kdReference, new LinkedList<Integer>());
        }
       
        min = inputMin.get(kdReference);
        max = inputMax.get(kdReference);
      }

      
      // Analyze coordinates of the array access
      coord.clear();
      coordNormal.clear();
      
      int nbDims=0;
      while (a>=0) {
        nbDims++;
        int b = variableAccessReference.indexOf("]",a);
        if (b<0) {
          ce.raiseError(fNode, getRelativeLine(),"while parsing the index of iterative variable '"+name+"' (missing ']')");
          error=true;
          break;
        }
        // Get the index
        boolean unknownCoordinates=false;
        String sIndex=variableAccessReference.substring(a + 1, b).trim();
        int index=0;
        try {
          index=Integer.parseInt(sIndex);
        } catch (NumberFormatException e) {
          // This is not an error, just an expression
          unknownCoordinates=true;
          ce.raiseWarning(1,fNode, getRelativeLine(),"Unknown index '"+sIndex+"' for iterative variable '"+name+"'");
        }
        if (unknownCoordinates) {
          coord.add(sIndex);
        }
        else {
          coord.add(index);
        }
        
        // Next one
        a = variableAccessReference.indexOf("[",b);
      }

      // Check the consistency of the number of dimensions of the access with
      // the referenced data
      if (kdReference!=null) {
        if (kdReference.getMatrixType()!=null) {
          if (nbDims!=kdReference.getMatrixType().getNbDims()) {
            ce.raiseError(fNode, getRelativeLine(),"access of "+nbDims+" dimension(s) for variable '"+kdReference.getName()+"' that has "+
                kdReference.getMatrixType().getNbDims()+" dimension(s)"); 
          }
        }
        else {
            // This is an output, do not know yet the matrix type ...
            // Will test for a correct access at the end of processing
        }
      }

      switch (indexing) {
      case C:
        /* The indexing is in the reverse order compared to the standardized kernel genius order */
        i=0;
        for(int c=coord.size()-1;c>=0;c--,i++) {
          Object oq=coord.get(c);
          coordNormal.add(oq);
          if (i>=min.size()) {
            if (oq instanceof Integer) {
              min.add((Integer)oq);
              max.add((Integer)oq);
            }
            else {
              min.add(null);
              max.add(null); 
            }
          }
          else {
            if (min.get(i)!=null) {
              if (oq instanceof Integer) { 
                int q=(Integer)oq;
                if (q<min.get(i)) {
                  min.set(i, q);
                }
                if (q>max.get(i)) {
                  max.set(i, q);
                }
              }
              else {
                min.set(i, null);
                max.set(i, null);
              }
            }
          }
        }
        break;
      case MATH:
        /* The indexing is in the good order */
        for(int c=0;c<coord.size();c++) {
          Object oq=coord.get(c);
          coordNormal.add(oq);
          if (c>=min.size()) {
            if (oq instanceof Integer) {
              min.add((Integer)oq);
              max.add((Integer)oq);
            }
            else {
              min.add(null);
              max.add(null); 
            }
          }
          else {
            if (min.get(i)!=null) {
              if (oq instanceof Integer) { 
                int q=(Integer)oq;
                if (q<min.get(c)) {
                  min.set(c, q);
                }
                if (q>max.get(c)) {
                  max.set(c, q);
                }
              }
              else {
                min.set(i, null);
                max.set(i, null);
              }
            }
          }
        }
        break;
      default:
        ce.raiseInternalError(fNode,getClass().getName()+": bad indexing property '"+indexing+"'");
      }

      updateRelativeLine(variableAccessReferenceFull);

      // Add the index object
      KernelDataCoordinate dir= new KernelDataCoordinate(kdReference,coordNormal);
      fList.add(dir);
      if (kdReference==this) { outputAccessList.add(dir); }

      // Next
      i1=s.indexOf("$",j);i2=s.indexOf("@",j);
      if (i1<0) {i=i2;} else { if (i2<0) {i=i1;} else { if (i1<i2) {i=i1;} else {i=i2;} } }
    }
    // Get the Last string
    fList.add(s.substring(j, s.length()));

    
    //=========================================================
    // Inputs management
    //=========================================================

    // Check that at least one iterative input is defined 
    if (iterationSet.size()==0) {
      ce.raiseError(functionNode, "No iterative input defined by the function");
      error=true;
    }

    // Sets iterative inputs and the 'read from function' patterns for each one
    for(KernelData kd:iterationSet) {
      addInputEdge(kd);
      readBoundFromFunctionProperty.put(kd,new TypedMatrixIndexes(kd.getMatrixType().getBaseCType(),inputMin.get(kd),inputMax.get(kd)));
    }
    
    //=========================================================
    // Output management
    //=========================================================

    // Check that the output is written
    if (outputMin.size()==0) {
      ce.raiseError(fNode, "No reference to output '@"+getName()+"' in the function");
      error=true;
    } 
     
    // Check the number of dimensions of output accesses (that is determined by iterative inputs)
    int nbDims=getFirstInputEdge().getSourceData().getMatrixType().getNbDims();
    for (KernelDataCoordinate dir:outputAccessList) {
      if (dir.getNbDims()!=nbDims) {
        ce.raiseError(fNode,"access of "+dir.getNbDims()+" dimension(s) for variable '"+getName()+"' that has "+
            nbDims+" dimension(s)"); 
        error=true;
      }
    }
    
    // Set output pattern
    //setWritePattern(new TypedMatrixIndexes(getOutputBaseCType(),outputMin,outputMax));
    writeBoundFromFunctionProperty=new TypedMatrixIndexes(getOutputBaseCType(),outputMin,outputMax);
    
    return error;
  }
  
  //------------------------------------------------------------------------
  // Get the full string corresponding to an array access.
  //------------------------------------------------------------------------
  int getEndOfReference(String s, int start, TNode fNode, CompilerError ce) {
    int size=s.length();
    int i;
    
    // Remove spaces
    for (i=start;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    // Get the identifier
    for (;i<size;i++) {
      char c=s.charAt(i);
      if (((c<'0')||(c>'9')) && ((c<'a')||(c>'z')) && ((c<'A')||(c>'Z')) && (c!='_')) {
        break;
      }
    }

    // Get 
    for (;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    while ((i<size) && (s.charAt(i)=='[')) {
      // Look for the other
      i = s.indexOf("]",i);
      if (i<0) {
        ce.raiseError(fNode,"while parsing the function of Filter '"+getName()+"' (missing ']')");
        return size;
      }
      for (i++;(i<size)&&Character.isWhitespace(s.charAt(i));i++);
    }
    
    return i;
  }


  
  //==================================================================
  // Data access patterns
  //
  // All properties fully dependent on user code, so all initialized
  // at null
  //==================================================================
   
  public void applyAccessPatternContraints() {
    // Nothing to do on inputs, since only one input
  }


  //========================================================
  // Code generation
  //========================================================

  public void generateConstLiterals(PrintStream ps) {
    // Nothing
  }

  
  protected void generateNodeComputeFunction(
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList, 
      Set<KernelData> internalData,
      PrintStream ps,
      String prefix) {
    
    ps.print(prefix);
    
    // The function node may be null for predefined nodes
    if (functionNode!=null) {
      functionNode.generatePreproDirective(ps);
    }
    for(Object o:functionList) {
      if (o instanceof KernelDataCoordinate) {
        KernelDataCoordinate mi=(KernelDataCoordinate)o;
        generateMatrixIndex(mi,firstIndexList,lastIndexList, lastIndexStringPlusOneList,internalData,ps);
      }
      else {
        ps.print(o);
      }
    }
    ps.println();
  }


  public void generateMatrixIndex(
      KernelDataCoordinate mi,
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList,
      Set<KernelData> internalData,
      PrintStream ps) {

    if (mi.getNbDims()!=firstIndexList.size()) {
      // TODO: ERROR
      System.err.println("Internal Error : mi.getNbDims()!=firstIndexList.size() ("+mi.getNbDims()+"!="+firstIndexList.size()+")");
      CompilerError.exitWithError();
    }
   
    KernelData kd=mi.getSourceData();
    
    StringBuffer sb=new StringBuffer();
    StringBuffer sConstBorder=null;
    boolean constBorder=false;
    boolean exp=false;
    for (int dim=mi.getNbDims()-1, n=0;dim>=0;dim--, n++) {
      Object oi=mi.getIndex(dim);

      if (oi instanceof Integer) {
        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // The index is a fixed integer
        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        int i=(Integer)oi;

        int firstIndex=firstIndexList.get(dim);
        int lastIndex=lastIndexList.get(dim);
        String lastIndexStringPlusOne=lastIndexStringPlusOneList.get(dim);

        // Borders only applied to inputs
        if ((i<firstIndex)&&(mi.getSourceData()!=this)) {
          // *************** left ****************
          switch(getInputEdge(kd).getBorderMode().getType()) {
          case CONST_VALUE:
            constBorder=true;
            break;
          case EXP:
            exp=true;
            break;
            
          case DUPLICATE:
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  
            
            if (dim==0) {
              CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
              sb.append("+");
              sb.append(firstIndex);

              //sb.append("0");
            }
            else {
              sb.append(firstIndex);
            } 
            
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case MIRROR:
            // BUG with y ??
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {
              CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
              sb.append("+");
              sb.append(2*firstIndex-i-1);

              //sb.append(firstIndex-i-1);
            }
            else {
              sb.append(2*firstIndex-i-1);
            }
            
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case SKIP:
          case UNDEF:
            // not processed at the level
            break;
          default:
            System.err.println("Internal Error, bad border type : "+getInputEdge(kd).getBorderMode().toString());
            CompilerError.exitWithError();      
          }
        }

        // Borders only applied to inputs
        else if ((i>lastIndex)&&(mi.getSourceData()!=this)) {
          // *************** right ****************
          switch(getInputEdge(kd).getBorderMode().getType()) {
          case CONST_VALUE:
            constBorder=true;
            break;
          case EXP:
            exp=true;
            break;

          case DUPLICATE:
            // Bug for Y axis ??
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {           
              CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
              sb.append("+");
              sb.append(lastIndex);

              //sb.append("(");
              //sb.append(lastIndexStringPlusOne).append("-1");
              //sb.append(")");
            }
            else {
              sb.append(lastIndex);
            }
            
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case MIRROR:
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {
              CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
              sb.append("+");
              sb.append(2*lastIndex-i+1);          
              
              //sb.append("(");
              //sb.append(lastIndexStringPlusOne).append("-").append(i-lastIndex);
              //sb.append(")");
            }
            else {
              sb.append(2*lastIndex-i+1);
            }
            
            if (CodegenOptions.isImageKernelMode()) {
              sb.append("]");
            }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case SKIP:
          case UNDEF:
            // not processed at the level
            break;
          default:
            System.err.println("Internal Error, bad border type : "+getInputEdge(kd).getBorderMode().toString());
            CompilerError.exitWithError();      
          }
        }

        else {
          // *************** center ****************
          if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
          else if (n!=0) { sb.append("+"); }  

          if (dim==0) {
            sb.append("(");
            appendLoopCounter(kd,sb);
            if (i<0)      { sb.append(i); }
            else if (i>0) { sb.append("+").append(i); }
            sb.append(")");
          }
          else { sb.append(i); }
          
          if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
          else {
            // Multiply by the plan size at the current dim
            for (int dim2=dim-1;dim2>=0;dim2--) {
              sb.append("*");
              sb.append(lastIndexStringPlusOneList.get(dim2));
            }
          }

        }
      } // If
 
      else {
        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
        // The index is a dynamic expression
        //$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        // Manage negative
        int firstIndex=firstIndexList.get(dim);
        int lastIndex=lastIndexList.get(dim);
        String lastIndexStringPlusOne=lastIndexStringPlusOneList.get(dim);
        
        // Potentially out of the border
        
        // Borders only applied to inputs
        if ((mi.getSourceData()!=this)&&(getInputEdge(kd).getReadPattern().getFirstIndex(dim)<firstIndex)) {
          // *************** left ****************
          switch(getInputEdge(kd).getBorderMode().getType()) {
          case CONST_VALUE:
            // prefix
            if (sConstBorder==null) { sConstBorder=new StringBuffer(); }
            else { sConstBorder.append("||"); }
            sConstBorder.append("(");
            sConstBorder.append("(");
            CLGenVarNames.appendIndex(sConstBorder,dim);
            sConstBorder.append("=").append(oi.toString()).append(")");
            sConstBorder.append("<").append(firstIndex);
            sConstBorder.append(")");

            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {
              sb.append("(");
              appendLoopCounter(kd,sb);
              sb.append("+");
              CLGenVarNames.appendIndex(sb,dim);
              sb.append(")");
            }
            else {
              CLGenVarNames.appendIndex(sb,dim);
            }

            if (CodegenOptions.isImageKernelMode()) {
              sb.append("]");
            }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
           break;
           
          case EXP:
            exp=true;
            break;
            
          case DUPLICATE:
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  
                                 
            if (dim==0) {
              sb.append("_KG_duplicate_inf0(");
              sb.append(oi.toString()).append(",");    
              sb.append(firstIndex).append(",");
              appendLoopCounter(kd,sb);
              sb.append(",(");
              sb.append(lastIndexStringPlusOne);
              sb.append(")");
              sb.append(")");
              // In-bound    : LoopCounter + oi
              // Out-of-bound: 0
           }
            else {
              sb.append("_KG_duplicate_inf1(");
              sb.append(oi.toString()).append(",");
              sb.append(firstIndex);
              sb.append(",(");
              sb.append(lastIndexStringPlusOne);
              sb.append(")");
              sb.append(")");
              // In-bound    : oi
              // Out-of-bound: firstIndex
            } 
            
            if (CodegenOptions.isImageKernelMode()) {
              sb.append("]");
            }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;

          case MIRROR:
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {
            	sb.append("_KG_mirror_inf0(");
            	sb.append(oi.toString()).append(",");    
            	sb.append(firstIndex).append(",");
            	appendLoopCounter(kd,sb);
            	sb.append(",(");
            	sb.append(lastIndexStringPlusOne);
            	sb.append(")");
            	sb.append(")");
            	// In-bound    : LoopCounter + oi
            	// Out-of-bound: firstIndex-1 - oi
           }
            else {
              sb.append("_KG_mirror_inf1(");
              sb.append(oi.toString()).append(",");
              sb.append(firstIndex);
              sb.append(",(");
              sb.append(lastIndexStringPlusOne);
              sb.append(")");
              sb.append(")");
              // In-bound    : oi
              // Out-of-bound: 2*firstIndex-1 - oi
            }
            
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case SKIP:
          case UNDEF:
            // not processed at the level
            break;
          default:
            System.err.println("Internal Error, bad border type : "+getInputEdge(kd).getBorderMode().toString());
            CompilerError.exitWithError();      
          }
        }

        // Borders only applied to inputs
        else if ((mi.getSourceData()!=this)&&(getInputEdge(kd).getReadPattern().getLastIndex(dim)>lastIndex)) {
         // *************** right ****************
          switch(getInputEdge(kd).getBorderMode().getType()) {
          case CONST_VALUE:
            // prefix
            if (sConstBorder==null) { sConstBorder=new StringBuffer(); }
            else { sConstBorder.append("||"); }
            sConstBorder.append("(");
            sConstBorder.append("(");
            CLGenVarNames.appendIndex(sConstBorder,dim);
            sConstBorder.append("=").append(oi.toString()).append(")");
            sConstBorder.append(">").append(lastIndex);
            sConstBorder.append(")");

            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  

            if (dim==0) {
              sb.append("(");
              appendLoopCounter(kd,sb);
              sb.append("+");
              CLGenVarNames.appendIndex(sb,dim);
              sb.append(")");
            }
            else {
              CLGenVarNames.appendIndex(sb,dim);
            }

            if (CodegenOptions.isImageKernelMode()) {
              sb.append("]");
            }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
           break;
 
          case EXP:
            exp=true;
            break;
            
          case DUPLICATE:                          
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  
                    
            if (dim==0) {
              sb.append("_KG_duplicate_sup0(");
              sb.append(oi.toString()).append(",");    
              sb.append(lastIndex).append(",");
              appendLoopCounter(kd,sb);
              sb.append(")");
              // In-bound    : LoopCounter + oi
              // Out-of-bound: lastIndexStringPlusOne-1        
            }
            else {
              sb.append("_KG_duplicate_sup1(");
              sb.append(oi.toString()).append(",");
              sb.append(lastIndex);
              sb.append(")");
              // In-bound    : oi
              // Out-of-bound: lastIndex       
            } 
            
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
                  
          case MIRROR:
            if (CodegenOptions.isImageKernelMode()) { sb.append("["); }
            else if (n!=0) { sb.append("+"); }  
           
            if (dim==0) {
              sb.append("_KG_mirror_sup0(");
              sb.append(oi.toString()).append(",");
              sb.append(lastIndex).append(",");
              appendLoopCounter(kd,sb);
              sb.append(")");
              // In-bound    : LoopCounter + oi
              // Out-of-bound: lastIndexStringPlusOne + lastIndex - oi
            }
            else {
              sb.append("_KG_mirror_sup1(");
              sb.append(oi.toString()).append(",");
              sb.append(lastIndex).append(",");
              sb.append(2*lastIndex+1);
              sb.append(")");
              // In-bound    : oi
              // Out-of-bound: 2*lastIndex+1 - oi
            } 
                    
            if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
            else {
              // Multiply by the plan size at the current dim
              for (int dim2=dim-1;dim2>=0;dim2--) {
                sb.append("*");
                sb.append(lastIndexStringPlusOneList.get(dim2));
              }
            }
            break;
            
          case SKIP:
          case UNDEF:
           // Not processed at the level
            break;
          default:
            System.err.println("Internal Error, bad border type : "+getInputEdge(kd).getBorderMode().toString());
            CompilerError.exitWithError();      
          }
        }

        else {
          // *************** center ****************  
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("[");
          }
          else if (n!=0) {
            sb.append("+");
          }  

          if (dim==0) {
            sb.append("(");
            appendLoopCounter(kd,sb);
            sb.append("+");
            sb.append("(").append(oi.toString()).append(")");
            sb.append(")");
          }
          else {
            sb.append("(").append(oi.toString()).append(")");
          }
          
          if (CodegenOptions.isImageKernelMode()) { sb.append("]"); }
          else {
            // Multiply by the plan size at the current dim
            for (int dim2=dim-1;dim2>=0;dim2--) {
              sb.append("*");
              sb.append(lastIndexStringPlusOneList.get(dim2));
            }
          }

        }
        
      }

    } // For

    
    // Actual generation
    if (internalData.contains(mi.getSourceData())) {
      // For internal data of macro-nodes
      ps.print(mi.getSourceData().getName());
    }
    else {
      // Non macro-nodes
      if (constBorder) {
        ps.print("(");   
        getInputEdge(kd).getBorderMode().generateValueLiteral(ps);
        ps.print(")");
      }
      else if (exp) {
        generateEXP(mi,firstIndexList,lastIndexList,lastIndexStringPlusOneList,ps);
      }
      else {
        if (sConstBorder!=null) {
          ps.print("(");
          ps.print("(");
          ps.print(sConstBorder);
          ps.print(")?");
          getInputEdge(kd).getBorderMode().generateValueLiteral(ps);
          ps.print(":"); 
        }

        ps.print(mi.getSourceData().getName());
        if (CodegenOptions.isTileKernelMode()) {
          ps.print("[");
        }
        ps.print(sb.toString());
        if (CodegenOptions.isTileKernelMode()) {
          ps.print("]");
        }

        if (sConstBorder!=null) {
          ps.print(")");
        }
      }
    }
  }

  // EXP management
  public void generateEXP(
      KernelDataCoordinate mi,
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList,
      PrintStream ps) {

    KernelData kd=mi.getSourceData();

    ps.print("(");
    // The function node may be null for predefined nodes
    if (getInputEdge(kd).getBorderMode().getASTNode()!=null) {
      getInputEdge(kd).getBorderMode().getASTNode().generatePreproDirective(ps);
    }
    for(Object o:getInputEdge(kd).getBorderMode().getBorderFunctionList()) {
      if (o instanceof KernelDataCoordinate) {
        KernelDataCoordinate miEXP=(KernelDataCoordinate)o;
        generateEXPMatrixIndex(miEXP,mi,firstIndexList,lastIndexList, lastIndexStringPlusOneList,ps);
      }
      else {
        ps.print(o);
      }
    }
    ps.print(")");
  }
    

  public void generateEXPMatrixIndex(
      KernelDataCoordinate miEXP,
      KernelDataCoordinate mi,
      List<Integer> firstIndexList,
      List<Integer> lastIndexList,
      List<String> lastIndexStringPlusOneList,
      PrintStream ps) {

  	
  	KernelData kd=mi.getSourceData();
  	
    if (mi.getNbDims()!=firstIndexList.size()) {
      System.err.println("Internal Error : mi.getNbDims()!=firstIndexList.size() ("+mi.getNbDims()+"!="+firstIndexList.size()+")");
      CompilerError.exitWithError();
    }

    StringBuffer sb=new StringBuffer();
    for (int dim=mi.getNbDims()-1, n=0;dim>=0;dim--,n++) {

      Object oi=mi.getIndex(dim);

      if (oi instanceof Integer) {
        int i=(Integer)oi;


        int firstIndex=firstIndexList.get(dim);
        int lastIndex=lastIndexList.get(dim);

        String lastIndexStringPlusOne=lastIndexStringPlusOneList.get(dim);

        if (i<firstIndex) {
          //***** left *******
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("[");
          }
          else if (n!=0) {
            sb.append("+");
          }
          if (dim==0) {
          	CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
          	sb.append("+");
          	sb.append(firstIndex);
          	sb.append("+");
          }
          sb.append(miEXP.getIndex(0));
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("]");
          }
          else {
            // Multiply by the plan size at the current dim
            for (int dim2=dim-1;dim2>=0;dim2--) {
              sb.append("*");
              sb.append(lastIndexStringPlusOneList.get(dim2));
            }
          }
        }

        else if (i>lastIndex) {
          // **** right ****
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("[");
          }
          else if (n!=0) {
            sb.append("+");
          }  
          if (dim==0) {
          	// x axis
          	sb.append("(");
          	CLGenVarNames.appendComputeLoopCounterPix(kd,sb);
          	sb.append("+");
          	sb.append(lastIndex);
            sb.append("-");
          	sb.append((Integer)miEXP.getIndex(0));
          	sb.append(")");

            //sb.append("(");
            //sb.append(lastIndexStringPlusOne);
            //sb.append("-");
            //sb.append(1+(Integer)miEXP.getIndex(0));
            //b.append(")");
          }
          else {
            // y and other axis
            sb.append(lastIndex-(Integer)miEXP.getIndex(0)); 
          }
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("]");
          }
          else {
            // Multiply by the plan size at the current dim
            for (int dim2=dim-1;dim2>=0;dim2--) {
              sb.append("*");
              sb.append(lastIndexStringPlusOneList.get(dim2));
            }
          }
        }

        else {
          // **** center **** 
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("[");
          }
          else if (n!=0) {
            sb.append("+");
          }  
          if (dim==0) {
            appendLoopCounter(mi.getSourceData(),sb);
            if (i<0) {
              sb.append(i);
            }
            else if (i>0) {
              sb.append("+");
              sb.append(i);
            }
          }
          else {
            sb.append(i);
          }
          if (CodegenOptions.isImageKernelMode()) {
            sb.append("]");
          }
          else {
            // Multiply by the plan size at the current dim
            for (int dim2=dim-1;dim2>=0;dim2--) {
              sb.append("*");
              sb.append(lastIndexStringPlusOneList.get(dim2));
            }
          }
        }
      } // if

      else {
        // TODO: Exp index not supported yet
        System.err.println("Internal Error : Exp index not supported yet");
        CompilerError.exitWithError();
      }

    } // for

    // Actual generation
    ps.print(mi.getSourceData().getName());
    if (CodegenOptions.isTileKernelMode()) {
      ps.print("[");
    }
    ps.print(sb.toString());
    if (CodegenOptions.isTileKernelMode()) {
      ps.print("]");
    }
  }

  // TODO: Considers today that a program is generated once
  static boolean generateRuntimeFunctions=false;
  public void generateRuntimeFunctions(PrintStream ps) { 
    if (!generateRuntimeFunctions) {
      generateRuntimeFunctions=true;
      ps.println(
          "// Runtime functions for Filter nodes\n"+
              "int _KG_duplicate_inf0(int oi,int firstIndex,int loopCounter,int size) {\n"+
              "  return oi<firstIndex?loopCounter+firstIndex:(oi-firstIndex>=size?size-1+firstIndex:loopCounter+oi);\n"+
              "}\n"+
              "int _KG_duplicate_inf1(int oi,int firstIndex,int size) {\n"+
              "  return oi<firstIndex?firstIndex:(oi-firstIndex>=size?size-1+firstIndex:oi);\n"+
              "}\n"+
              "int _KG_mirror_inf0(int oi,int firstIndex,int loopCounter,int size) {\n"+
              "  return oi<firstIndex?loopCounter+2*firstIndex-1-oi:(oi-firstIndex>=size?2*size-1+2*firstIndex-oi:loopCounter+oi);\n"+
              "}\n"+
              "int _KG_mirror_inf1(int oi,int firstIndex,int size) {\n"+
              "  return oi<firstIndex?2*firstIndex-1-oi:(oi-firstIndex>=size?2*size-1+2*firstIndex-oi:oi);\n"+
              "}\n"+
              "int _KG_duplicate_sup0(int oi,int lastIndex,int loopCounter) {\n"+
              "  return oi>lastIndex?loopCounter+lastIndex:loopCounter+oi;\n"+
              "}\n"+
              "int _KG_duplicate_sup1(int oi,int lastIndex) {\n"+
              "  return oi>lastIndex?lastIndex:oi;\n"+
              "}\n"+
              "int _KG_mirror_sup0(int oi,int lastIndex,int loopCounter) {\n"+
              "  return oi>lastIndex?loopCounter+2*lastIndex+1-oi:loopCounter+oi;\n"+
              "}\n"+
              "int _KG_mirror_sup1(int oi,int lastIndex,int oob_inc) {\n"+
              "  return oi>lastIndex?oob_inc-oi:oi;\n"+
              "}\n"
          );
    }
  }
  
  
  //========================================================
  // Verbose
  //========================================================

   public String toString() {
     StringBuffer sb = new StringBuffer();
      sb.append("'").append(this.getClass().getSimpleName()).append("' ");
      // Algorithm toString
      sb.append(super.toString());
      // Specific to filter
      if (this.getClass()==Filter.class) {
        sb.append("; function = ").append(functionList);
      }
      return sb.toString();
   }

}
