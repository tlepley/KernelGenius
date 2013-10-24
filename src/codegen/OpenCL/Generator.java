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

import ir.base.FunctionNode;
import ir.base.Kernel;
import ir.base.Program;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;

import parser.KernelGeniusEmitter;
import parser.TNode;

import common.CompilerError;
import common.ResourceManager;

import codegen.CodeGenerator;
import driver.DriverHelper;
import driver.options.CodegenOptions;
import driver.options.GeneralOptions;

public class Generator extends CodeGenerator {
  
  //=================================================
  // Main entry point
  //=================================================
  
  public void generate(Program prog, List<File> generatedFiles, File tempDir) {
    // The OpenCL-C code
    generateOpenCLC(prog,generatedFiles,tempDir);
    // The C (host) code
    CLGenHostWrapper.generateHostWrapper(prog,generatedFiles,tempDir);
  }

  //=================================================
  // OpenCL-C program
  //=================================================
  
  public static void generateOpenCLC(Program prog, List<File> generatedFiles, File tempDir) {
    // Debug messages
    File fileToGenerate = DriverHelper.makeOutputFile(prog.getName()+".cl", tempDir);
    generatedFiles.add(fileToGenerate);
    
    if (GeneralOptions.getDebugLevel() > 0) {
      CompilerError.GLOBAL.raiseMessage("  ... generating file '"
          + fileToGenerate.getName() + "'");
    }
    PrintStream ps=null;
    try {
      ps = new PrintStream(fileToGenerate);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ResourceManager.registerStream(ps);
    // Generate the program
    Generator.generateOpenCLC_Program(prog,ps);
    // Close the file
    try {
      ResourceManager.closeStream(ps);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  static public void generateOpenCLC_Program(Program prog, PrintStream ps) {
    ps.println("#line 1 \"KernelGenius generated code\"");
    ps.println("/*");
    ps.println("   File generated automatically, do not modify");
    ps.println("*/");
    ps.println();

    // Iterates over statements
    for(Object o:prog.getStatementList()) {
      if (o instanceof String) {
        // Native section
        ps.println((String)o);
      }
      else if (o instanceof Kernel) {
        generateOpenCLC_Kernel(((Kernel)o), ps);
        ps.println();
      }
      else if (o instanceof TNode) {
        // This is a declaration
        ps.println("// User data type statement");
        TNode tn=(TNode)o;
        CommonTreeNodeStream nodes = new CommonTreeNodeStream((Tree)tn);
        KernelGeniusEmitter emitter=new KernelGeniusEmitter(nodes);
        emitter.setNoLineManagement();
        emitter.setPrintStream(ps);
        try {
          emitter.declaration();
        } catch (RecognitionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        ps.println();
        ps.println();
      }
      else {
        CompilerError.GLOBAL.raiseFatalError("generateOpenCLC_Program : unknown object type");
      }
    }
  }

  public static void generateOpenCLC_Kernel(Kernel k, PrintStream ps) {
    // ----------------------------------
    // Generation of the kernel stub file
    // ----------------------------------   

    // Generate constant literals
    for(int i=0;i<k.getNbFunctionNodes();i++) {
      FunctionNode a=k.getFunctionNode(i);
      a.generateConstLiterals(ps);
    }

    // Generate runtime functions for algos
    for(int i=0;i<k.getNbFunctionNodes();i++) {
      FunctionNode a=k.getFunctionNode(i);
      a.generateRuntimeFunctions(ps);
    }

    // Generate compute functions for each algorithm
    for(int i=0;i<k.getNbFunctionNodes();i++) {
      FunctionNode fn=k.getFunctionNode(i);
      CLGenKernel.generateImageComputeFunction(fn, ps);
    }

    // Generate kernels main function
    CLGenKernel.generateOpenCLC_MainKernelFunction(k, ps);
  }
  
  
  //===========================================================================
  // Code generation report
  //===========================================================================

  public void generateReport(Program prog, PrintStream ps) {
    if (CodegenOptions.getKernelGranularityMode()==CodegenOptions.KERNEL_GRANULARITY_MODE.IMAGE) {       
      ps.print("Report for program '");
      ps.print(prog.getName());
      ps.println("'");
      for(int i=0;i<"Report for program ''".length()+prog.getName().length();i++) {
        ps.print("-");
      }
      ps.println();
      for(Kernel k:prog.getKernelList()) {
        k.generateReportImageMode(ps);
      }
      ps.println();
    }
    else {
      ps.print("No report for "+CodegenOptions.getKernelGranularityMode()+" mode");
    }
  }

}
