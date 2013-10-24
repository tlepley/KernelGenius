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

package codegen.OpenCL;

import ir.base.KernelData;
import ir.types.kg.MatrixIndexes;
import java.io.PrintStream;
import org.antlr.runtime.RecognitionException;
import parser.KernelGeniusEmitter;
import codegen.CodegenDataPattern;
import common.CompilerError;
import driver.options.CodegenOptions;

public class CLGenKernelData {

  
  static void generateWGFullSliceSizePixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    // Compute the full slice size (unit = pixel)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateFullSliceSizePixUnit(kd,dim,ps);
    ps.print("= ");

    ps.print("(");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.print("+");
    ps.print(-kd.getTileNbhForSuccessors().getFirstIndex(dim)); 
    ps.print("+");
    ps.print(kd.getTileNbhForSuccessors().getLastIndex(dim)); 
    ps.print(")");
    generateTileGrainMultiply(kd,dim,ps);
    ps.println(";");
  }

  
  static void generateMyWGSliceLeftExtentPixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    // Compute my own left slice extension (unit = pix)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSliceLeftExtentPixUnit(kd,dim,ps);
    ps.print("= ");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(kd,dim,ps);
    generateTileGrainMultiply(kd,dim,ps);
    ps.println(";");
  }

  static void generateMyWGSliceLeftExtentGrainInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    // Compute my own left slice extension (unit = grain)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(kd,dim,ps);
    ps.print("= ");
    if (kd.getTileNbhForSuccessors().getFirstIndex(dim)==0) {
      ps.print("0");

    }
    else { 
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
      ps.print("-");
      ps.print(-kd.getTileNbhForSuccessors().getFirstIndex(dim)/kd.getTileGrainForGraph().getNbElements(dim));
      ps.print(">=0?");
      ps.print(-kd.getTileNbhForSuccessors().getFirstIndex(dim)/kd.getTileGrainForGraph().getNbElements(dim));
      ps.print(":");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
    }
    ps.println(";");
  }

  // Compute my own right slice extension (unit = grain)
  static void generateMyWGSliceRightExtentGrainInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(kd,dim,ps);
    ps.print("= ");
    if (kd.getTileNbhForSuccessors().getLastIndex(dim)==0) {
      ps.print("0");
    }
    else {
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
      ps.print("+");
      CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
      ps.print("+");
      ps.print(kd.getTileNbhForSuccessors().getLastIndex(dim)/kd.getTileGrainForGraph().getNbElements(dim));
      ps.print("-");
      CLGenVarNames.generateNbGrains(dim,ps);
      ps.print("<=0?");
      ps.print(kd.getTileNbhForSuccessors().getLastIndex(dim)/kd.getTileGrainForGraph().getNbElements(dim));
      ps.print(":");
      CLGenVarNames.generateNbGrains(dim,ps);
      ps.print("-");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
      ps.print("-");
      CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    }
    ps.println(";");
  }


  // Compute my own full slice position (unit = pixel)
  static void generateMyWGFullSlicePositionPixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyFullSlicePositionPixUnit(kd,dim,ps);
    ps.print("= ");
    ps.print("(");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
    ps.print("-");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(kd,dim,ps);
    ps.print(")");
    generateTileGrainMultiply(kd,dim,ps);

    ps.println(";");
  }

  // Compute my own full slice size (unit = pixel)
  static void generateMyWGFullSliceSizePixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyFullSliceSizePixUnit(kd,dim,ps);
    ps.print("= ");
    CLGenVarNames.generateMyFullSlicePositionPixUnit(kd,dim,ps);
    ps.print("+");
    ps.print("(");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(kd,dim,ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(kd,dim,ps);
    ps.print(")");
    generateTileGrainMultiply(kd,dim,ps);
    
    ps.print(">=");
    kd.getMatrixType().generateLastIndexPlusOne(dim,ps);
    ps.print("?");
    
    kd.getMatrixType().generateLastIndexPlusOne(dim,ps);
    ps.print("-");
    CLGenVarNames.generateMyFullSlicePositionPixUnit(kd,dim,ps);
    
    ps.print(":");
    ps.print("(");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(kd,dim,ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(kd,dim,ps);
    ps.print(")");
    generateTileGrainMultiply(kd,dim,ps);

    ps.println(";");
  }

  
  static void generateMyWGSlicePositionPixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {    
    // Compute my own slice position (unit = pixel)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMySlicePositionPixUnit(kd,dim,ps);
    ps.print("= ");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
    generateTileGrainMultiply(kd,dim,ps);
    
    ps.println(";");
  }
    
  static void generateMyWGSliceSizePixInfos(KernelData kd, int dim, PrintStream ps, String prefix ) {
    // Compute my own slice size (unit = pixel)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSliceSizePixUnit(kd,dim,ps);
    ps.print("= ");
    
    CLGenVarNames.generateMySlicePositionPixUnit(kd,dim,ps);
    ps.print("+");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    generateTileGrainMultiply(kd,dim,ps);
    ps.print(">=");
    kd.getMatrixType().generateLastIndexPlusOne(dim,ps);
    ps.print("?");
    
    kd.getMatrixType().generateLastIndexPlusOne(dim,ps);
    ps.print("-");
    CLGenVarNames.generateMySlicePositionPixUnit(kd,dim,ps);
    
    ps.print(":");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    generateTileGrainMultiply(kd,dim,ps);
    
    ps.println(";");
  }
 
  
  // Take the full slice
  static void generateWGSliceInPositioning(KernelData kd, PrintStream ps, String prefix ) {
    ps.print(prefix);
    ps.print(kd.getName());
    ps.print("+=");
    for (int i=0;i<kd.getMatrixType().getNbDims();i++) {
      if (i>0) {ps.print("+");}
      CLGenVarNames.generateMyFullSlicePositionPixUnit(kd,i,ps);
      if (i>0) {
        ps.print("*(") ;
        for (int j=0;j<i;j++) {
          if (j>0) {ps.print("*");}
          ps.print("(") ;
          kd.getMatrixType().generateLastIndexPlusOne(j,ps);
          ps.print(")") ;
        }
        ps.print(")");
      }
    }
    ps.println(";");
  }

  // Take the standard slice
  static void generateWGSliceOutPositioning(KernelData kd, PrintStream ps, String prefix ) {
    ps.print(prefix);
    ps.print(kd.getName());
    ps.print("+=");
    for (int i=0;i<kd.getMatrixType().getNbDims();i++) {
      if (i>0) {ps.print("+");}
      CLGenVarNames.generateMySlicePositionPixUnit(kd,i,ps);
      if (i>0) {
        ps.print("*(") ;
        for (int j=0;j<i;j++) {
          if (j>0) {ps.print("*");}
          ps.print("(") ;
          kd.getMatrixType().generateLastIndexPlusOne(j,ps);
          ps.print(")") ;
        }
        ps.print(")");
      }
    }
    ps.println(";");
  }


  static public void generateKernelLocalCachingNonIterative(KernelData kd, PrintStream ps, String prefix) {

    // We simply cache the data into local memory in normal mode
    // In Image node, there is no need to cache data in local memory
    if (CodegenOptions.isImageKernelMode()) {
      // Local array declaration
      ps.print(prefix);
      ps.print("local ");
      kd.getMatrixType().generate(ps, kd.getBaseCTypeNode(), CLGenVarNames.getKernelLocalName(kd));
      ps.println(";");

      // Async copy
      ps.print(prefix);
      ps.print("event_t ");
      CLGenVarNames.generateKernelEventName(kd, ps);
      ps.print(" = async_work_group_copy(");
      ps.print("(local char *)(");
      CLGenVarNames.generateKernelLocalName(kd, ps);
      ps.print("),");
      ps.print("(global char *)(");
      ps.print(kd.getName());
      ps.print("),");
      kd.getMatrixType().generateSizeInBytes(ps, kd.getBaseCTypeNode());
      ps.print(",0");
      ps.println(");");

      // Wait
      ps.print(prefix+"wait_group_events(1,&");
      CLGenVarNames.generateKernelEventName(kd, ps);
      ps.println(");");
    }
  }


  static public void generateKernelLocalBufferDeclaration(KernelData kd, PrintStream ps, String prefix) {

    CodegenDataPattern codegenPattern=kd.getCodegenDataPattern();
    if (codegenPattern==null) {
      // TODO: Error
    }
    else {      
      if (codegenPattern.getDataAccessType()!=CodegenDataPattern.ComputeGranularity.D2_BY_RAW) {
        // TODO: Error, not supported today
      }

      int nbSubBuf=codegenPattern.getNbBufferSlot();   
      if ( (kd.getAccessPatternUnion().getIndexRangeSpecifier(1)==null) && 
          ( (kd.getAccessPatternUnion().getFirstIndexType(1)==MatrixIndexes.IndexType.NOT_SPECIFIED) ||
              (kd.getAccessPatternUnion().getLastIndexType(1)==MatrixIndexes.IndexType.NOT_SPECIFIED) )
          ) {
        // TODO: fully variable, not supported today
        CompilerError.GLOBAL.raiseFatalError(
            "generateKernelLocalBufferDeclaration: Fully variable intrinsic data access pattern, not supported today");
      }

      // The extension of the circular buffer is related to the usage of the buffer
      // by who writes and who reads this data, and by the 'left' tiling property in dim1
      // Since the scheduler loop starts with negative tile number
      int circBufferBegin=kd.getAccessPatternUnion().getFirstIndex(1)+kd.getTileNbhForSuccessors().getFirstIndex(1);
      int circBufferSize=nbSubBuf+kd.getAccessPatternUnion().getLastIndex(1)-circBufferBegin;

      // Circular buffer
      ps.print(prefix);
      if (CodegenOptions.isImageKernelMode()) {
        ps.print("local ");
      }
      else {
        ps.print("global ");
      }
      generateBaseCType(kd, ps);
      ps.print(" *");
      CLGenVarNames.generateKernelLocalCircularName(kd, ps);
      ps.print("[");
      ps.print(circBufferSize);
      ps.print("] = {");
      for(int i=0;i<circBufferSize;i++) {
        int j=(i+nbSubBuf+circBufferBegin)%nbSubBuf;
        if (i!=0) {ps.print(",");}
        CLGenVarNames.generateKernelLocalBufferName(kd, ps);
        // Center to the tile coordinate [0,0]
        ps.print("+");
        ps.print(-kd.getTileNbhForSuccessors().getFirstIndex(0));
        if (j>0) {
          ps.print("+");
          if (j>1) {
            ps.print(j);
            ps.print("*");
          }
          CLGenVarNames.generateFullSliceSizePixUnit(kd,0,ps);
          //kd.getMatrixType().generateLastIndexPlusOne(0,ps);
        }
      }
      ps.println("};");

      // Local buffer
      ps.print(prefix);
      if (CodegenOptions.isImageKernelMode()) {
        ps.print("local ");
      }
      else {
        ps.print("local ");             
      }
      generateBaseCType(kd, ps);
      ps.print(" **");
      CLGenVarNames.generateKernelLocalName(kd, ps);
      ps.print("= &");
      CLGenVarNames.generateKernelLocalCircularName(kd, ps);
      ps.print("[");
      ps.print(-circBufferBegin);
      ps.println("];");
    } 
  }

  static public void generateKernelEventDeclarationOutput(KernelData kd, PrintStream ps, String prefix) {
    ps.print(prefix);
    ps.print("event_t ");
    CLGenVarNames.generateKernelEventName(kd,kd.getRate()*kd.getPush(), ps);
    ps.print(";");
  }


  static public void generateKernelEventDeclarationInput(KernelData kd, PrintStream ps, String prefix) {
    ps.print(prefix);
    ps.print("event_t ");
    CLGenVarNames.generateKernelEventName(kd,kd.getRate(), ps);
    ps.print(";");
  }


  static public void generateKernelTileDeclaration(KernelData kd, PrintStream ps, String prefix) {
    CodegenDataPattern codegenPattern=kd.getCodegenDataPattern();
    if (codegenPattern==null) {
      // TODO: Error
    }
    else {      
      if (codegenPattern.getDataAccessType()!=CodegenDataPattern.ComputeGranularity.D2_BY_RAW) {
        // TODO: Error, not supported today
      }

      if ( (kd.getAccessPatternUnion().getIndexRangeSpecifier(1)==null) && 
          ( (kd.getAccessPatternUnion().getFirstIndexType(1)==MatrixIndexes.IndexType.NOT_SPECIFIED) ||
              (kd.getAccessPatternUnion().getLastIndexType(1)==MatrixIndexes.IndexType.NOT_SPECIFIED) )
          ) {
        // TODO: fully variable, not supported today
        CompilerError.GLOBAL.raiseFatalError(
        "generateKernelTileDeclaration: Fully variable intrinsic data access pattern, not supported today");
      }

      // Position the Tile pointer to the correct line
      ps.print(prefix);
      ps.print("global ");
      generateBaseCType(kd, ps);
      ps.print(" *");
      CLGenVarNames.generateKernelTilePtrName(kd,ps);
      ps.print(" =");
      ps.print(kd.getName());
      if (kd.getAccessPatternUnion().getFirstIndex(1)!=0) {
        ps.print("+min(");
        ps.print(-kd.getAccessPatternUnion().getFirstIndex(1));
        ps.print(",");
        CLGenVarNames.generateKernelTilePosition(1,ps);
        ps.print("*");
        kd.getMatrixType().generateLastIndexPlusOne(0,ps);
      }
      ps.println(";");
    }
  }


  static public int generateKernelLocalParamDeclaration(KernelData kd, PrintStream ps, int n) {
    if (kd.shouldBeCached()) {
      // It needs a local temporary buffer

      if (CodegenOptions.isTileKernelMode()) {
        // Only non IO data will have a local temporary buffer
        if (!kd.isKernelIOData()) {
          if (n!=0) {
            ps.print(", ");
          }
          ps.print("local ");
          generateBaseCType(kd, ps);
          ps.print(" * ");
          CLGenVarNames.generateKernelLocalBufferName(kd,ps);
          return 1;
        }
      }
      else {
        if (n!=0) {
          ps.print(", ");
        }
        ps.print("local ");
        generateBaseCType(kd, ps);
        ps.print(" * ");
        CLGenVarNames.generateKernelLocalBufferName(kd,ps);
        return 1;
      }
    }
    return 0;
  }


  static public void generateKernelParamDeclaration(KernelData kd, PrintStream ps) {
    if (!kd.getMatrixType().hasSingleElement()) {
      // It will be passed as a buffer
      ps.print("global ");
      generateBaseCType(kd,ps);
      ps.print(" * ");
      ps.print(kd.getName());
    }
    else {
      // It will be passed by value
      generateBaseCType(kd,ps);
      ps.print(" ");
      ps.print(kd.getName());    
    }
  }


  static public void generateComputeFunctionParamDeclaration(KernelData kd, MatrixIndexes pattern, PrintStream ps) {
    if (kd.isControlData()) {
      if (kd.isNonIterativeDataToCache()) {
        // Non iterative data data to cache
        if (CodegenOptions.isTileKernelMode()) {
          // Tile mode
          ps.print("global ");
        }
        else {
          // Image mode
          ps.print("local ");
        }
        kd.getMatrixType().generate(ps, kd.getBaseCTypeNode(), kd.getName());
      }
      else {
        // This is a control parameter copied by value
        generateBaseCType(kd,ps);
        ps.print(" ");
        ps.print(kd.getName());
      }
    }
    else {
      // This is a iterative computational data
      if (CodegenOptions.isTileKernelMode()) {
        // The tile is already given 
        ps.print("global ");
        generateBaseCType(kd,ps);
        switch (pattern.getNbDims()) {
        case 1:
        case 2:
          ps.print(" *");
          break;
        default:
          // Not supported
          CompilerError.GLOBAL.raiseInternalError(
              "access pattern for data '"+kd.getName()+"' type is "+pattern.getNbDims()+"D");
        }
      }
      else {
        // Image mode
        ps.print("local ");
        generateBaseCType(kd,ps);
        switch (pattern.getNbDims()) {
        case 1:
          // Standard buffer
          // For the moment, all circular buffers
          ps.print(" **");
          break;
        case 2:
          // Circular buffer
          ps.print(" **");
          break;
        default:
          // Not supported
          CompilerError.GLOBAL.raiseInternalError(
              "access pattern for data '"+kd.getName()+"' type is "+pattern.getNbDims()+"D");
        }
      }
      ps.print(kd.getName());
    }
  }


  static public void generateIOPointerDeclaration(KernelData kd, String prefix, PrintStream ps) {
    CodegenDataPattern outputPattern=kd.getCodegenDataPattern();
    if (outputPattern==null) {
      // This is a control parameter
    }
    else {
      // This is a computational data
      ps.print(prefix);
      ps.print("global ");
      generateBaseCType(kd,ps);
      ps.print(" *");
      ps.print(kd.getName());
      ps.print("_IO_pointer;");
    }
  }


  public static void generateWait(KernelData kd, int sched, int offset, String prefix, PrintStream ps) {
    // Wait for async copy
    ps.print(prefix+"  wait_group_events(1,&");
    CLGenVarNames.generateKernelEventName(kd,offset,ps);
    ps.println(");");
  }


  static public void generateOutputCopy(KernelData kd, 
      String prefix,
      //MatrixIndexes refFormat, int refRate,
      PrintStream ps) {
    int sched=kd.getCodegenDataPattern().getSchedulingCycle()+1;

    // If condition
    generateSchedulerIfCondition(kd,sched,false, prefix, ps);

    //--------------------------------------------------------------------
    // Note:
    // Async write operations do not have their own node in the IR.
    // They always have a push=1, so that their actual rate is rate*push 
    // of the function node that generates the data async-written
    //--------------------------------------------------------------------
    int actualPop=1;
    int actualRate=kd.getRate()*kd.getPush();
    for (int i=0;i<actualRate;i++) {

      // Additional if condition
      generateLineIfCondition(kd,sched,i,prefix,ps);

      // Async copy
      ps.print(prefix+"  ");
      CLGenVarNames.generateKernelEventName(kd,i,ps);
      ps.print(" = async_work_group_copy(");
      //if (CodegenOptions.getFixDMA()) {
      //  // Fake outputs as inputs
      //  ps.print("(local char*)(");
      //}
      //else {
        ps.print("(global char*)(");
      //}
      ps.print(kd.getName());

      // TODO: not correct, should be handled by the my tile size and position
      if (kd.getOutputSkipPattern().getFirstIndex(0)!=0) {
        ps.print("+");
        CLGenVarNames.generateSkipBeginPixUnit(kd,0,ps);
        //ps.print(-kd.getOutputSkipPattern().getFirstIndex(0));
      }

      //if (CodegenOptions.getFixDMA()) {
      //  // Fake outputs as inputs
      // ps.print("),(global char*)(");
      //}
      //else {
      ps.print("),(local char*)(");
      //}
      generateKernelLocalAccess(kd,sched,actualRate,actualPop,i, ps);
      if (kd.getOutputSkipPattern().getFirstIndex(0)!=0) {
        ps.print("+");
        CLGenVarNames.generateSkipBeginPixUnit(kd,0,ps);
        //ps.print(-kd.getOutputSkipPattern().getFirstIndex(0));
      }

      ps.print("),");
      ps.print("sizeof(");
      generateBaseCType(kd,ps);
      ps.print(")*(");
      CLGenVarNames.generateMyWGSliceSizePixUnit(kd,0,ps);

      // TODO: not correct, should be handled by the my tile size and position
      // Should not overwrite the border
      if ((kd.getOutputSkipPattern().getFirstIndex(0)!=0) || (kd.getOutputSkipPattern().getLastIndex(0)!=0)) {
        ps.print("-(");
        CLGenVarNames.generateSkipBeginPixUnit(kd,0,ps);
        ps.print("+");
        CLGenVarNames.generateSkipEndPixUnit(kd,0,ps);
        ps.print(")");
        //ps.print(kd.getOutputSkipPattern().getLastIndex(0)-kd.getOutputSkipPattern().getFirstIndex(0));
      }

      ps.print(")");   
      ps.println(",0);");

      ps.print(prefix);
      ps.println("}");    

      
      // Increment the pointer outside the conditional block
      ps.print(prefix+"  ");
      ps.print(kd.getName());
      ps.print("+=");
      kd.getMatrixType().generateLastIndexPlusOne(0,ps);
      ps.println(";");
    }

    // Close the main if
    ps.print(prefix);
    ps.println("}");
  }

  static public void generateOutputWait(KernelData kd, 
      String prefix,
      PrintStream ps) { 
    
    int sched=kd.getCodegenDataPattern().getSchedulingCycle()+1;
    
    // If condition
    generateSchedulerIfCondition(kd,sched,false, prefix, ps);
    
    for(int i=0;i<kd.getRate()*kd.getPush();i++) {
      generateLineIfCondition(kd,sched,i,prefix, ps);
      generateWait(kd,sched,i,prefix,ps);
      ps.print(prefix);
      ps.println("}");
    }
    
    // Close the if
    ps.print(prefix);
    ps.println("}");
  }
  
  
  static public void generateInputCopy(KernelData kd, 
      String prefix,
      //MatrixIndexes refFormat,int refRate,
      PrintStream ps) {
    int sched=kd.getCodegenDataPattern().getSchedulingCycle();

    // If condition
    generateSchedulerIfCondition(kd,sched,true, prefix, ps);

    for(int i=0;i<kd.getRate();i++) {
      // Note: Multiple 1D copies due to the circular buffer ...

        generateLineIfCondition(kd,sched,i,prefix,ps);

      // Only read if somebody will use it
      //      if (this.getIntrinsicAccessPattern().getNbElements(1)>i) {
      // Async copy
      ps.print(prefix+"  ");
      CLGenVarNames.generateKernelEventName(kd,i,ps);
      ps.print(" = async_work_group_copy(");
      
      if (CodegenOptions.getFixDMA()) {
        // Fake inputs as outputs
        ps.print("(global char*)(");
      }
      else {
        ps.print("(local char*)(");
      }
      generateKernelLocalAccess(kd,sched,kd.getRate(),kd.getPush(),i, ps);
      ps.print("-");
      CLGenVarNames.generateMyWGSliceLeftExtentPixUnit(kd,0,ps);
      if (CodegenOptions.getFixDMA()) {
        // Fake inputs as outputs
        ps.print("),(local char*)(");
      }
      else {
        ps.print("),(global char*)(");
      }
      ps.print(kd.getName());
      ps.print("),");
      ps.print("sizeof(");
      generateBaseCType(kd,ps);
      ps.print(")");
      // TODO: not correct for multi-WG (?)
      if (kd.getPush()!=1) {
        ps.print("*"+kd.getPush());
      }
      ps.print("*(");
      CLGenVarNames.generateMyFullSliceSizePixUnit(kd,0,ps);
      ps.print(")");
      ps.println(",0);");
      //      }

      // Close the if
      ps.print(prefix);
      ps.println("}");

      
      // Increment the pointer
      ps.print(prefix+"  ");
      ps.print(kd.getName());
      ps.print("+=");
      kd.getMatrixType().generateLastIndexPlusOne(0,ps);
      ps.println(";");
    }
    // Close the main if
    ps.print(prefix);
    ps.println("}");
  }
  
  static public void generateInputWait(KernelData kd,
      String prefix,
      PrintStream ps) { 
    
    int sched=kd.getCodegenDataPattern().getSchedulingCycle();

    // If condition
    generateSchedulerIfCondition(kd,sched,true,prefix, ps);
    for(int i=0;i<kd.getRate()*kd.getPush();i++) {
        generateLineIfCondition(kd,sched,i,prefix,ps);
      generateWait(kd,sched,i,
          prefix,ps);
      ps.print(prefix);
      ps.println("}");
    }
    
    // Close the if
    ps.print(prefix);
    ps.println("}");
  }
  
  static public void generateLocalBufferSizeFormula(KernelData kd, PrintStream ps) {
    if (kd.isIterative()) {
      int slot=kd.getCodegenDataPattern().getNbBufferSlot();
      // Size in bytes
      ps.print("sizeof(");
      generateBaseCType(kd, ps);
      ps.print(")");
      ps.print("*");
      if (slot!=1) {
        ps.print(slot);
        ps.print("*");
      }
      kd.getMatrixType().generateLastIndexPlusOne(0,ps);
    }
  }

  static public void generateBaseCType(KernelData kd, PrintStream ps) {
    KernelGeniusEmitter emitter=new KernelGeniusEmitter(ps,kd.getBaseCTypeNode());
    try {
      emitter.declSpecifiers();
    } catch (RecognitionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  
  static public void generateTilePositioning(KernelData kd, String prefix, int shift, PrintStream ps) {
    if (shift != 0) {
      ps.print(prefix);
      ps.print(kd.getName());
      ps.print("+=");
      ps.print("(");
      ps.print(shift);
      ps.print(")");
      ps.print("*");
      kd.getMatrixType().generateLastIndexPlusOne(0,ps);
      ps.println(";");
    }
  }

  static public void generateTileIncrement(KernelData kd, String prefix, PrintStream ps) {
    // TODO: if condition ? can save time in case of a graph
    ps.print(prefix);
    ps.print(kd.getName());
    ps.print("+=");
    kd.getMatrixType().generateLastIndexPlusOne(0,ps);
    ps.println(";");
  }

  static public void generateKernelLocalAccess(KernelData kd,int sched, int rate,int pop_push,
      int offset, PrintStream ps) {
    int slot=kd.getCodegenDataPattern().getNbBufferSlot();

    CLGenVarNames.generateKernelLocalName(kd,ps);
    ps.print("[(");
    if (pop_push!=1) {
      ps.print(pop_push);
      ps.print("*(");
    }
    if (rate!=1) {
      ps.print(rate);
      ps.print("*(");
    }
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    if (sched>0) {
      ps.print("-");
      ps.print(sched);
    }
    if (rate!=1) {
      ps.print(")");
    }
    if (offset!=0) {
      ps.print("+");
      ps.print(offset);
    }
    if (pop_push!=1) {
      ps.print(")");
    }
    ps.print(")%");
    ps.print(slot);
    ps.print("]");
  }


  static public void generateKernelCircLocalAccess(KernelData kd,int sched, int rate,int pop_push,
      int offset, PrintStream ps) {
    ps.print("&");
    generateKernelLocalAccess(kd,sched,rate,pop_push,offset,ps);
  }

  
  static public void generateLineIfCondition(
      KernelData kd,
      int sched,
      int i,
      String prefix, PrintStream ps) {

    // If condition
    ps.print(prefix);
    ps.print("if (");

    // Manage left skip
    if (kd.getOutputSkipPattern().getFirstIndex(1)!=0) {
        ps.print("(");
        ps.print("(");
        CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
        ps.print("+");
        CLGenVarNames.generateSchedulerLoopCounter(ps);
        ps.print(" - ");
        ps.print(sched);
        ps.print(")");
        generateTileGrainMultiply(kd,1,ps);
        ps.print(" + "+i);
        ps.print(">=");
        ps.print(-kd.getOutputSkipPattern().getFirstIndex(1));
        //CLGenVarNames.generateSkipBeginPixUnit(kd,1,ps);
        ps.print(")");
        ps.print("&&");
    }
    
    {
        // Manage right limit, including skip
        ps.print("(");
        ps.print("(");
        CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
        ps.print("+");
        CLGenVarNames.generateSchedulerLoopCounter(ps);
        ps.print(" - ");
        ps.print(sched);
        ps.print(")");
        generateTileGrainMultiply(kd,1,ps);
        ps.print(" + "+i);
        ps.print("<");
        //CLGenVarNames.generateMyWGSliceSizeGrainUnit(1,ps);
        //if (tileGrain!=1) {
        //  ps.print("*");
        //  ps.print(tileGrain);
        //}
        //if (kd.getOutputSkipPattern().getLastIndex(1)!=0) {
        //  ps.print("-");
        //  CLGenVarNames.generateSkipEndPixUnit(kd,1,ps);
        //}
        kd.getMatrixType().generateLastIndexPlusOne(1,ps);
        if (kd.getOutputSkipPattern().getLastIndex(1)!=0) {
            ps.print(" - ");
            ps.print(kd.getOutputSkipPattern().getLastIndex(1));
      }
        ps.print(")");
    }

    ps.println(") {");
  }

 
  
  static public void generateSchedulerIfCondition(KernelData kd, int sched, boolean fullTile,
      String prefix, PrintStream ps) {
    ps.print(prefix);
    // If condition
    ps.print("if (");
    ps.print("(");
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    ps.print(">=");
    ps.print(sched);
    if (fullTile) {
      ps.print("-");
      ps.print(-kd.getTileNbhForSuccessors().getFirstIndex(1)/kd.getTileGrainForGraph().getNbElements(1));
    }
    ps.print(") &&");
    ps.print("(");
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    ps.print("<");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(1,ps);
    ps.print("+");
    ps.print(sched);
    if (fullTile) {
      ps.print("+");
      ps.print(kd.getTileNbhForSuccessors().getLastIndex(1)/kd.getTileGrainForGraph().getNbElements(1));
    }
    ps.print(")");

    // Ensure to stay in the limits of the dataset
    if (fullTile) {
      ps.print("&&");
      ps.print("(");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
      ps.print("+");
      CLGenVarNames.generateSchedulerLoopCounter(ps);
      ps.print("-");
      ps.print(sched);
      ps.print(">=0");
      ps.print(")");
      ps.print("&&");
      ps.print("(");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
      ps.print("+");
      CLGenVarNames.generateSchedulerLoopCounter(ps);
      ps.print("-");
      ps.print(sched);
      ps.print("<");
      CLGenVarNames.generateNbGrains(1,ps);
      ps.print(")");
    }

    ps.println(") {");
  }

  static public void generateTileGrainMultiply(KernelData kd, int dim, PrintStream ps) {
    int tileGrain=kd.getTileGrainForGraph().getNbElements(dim);
    if (tileGrain!=1) {
      ps.print("*");
      ps.print(tileGrain);
    }
  }


  
}
