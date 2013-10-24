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

import ir.base.DataEdge;
import ir.base.FunctionNode;
import ir.base.Kernel;
import ir.base.KernelData;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import utility.math.Arithmetic;

import driver.options.CodegenOptions;

public class CLGenKernel {

  public static void generateOpenCLC_MainKernelFunction(Kernel k, PrintStream ps) {
    // Compute function prototype
    ps.print("__kernel void "+k.getName()+"(");
    // Output first
    int n=0;
    for(KernelData kd:k.getComputationalOutputList()) {
      if (n++!=0) {
        ps.print(", ");
      }
      CLGenKernelData.generateKernelParamDeclaration(kd, ps);
    }
    // Then inputs (global)
    for(KernelData kd:k.getParameterList()) {
      if (n++!=0) {
        ps.print(", ");
      }
      CLGenKernelData.generateKernelParamDeclaration(kd, ps);
    }
    // Then local variables
    for(KernelData kd:k.getComputationalDataList()) {
      n+=CLGenKernelData.generateKernelLocalParamDeclaration(kd,ps, n);
    }
    for(KernelData kd:k.getControlParameterList()) {
      if (kd.shouldBeCached()) {
        n+=CLGenKernelData.generateKernelLocalParamDeclaration(kd,ps, n);
      }
    }

    // In external tile mode, we get the tile information as kernel input
    if (CodegenOptions.isTileKernelMode()) {
      int nbDims=2; // Now considering only X and Y
      for(int i=0;i<nbDims;i++) {
        ps.print(", int ");
        CLGenVarNames.generateKernelTileSize(i,ps);
        ps.print(", int ");
        CLGenVarNames.generateKernelTilePosition(i,ps);
      }
    }
    ps.print(")");

    //=============
    // Kernel body
    //=============
    ps.println(" {");

    // First ND-Range infos
    int nbDims=2;
    for(int i=0;i<nbDims;i++) {
      //CLGenVarNames.generateLocalInfoVarDeclaration(i,ps,"  ");
      CLGenVarNames.generateWGInfoVarDeclaration(i,ps,"  ");
    }

    // Compute WG slicing infos: size and position
    for(int i=0;i<nbDims;i++) {
      generateWGSliceGeneralInfos(k,i,ps,"  ");
    }
    for(int i=0;i<nbDims;i++) {
      generateWGMySliceGeneralInfos(k,i,ps,"  ");
    }


    // Special case for the last WG
    ps.print("  if ((");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(0,ps);
    ps.print(">=");
    CLGenVarNames.generateNbGrains(0,ps);
    ps.print(") || (");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
    ps.print(">=");
    CLGenVarNames.generateNbGrains(1,ps);
    ps.println(")) return;");
    ps.println();

    // Slice data info per data (dim 0 only)
    generateWGSliceDataInfos(k,0,ps,"  ");

    // My slice data info per data
    for(int i=0;i<nbDims;i++) {
      generateWGMySliceDataInfos(k,i,ps,"  ");
      if (i==0) {
        generateSkipDataInfos(k,i,ps,"  ");
      }
    }

    // Event and buffer slots declaration
    for(KernelData kd:k.getParameterList()) {
      if (kd.shouldBeCached()) {
        // All may not be iterative
        if (CodegenOptions.isImageKernelMode()) { 
          if (kd.isIterative()) {
            CLGenKernelData.generateKernelEventDeclarationInput(kd,ps, "  ");
            ps.println();
            CLGenKernelData.generateKernelLocalBufferDeclaration(kd,ps, "  ");
            ps.println();
          }
          else {
            CLGenKernelData.generateKernelLocalCachingNonIterative(kd,ps, "  ");
            ps.println();      
          }
        }
      }
    }

    // Declarations for local buffers management
    if (CodegenOptions.isImageKernelMode()) { 
      for(KernelData kd:k.getFunctionNodeList()) {
        if (kd.isKernelOutputData()) {
          CLGenKernelData.generateKernelEventDeclarationOutput(kd,ps, "  ");
          ps.println();
        }
        CLGenKernelData.generateKernelLocalBufferDeclaration(kd,ps, "  ");
        ps.println();
      }
    }

    // Position global pointers to the slice
    generateWGSliceInOutPositioning(k,ps,"  ");

    //==============================================================
    // TODO: consider now that all inputs have coherent formats
    // (same specifier or size)
    // so take the first input. To be generic, we should split the
    // algo graph in a set of distinct subgraphs and process each
    // each subgraph independently. It will ensure input coherency
    //==============================================================

    // Compute the number of iterations
    // For this, pick an iterative input and look at a user (pop*rate)
    KernelData refInput=k.getComputationalInputList().get(0);
    DataEdge refEdge=refInput.getUserEdge(0);
    FunctionNode refUser=refEdge.getTargetFunctionNode();
    int refRate=refEdge.getPop()*refUser.getRate();

    // === Main loop ===
    int maxSched=0;
    for(KernelData kd:k.getComputationalDataList()) {
      maxSched=Arithmetic.max(maxSched,kd.getCodegenDataPattern().getSchedulingCycle());
    }
    // TODO: change the minsched initialization
    int minSched=1000;
    for(KernelData kd:k.getFunctionNodeList()) {
      minSched=Arithmetic.min(minSched,kd.getCodegenDataPattern().getSchedulingCycle());
    }

    int maxTop=0;
    for(KernelData kd:k.getComputationalDataList()) {
      maxTop=Arithmetic.max(maxTop,-kd.getTileNbhForSuccessors().getFirstIndex(1)/kd.getTileGrainForGraph().getNbElements(1));
    }
    int maxBottom=0;
    for(KernelData kd:k.getComputationalDataList()) {
      maxBottom=Arithmetic.max(maxBottom,kd.getTileNbhForSuccessors().getLastIndex(1)/kd.getTileGrainForGraph().getNbElements(1));
    }

    if (CodegenOptions.isTileKernelMode()) {
      // Position tile input/output pointers
      for(KernelData kd:k.getComputationalInputList()) {
        // sched-1 since no double buffering in tile mode
        CLGenKernelData.generateTilePositioning(kd,"  ",minSched-1, ps);
      }
      // TODO: should position tile output pointers also when it will be supported (ex: Integral image)
    }

    ps.print("  for(int ");
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    ps.print("= 0");
    ps.print("-");
    ps.print(maxTop);
    ps.print("; ");
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    ps.print(" < ");
    if (CodegenOptions.isTileKernelMode()) {
      //** Tile mode **
      CLGenVarNames.generateKernelTileSize(1,ps);
      if (refRate!=1) {
        ps.print("/");
        ps.print(refRate);
      }
      ps.print("+");
      ps.print(maxSched-minSched);    
      ps.print("+");
      ps.print(maxBottom);    
    }
    else {
      //** Image mode **
      CLGenVarNames.generateMyWGSliceSizeGrainUnit(1,ps);
      ps.print("+");
      ps.print(maxSched+1);
      ps.print("+");
      ps.print(maxBottom);    
    }

    ps.print("; ");
    CLGenVarNames.generateSchedulerLoopCounter(ps);
    ps.println("++ ) {");
    // Schedule input and outputs
    if (CodegenOptions.isTileKernelMode()) {
      //** Tile mode **

      // Schedule algos
      for(FunctionNode fn:k.getFunctionNodeList()) {
        CLGenKernel.generateTileComputeCall(fn,"    ",minSched,
            ps);
      }
      // Increment input and output pointers
      for(KernelData kd:k.getComputationalInputList()) {
        CLGenKernelData.generateTileIncrement(kd,"    ", ps);
      }
    }
    else {
      //** Image mode **

      // Schedule input copies
      for(KernelData kd:k.getComputationalInputList()) {
        CLGenKernelData.generateInputCopy(kd,"    ",ps);
      }
      // Schedule output copies
      for(KernelData kd:k.getComputationalOutputList()) {
        CLGenKernelData.generateOutputCopy(kd,"    ",ps);
      }

      // Schedule computation
      for(FunctionNode fn:k.getFunctionNodeList()) {
        CLGenKernel.generateImageComputeCall(fn,"    ",ps);
      }

      // Global synchro needed in asynchronous merge mode
      if (CodegenOptions.isAsyncMergeMode()) {
        ps.println("    barrier(CLK_LOCAL_MEM_FENCE);");
      }

      // Schedule wait for input
      for(KernelData kd:k.getComputationalInputList()) {
        CLGenKernelData.generateInputWait(kd,"    ",ps);
      }
      // Schedule wait for outputs
      for(KernelData kd:k.getComputationalOutputList()) {
        CLGenKernelData.generateOutputWait(kd,"    ",ps);
      }
    }

    // === end of Main loop ===
    ps.println("  }");

    // === end of kernel ====
    ps.println("}");
  }


  //===========================================================================
  // TO SORT ...
  //===========================================================================

  static void generateWGSliceGeneralInfos(Kernel k, int dim, PrintStream ps, String prefix ) {
    // Pick an iterative input as reference
    KernelData kdRef=k.getComputationalInputList().get(0);

    // Compute the nb of grains in the slice
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateNbGrains(dim,ps);
    ps.print("= ");
    int tileGrain=kdRef.getTileGrainForGraph().getNbElements(dim);
    if (tileGrain!=1) {
      ps.print("((");
      kdRef.getMatrixType().generateLastIndexPlusOne(dim,ps);
      ps.print(")+");
      ps.print(tileGrain);
      ps.print("-1)/");
      ps.print(tileGrain);
    }
    else {
      kdRef.getMatrixType().generateLastIndexPlusOne(dim,ps);
    }
    ps.println(";");

    // Compute the common slice size (unit = grain)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.print("= (");
    CLGenVarNames.generateNbGrains(dim,ps);
    ps.print("+");
    ps.print(CLGenVarNames.getNbWGVarName(dim));
    ps.print("-1)/");
    ps.print(CLGenVarNames.getNbWGVarName(dim));
    ps.println( ";");
  }

  static void generateWGMySliceGeneralInfos(Kernel k, int dim, PrintStream ps, String prefix ) {
    // Compute my own tile position (unit = grain)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(dim,ps);
    ps.print("= ");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.print("*");
    ps.print(CLGenVarNames.getGroupIdVarName(dim));
    ps.println( ";");

    // Compute my own tile size (unit = grain)
    ps.print(prefix);
    ps.print("int ");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(dim,ps);
    ps.print("= ");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.print("*(");
    ps.print(CLGenVarNames.getGroupIdVarName(dim));
    ps.print("+1) >");
    CLGenVarNames.generateNbGrains(dim,ps);
    ps.print("?");
    CLGenVarNames.generateNbGrains(dim,ps);
    ps.print("-");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.print("*");
    ps.print(CLGenVarNames.getGroupIdVarName(dim));
    ps.print(":");
    CLGenVarNames.generateWGSliceSizeGrainUnit(dim,ps);
    ps.println(";");
    ps.println();
  }

  static void generateWGSliceDataInfos(Kernel k, int dim, PrintStream ps, String prefix ) {
    for(KernelData kd:k.getComputationalDataList()) {
      CLGenKernelData.generateWGFullSliceSizePixInfos(kd, dim, ps, prefix );
      ps.println();
    }
  }

  static void generateWGMySliceDataInfos(Kernel k, int dim, PrintStream ps, String prefix) {
    // For the input, my full slice information
    for(KernelData kd:k.getComputationalInputList()) {
      // Left and right grain extension
      CLGenKernelData.generateMyWGSliceLeftExtentGrainInfos(kd,dim,ps,prefix);
      if (dim==0) {
        CLGenKernelData.generateMyWGSliceRightExtentGrainInfos(kd,dim,ps,prefix);
        CLGenKernelData.generateMyWGSliceLeftExtentPixInfos(kd,dim,ps,prefix);
      }

      CLGenKernelData.generateMyWGFullSlicePositionPixInfos(kd,dim,ps,prefix);
      if (dim==0) {
        CLGenKernelData.generateMyWGFullSliceSizePixInfos(kd,dim,ps,prefix);
      }
      ps.println();
    }

    // Function nodes
    for(FunctionNode fn:k.getFunctionNodeList()) {  
      if (dim==0) {
        CLGenKernelData.generateMyWGSliceLeftExtentGrainInfos(fn,dim,ps,prefix);
        CLGenKernelData.generateMyWGSliceRightExtentGrainInfos(fn,dim,ps,prefix);
        //CLGenKernelData.generateMyWGSliceLeftExtentPixInfos(fn,dim,ps,prefix);
        ps.println();  
      }
    }

    // For the outputs, my slice information
    for(KernelData kd:k.getComputationalOutputList()) {
      CLGenKernelData.generateMyWGSlicePositionPixInfos(kd,dim,ps,prefix);
      if (dim==0) {
        CLGenKernelData.generateMyWGSliceSizePixInfos(kd,dim,ps,prefix);
      }
      ps.println();
    }
  }

  static void generateSkipDataInfos(Kernel k, int dim, PrintStream ps, String prefix ) {
    // For the outputs, my slice information
    for(FunctionNode fn:k.getFunctionNodeList()) {
      if (fn.isKernelOutputData() && fn.isSkipOrUndef()) {
        if (fn.getOutputSkipPattern().getFirstIndex(0)!=0) {
          fn.generateSkipBeginVarDeclarationPix(dim,ps,prefix);
        }
        if (fn.getOutputSkipPattern().getLastIndex(0)!=0) {
          fn.generateSkipEndVarDeclarationPix(dim,ps,prefix);
        }
      }
    }
  }

  static void generateWGSliceInOutPositioning(Kernel k, PrintStream ps, String prefix) {
    // Position inputs
    for(KernelData kd:k.getComputationalInputList()) {
      CLGenKernelData.generateWGSliceInPositioning(kd,ps,prefix);
    }
    // Position outputs
    for(KernelData kd:k.getComputationalOutputList()) {
      CLGenKernelData.generateWGSliceOutPositioning(kd,ps,prefix);
    }
    ps.println();
  }

  static public void generateImageComputeCall(FunctionNode fn, 
      String prefix,
      PrintStream ps) {
    int sched=fn.getCodegenDataPattern().getSchedulingCycle();

    // If condition
    CLGenKernelData.generateSchedulerIfCondition(fn,sched,true,prefix, ps);

    for(int i=0;i<fn.getRate();i++) {  
      // Function call
      ps.print(prefix+"  ");
      CLGenVarNames.generateComputeFunctionName(fn, ps);
      ps.print("(");

      // 1- Output first
      CLGenKernelData.generateKernelCircLocalAccess(fn,sched,fn.getRate(),fn.getPush(),i, ps);
      // 2- Inputs
      for(int j=0;j<fn.getNbInputData();j++) {
        KernelData kd=fn.getInputData(j);
        ps.print(", ");
        if (kd.isIterative()) {
          // Iterative data
          // TODO: Not adapted to the tiling (rate+pop should be replaced by the tile grain)
          CLGenKernelData.generateKernelCircLocalAccess(kd,sched,fn.getRate(),fn.getInputEdge(kd).getPop(),i, ps);
        }
        else {
          // Control data   
          if (kd.isNonIterativeDataToCache()){
            CLGenVarNames.generateKernelLocalName(kd, ps);
          }
          else {
            ps.print(kd.getName());
          }
        }
      }
      // 3- All implicit control data (size of iterative input)
      LinkedHashSet<KernelData> controlParamSet=new LinkedHashSet<KernelData>();
      fn.getImplicitControlParamList(controlParamSet);
      for(KernelData kd:controlParamSet) {
        ps.print(", ");
        ps.print(kd.getName());
      }

      // 4- Slice information (dim 0)
      ps.print(",");
      CLGenVarNames.generateMyWGSliceSizeGrainUnit(0,ps);
      ps.print(",");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(0,ps);
      ps.print(", ");
      CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(fn,0,ps);
      ps.print(", ");
      CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(fn,0,ps);

      // 5- Line information (dim 1)
      ps.print(",");
      ps.print("(");
      CLGenVarNames.generateMyWGSlicePositionGrainUnit(1,ps);
      ps.print("+");
      CLGenVarNames.generateSchedulerLoopCounter(ps);
      ps.print(" - ");
      ps.print(sched);
      ps.print(")");
      CLGenKernelData.generateTileGrainMultiply(fn,1,ps);
      if (i!=0) {
        ps.print(" + "+i);
        if (fn.getOutputStridePattern().getNbElements(1)!=1) {
          ps.print("*");
          ps.print(fn.getOutputStridePattern().getNbElements(1));
        }
      }

      ps.println(");");
    }

    // Node synchro needed only in synchronous merge mode
    if (CodegenOptions.isSyncMergeMode()) {
      ps.println(prefix+"  barrier(CLK_LOCAL_MEM_FENCE);");
    }

    // Close the if
    ps.print(prefix);
    ps.println("}");
  }

  static public void generateImageComputeFunctionPrototype(FunctionNode fn, PrintStream ps) {
    // Compute function prototype
    ps.print("void ");
    CLGenVarNames.generateComputeFunctionName(fn, ps);
    ps.print("(");
    // 1- Output first
    CLGenKernelData.generateComputeFunctionParamDeclaration(fn,fn.getWritePattern(), ps);
    // 2- Inputs
    for(KernelData kd:fn.getInputDataList()) {
      ps.print(", ");
      if (kd.isIterative()) {
        CLGenKernelData.generateComputeFunctionParamDeclaration(kd, fn.getInputEdge(kd).getReadPattern(), ps);
      }
      else {
        CLGenKernelData.generateComputeFunctionParamDeclaration(kd, null, ps);
      }
    }
    // 3- All implicit control data (size of iterative input)
    LinkedHashSet<KernelData> controlParamSet=new LinkedHashSet<KernelData>();
    fn.getImplicitControlParamList(controlParamSet);
    for(KernelData kd:controlParamSet) {
      ps.print(", ");
      CLGenKernelData.generateComputeFunctionParamDeclaration(kd,null, ps);   
    }

    // 4- Slice information in dim 0
    ps.print(", int ");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(0,ps);
    ps.print(", int ");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(0,ps);
    ps.print(", int ");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(fn,0,ps);
    ps.print(", int ");
    CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(fn,0,ps);

    // 5- Line information in dim 1
    ps.print(", int ");
    CLGenVarNames.generatePositionPixUnit(fn,1,ps);
    ps.print(")");
  }

  static public void generateImageComputeFunction(FunctionNode fn, PrintStream ps) {
    // Function prototype
    generateImageComputeFunctionPrototype(fn,ps);

    // Function body
    ps.println("{");
    // Unit translation
    ps.print("  int ");
    CLGenVarNames.generateMyWGSliceSizeBlockUnit(fn,0,ps);
    ps.print("=");
    CLGenVarNames.generateMyWGSliceSizeGrainUnit(0,ps);
    if (fn.nbBlocksPerTileGrainForGraph.getNbElements(0)!=1) {
      ps.print("*");
      ps.print(fn.nbBlocksPerTileGrainForGraph.getNbElements(0));
    }
    ps.println(";");

    ps.print("  int ");
    CLGenVarNames.generateMyWGSlicePositionBlockUnit(fn,0,ps);
    ps.print("=");
    CLGenVarNames.generateMyWGSlicePositionGrainUnit(0,ps);
    if (fn.nbBlocksPerTileGrainForGraph.getNbElements(0)!=1) {
      ps.print("*");
      ps.print(fn.nbBlocksPerTileGrainForGraph.getNbElements(0));
    }
    ps.println(";");

    // Unit translation
    ps.print("  int ");
    CLGenVarNames.generateMyWGSliceLeftExtentBlockUnit(fn,0,ps);
    ps.print("=");
    CLGenVarNames.generateMyWGSliceLeftExtentGrainUnit(fn,0,ps);
    if (fn.nbBlocksPerTileGrainForGraph.getNbElements(0)!=1) {
      ps.print("*");
      ps.print(fn.nbBlocksPerTileGrainForGraph.getNbElements(0));
    }
    ps.println(";");

    ps.print("  int ");
    CLGenVarNames.generateMyWGSliceRightExtentBlockUnit(fn,0,ps);
    ps.print("=");
    CLGenVarNames.generateMyWGSliceRightExtentGrainUnit(fn,0,ps);
    if (fn.nbBlocksPerTileGrainForGraph.getNbElements(0)!=1) {
      ps.print("*");
      ps.print(fn.nbBlocksPerTileGrainForGraph.getNbElements(0));
    }
    ps.println(";");
    ps.println();

    fn.generateImageComputeFunctionBody(ps);
    ps.println("}");
    ps.println();
  }
  
  
  
  
  //===========================================================================
  // Kernel tile version: to rework
  //===========================================================================

  static public void generateTileComputeCall(FunctionNode fn, String prefix, 
      int minSched,
      PrintStream ps) {
    int sched=fn.getCodegenDataPattern().getSchedulingCycle()-minSched;

    // If condition
    CLGenKernelData.generateSchedulerIfCondition(fn,sched,true, prefix, ps);

    for(int i=0;i<fn.getRate();i++) {  

      // Function call
      ps.print(prefix+"  ");
      CLGenVarNames.generateComputeFunctionName(fn, ps);
      ps.print("(");

      // 1- Output first
      if (fn.isKernelOutputData()) {
        // Direct pointer
        CLGenVarNames.generateKernelTilePtrName(fn, ps);
      }
      else {
        // Circular buffer
        CLGenKernelData.generateKernelCircLocalAccess(fn,sched,fn.getRate(),fn.getPush(),i, ps);
      }
      // 2- Inputs
      for(int j=0;j<fn.getNbInputData();j++) {
        KernelData kd=fn.getInputData(j);
        ps.print(", ");
        if (kd.isKernelInputData()) {
          // Direct pointer
          CLGenVarNames.generateKernelTilePtrName(kd, ps);
        }
        else {
          CLGenKernelData.generateKernelCircLocalAccess(kd,sched,fn.getRate(),fn.getInputEdge(kd).getPop(),i, ps);
        }
      }
      // 3- All implicit control data (size of iterative input)
      LinkedHashSet<KernelData> controlParamSet=new LinkedHashSet<KernelData>();
      fn.getImplicitControlParamList(controlParamSet);
      for(KernelData kd:controlParamSet) {
        ps.print(", ");
        ps.print(kd.getName());
      }
      // 4- Position information
      if (fn.needPosition()) {
        ps.print(", ");
        CLGenVarNames.generateKernelTilePosition(1,ps);
        ps.print(" + ");
        CLGenVarNames.generateSchedulerLoopCounter(ps);
        ps.print(" - ");
        ps.print(sched);
      }
      ps.println(");");

      // Increment tile pointers for outputs
      if  (fn.isKernelOutputData()) {
        CLGenKernelData.generateTileIncrement(fn,prefix+"  ", ps);
      }
    }

    // Close the if
    ps.print(prefix);
    ps.println("}");
  }

}
