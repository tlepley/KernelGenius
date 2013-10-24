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

import ir.base.Kernel;
import ir.base.KernelData;
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

import driver.DriverHelper;
import driver.options.GeneralOptions;

public class CLGenHostWrapper {

  public static void generateHostWrapper(Program prog, List<File> generatedFiles, File tempDir) {
    //==> .c wrapper file
    File fileToGenerate = DriverHelper.makeOutputFile(prog.getName()+".c", tempDir);
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
    CLGenHostWrapper.generateHostWrapperC(prog,ps);
    // Close the file
    try {
      ResourceManager.closeStream(ps);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  
    //==> .h wrapper file
    fileToGenerate = DriverHelper.makeOutputFile(prog.getName()+".h", tempDir);
    generatedFiles.add(fileToGenerate);
  
    if (GeneralOptions.getDebugLevel() > 0) {
      CompilerError.GLOBAL.raiseMessage("  ... generating file '"
          + fileToGenerate.getName() + "'");
    }
    ps=null;
    try {
      ps = new PrintStream(fileToGenerate);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    ResourceManager.registerStream(ps);
    // Generate the program
    CLGenHostWrapper.generateHostWrapperH(prog,ps);
    // Close the file
    try {
      ResourceManager.closeStream(ps);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  static public void generateHostWrapperC(Program prog, PrintStream ps) {
    ps.println("// ====================================================================");
    ps.println("//");
    ps.print(  "// Low level host interface for the '");
    ps.print( prog.getName());
    ps.println("' OpenCL program");
    ps.println("//");
    ps.println("// Generated Automatically by the KernelGenius compiler");
    ps.println("//");
    ps.println("// ====================================================================");
    ps.println();
    ps.println("#include <CL/cl.h>");
    ps.println("#include \"oclUtil.h\"");
    
    ps.println();
    // Program init functions
    CLGenHostWrapper.generateHostWrapperCProgramInit(prog,ps);
    ps.println();
  
    // Generate types defined by the user
    ps.println("// User data types");
    for(Object o:prog.getStatementList()) {
      if (o instanceof TNode) {
        TNode tn=(TNode)o;
        // This is a declaration
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
      }
    }    
    ps.println();
  
    // Kernel functions
    for(Kernel k:prog.getKernelList()) {
      ps.println();
      CLGenHostWrapper.generateHostWrapperC(k, ps);
      ps.println();
    }
  }

  public static void generateHostWrapperCProgramInit(Program prog, PrintStream ps) {
    // Binary version
    ps.println("/**");
    ps.println(" * Read the binary program file, create an OpenCL program object,");
    ps.println(" * build it and check if everything is fine.");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("cl_program create");
    ps.print(prog.getName());
    ps.println("ProgramFromBinary(cl_context context, cl_device_id device) {");
    ps.println("  char path[256];");
    ps.print("  return oclCreateProgramFromBinary(context,device,oclGetProgramPath(path, \"");
    ps.print(prog.getName());
    ps.println(".so\", 256));");
    ps.println("}");
  
    // Source  version
    ps.println("/**");
    ps.println(" * Read the source  program file, create an OpenCL program object,");
    ps.println(" * build it and check if everything is fine.");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("cl_program create");
    ps.print(prog.getName());
    ps.println("ProgramFromSource(cl_context context, cl_device_id device, char *options) {");
    ps.println("  char path[256];");
    ps.print("  return oclCreateProgramFromSource(context,device,oclGetProgramPath(path, \"");
    ps.print(prog.getName());
    ps.println(".cl\", 256), options);");
    ps.println("}");
  }

  static public void generateHostWrapperH(Program prog, PrintStream ps) {
    ps.println("// ====================================================================");
    ps.println("//");
    ps.print(  "// Low level host interface for the '");
    ps.print( prog.getName());
    ps.println("' OpenCL program");
    ps.println("//");
    ps.println("// Generated Automatically by the KernelGenius compiler");
    ps.println("//");
    ps.println("// ====================================================================");
    ps.println();
    ps.println("#include <CL/cl.h>");
    ps.println("#include \"oclUtil.h\"");
    ps.println();
  
    ps.println();
    ps.print("#ifndef ");
    ps.print(prog.getName().toUpperCase());
    ps.println("_KERNELS_H");
    ps.print("#define ");
    ps.print(prog.getName().toUpperCase());
    ps.println("_KERNELS_H");
    ps.println();
  
    // Program init functions
    CLGenHostWrapper.generateHostWrapperHProgramInit(prog,ps);
    
    // No types generated by the user here in order not to colide with
    // Application includes that may redefine these types and include
    // this .h file
    
    // Kernel functions
    for(Kernel k:prog.getKernelList()) {
      ps.println();
      CLGenHostWrapper.generateHostWrapperH(k, ps);
      ps.println();
    }
  
    ps.println();
    ps.print("#endif"); 
  }

  public static void generateHostWrapperHProgramInit(Program prog, PrintStream ps) {
    // Binary version
    ps.println("/**");
    ps.println(" * Read the binary program file, create an OpenCL program object,");
    ps.println(" * build it and check if everything is fine.");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("cl_program create");
    ps.print(prog.getName());
    ps.println("ProgramFromBinary(cl_context context, cl_device_id device);");
  
    // Source  version
    ps.println("/**");
    ps.println(" * Read the source  program file, create an OpenCL program object,");
    ps.println(" * build it and check if everything is fine.");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("cl_program create");
    ps.print(prog.getName());
    ps.println("ProgramFromSource(cl_context context, cl_device_id device, char *options);");
  }

  public static void generateHostWrapperC(Kernel k, PrintStream ps) {
    ps.println("// #########################################################");
    ps.print(  "// Kernel '");
    ps.println(k.getName());
    ps.println("// #########################################################");
    ps.println();
    
    CLGenHostWrapper.generateHostWrapperC_CreateKernel(k, ps);
    ps.println();
    CLGenHostWrapper.generateHostWrapperC_setKernelArgs(k, ps);
    ps.println();
    CLGenHostWrapper.generateHostWrapperC_run(k, ps);
  }

  public static void generateHostWrapperC_CreateKernel(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.print(" * Creates a '");
    ps.print(k.getName());
    ps.println("' kernel.");
    ps.println(" * This function exits in case of error.");
    ps.println("*/");
    ps.print("cl_kernel createKernel_");
    ps.print(k.getName());
    ps.println("(cl_program program) {");
    ps.println("  cl_int status;");
    ps.print(  "  cl_kernel kernel = clCreateKernel(program, \"");
    ps.print(k.getName());
    ps.println("\", &status);");
    ps.print(  "  oclCheckStatus(status,\"clCreateKernel (");
    ps.print(k.getName());
    ps.println(") failed.\");");
    ps.println("  return kernel;");
    ps.println("}");
  }

  public static void generateHostWrapperC_setKernelArgs(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.println(" * Sets parameters to a kernel object");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("void setKernelArgs_");
    ps.print(k.getName());
    ps.print("(cl_kernel kernel");
  
    ps.println(", int "+CLGenVarNames.getNbWGVarName(0)+
        ", int "+CLGenVarNames.getNbWGVarName(1)+
        ", int "+CLGenVarNames.getLocalSizeVarName(0));

    // ============= Parameters ==============
    // Output first
    int n=0;
    for(KernelData kd:k.getComputationalOutputList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }

    ps.println(") {");
    ps.println("cl_int status;");
    
    // Compute WG slicing infos (dim 0 only)
    CLGenKernel.generateWGSliceGeneralInfos(k,0,ps,"  ");
    CLGenKernel.generateWGSliceDataInfos(k,0,ps,"  ");

    // ============= Set kernel parameters ==============
    // Output first
    n=0;
    for(KernelData kd:k.getComputationalOutputList()) {
      CLGenHostWrapper.generateKernelWrapperParamSetting(kd,n,"  ", ps);
      n++;
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      CLGenHostWrapper.generateKernelWrapperParamSetting(kd,n,"  ", ps);
      n++;
    }
    // Then local variables
    for(KernelData kd:k.getComputationalDataList()) {
      CLGenHostWrapper.generateKernelWrapperLocalParamSetting(kd,n,"  ", ps);
      n++;
    }
    for(KernelData kd:k.getControlParameterList()) {
      if (kd.shouldBeCached()) {
        CLGenHostWrapper.generateKernelWrapperLocalParamSetting(kd,n,"  ", ps);
        n++;
      }
    }
    
    ps.println("}"); 
  }

 
  public static void generateHostWrapperC_run(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.println(" * Sets parameters to a kernel object");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("void run_");
    ps.print(k.getName());
    ps.println("(cl_command_queue commandQueue, cl_program program,");
    
    ps.println("  int "+CLGenVarNames.getNbWGVarName(0)+
        ", int "+CLGenVarNames.getNbWGVarName(1)+
        ", int "+CLGenVarNames.getLocalSizeVarName(0));
    
    // ============= Parameters ==============
    // Output first
    for(KernelData kd:k.getComputationalOutputList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    ps.println(") {");
    ps.println("  cl_int status;");
    
    // Kernel
    ps.print("  cl_kernel kernel=createKernel_");
    ps.print(k.getName());
    ps.println("(program);");
  
    // Set arguments
    ps.print("  setKernelArgs_");
    ps.print(k.getName());
    ps.print("(kernel");
    ps.println(","+CLGenVarNames.getNbWGVarName(0));
    ps.println(","+CLGenVarNames.getNbWGVarName(1));
    ps.println(","+CLGenVarNames.getLocalSizeVarName(0));
    // Output first
    for(KernelData kd:k.getComputationalOutputList()) {
      ps.print(", ");
      ps.print(kd.getName());
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      ps.print(", ");
      ps.print(kd.getName());
    }
    ps.println(");");
  
    // Call
    ps.println("  size_t globalThreads[2] = {"+
    CLGenVarNames.getLocalSizeVarName(0)+"*"+CLGenVarNames.getNbWGVarName(0)+
    ","+CLGenVarNames.getNbWGVarName(1)+"};");
    ps.println("  size_t localThreads[2]  = {"+CLGenVarNames.getLocalSizeVarName(0)+", 1};");
    ps.println("  cl_event event;");
    ps.println("  status = clEnqueueNDRangeKernel(");
  
    ps.println("    commandQueue,");
    ps.println("    kernel,");
    ps.println("    2,NULL,globalThreads,localThreads,");
    ps.println("    0,NULL, &event);");
    ps.print(  "  oclCheckStatus(status,\"clEnqueueNDRangeKernel (");
    ps.print(k.getName());
    ps.println(") failed.\");");    
  
    ps.println("  status = clReleaseKernel(kernel);");
    ps.print(  "  oclCheckStatus(status,\"clReleaseKernel (");
    ps.print(k.getName());
    ps.println(") failed.\");");    
  
    ps.println("  status = clWaitForEvents(1, &event);");
    ps.print(  "  oclCheckStatus(status,\"clWaitForEvent (");
    ps.print(k.getName());
    ps.println(") failed.\");");    
  
    ps.println("  clReleaseEvent(event);");
    ps.println("}");
  }

  public static void generateHostWrapperH(Kernel k, PrintStream ps) {
    ps.println("// #########################################################");
    ps.print(  "// Kernel '");
    ps.println(k.getName());
    ps.println("// #########################################################");
    ps.println();
    
    CLGenHostWrapper.generateHostWrapperH_CreateKernel(k, ps);
    ps.println();
    CLGenHostWrapper.generateHostWrapperH_setKernelArgs(k, ps);
    ps.println();
    CLGenHostWrapper.generateHostWrapperH_run(k, ps);
  }

  public static void generateHostWrapperH_CreateKernel(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.print(" * Creates a '");
    ps.print(k.getName());
    ps.println("' kernel.");
    ps.println(" * This function exits in case of error.");
    ps.println("*/");
    ps.print("cl_kernel createKernel_");
    ps.print(k.getName());
    ps.println("(cl_program program);");
  }

  public static void generateHostWrapperH_setKernelArgs(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.println(" * Sets parameters to a kernel object");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("void setKernelArgs_");
    ps.print(k.getName());
    ps.print("(cl_kernel kernel");

    ps.println(",  int "+CLGenVarNames.getNbWGVarName(0)+
        ", int "+CLGenVarNames.getNbWGVarName(1)+
        ", int "+CLGenVarNames.getLocalSizeVarName(0));

    // ============= Parameters ==============
    // Output first
    for(KernelData kd:k.getComputationalOutputList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    ps.println(");");
  }

  public static void generateHostWrapperH_run(Kernel k, PrintStream ps) {
    ps.println("/*");
    ps.println(" * Sets parameters to a kernel object");
    ps.println(" * This function exists in case of error.");
    ps.println("*/");
    ps.print("void run_");
    ps.print(k.getName());
    ps.println("(cl_command_queue commandQueue, cl_program program");     
    ps.println(", int "+CLGenVarNames.getNbWGVarName(0)+
        ", int "+CLGenVarNames.getNbWGVarName(1)+
        ", int "+CLGenVarNames.getLocalSizeVarName(0));
    ps.print("  ");
 
    // ============= Parameters ==============
    // Output first
    for(KernelData kd:k.getComputationalOutputList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    // Then inputs
    for(KernelData kd:k.getParameterList()) {
      ps.print(", ");
      CLGenHostWrapper.generateKernelWrapperParamDeclaration(kd, ps);
    }
    ps.println(");");
  }

  static public void generateKernelWrapperLocalParamSetting(KernelData kd, int n, String prefix, PrintStream ps) {
    ps.print(prefix);
    ps.print("status = clSetKernelArg(kernel,");
    ps.print(n);
    ps.print(",");
  
    if (kd.isIterative()) {
      int slot=kd.getCodegenDataPattern().getNbBufferSlot();
      // Size in bytes
      ps.print("sizeof(");
      CLGenKernelData.generateBaseCType(kd,ps);
      ps.print(")");
      ps.print("*");
      if (slot!=1) {
        ps.print(slot);
        ps.print("*");
      }
      CLGenVarNames.generateFullSliceSizePixUnit(kd,0,ps);
      //kd.getMatrixType().generateLastIndexPlusOne(0,ps);
    }
    else {
      ps.print(prefix);
      kd.getMatrixType().generateSizeInBytes(ps, kd.getBaseCTypeNode());
    }
  
    // NULL pointer since local buffer
    ps.println(",(void *)NULL);");
    // Check
    ps.print(prefix);
    ps.print("oclCheckStatus(status,\"clSetKernelArg (local ");
    ps.print(kd.getName());
    ps.println(") failed.\");");
  }

  static public void generateKernelWrapperParamSetting(KernelData kd, int n, String prefix, PrintStream ps) {
    ps.print(prefix);
    ps.print("status = clSetKernelArg(kernel,");
    ps.print(n);
    ps.print(",");
  
    if (kd.shouldBeCached()) {
      // It will be passed as a buffer
      ps.print("sizeof(cl_mem)");
    }
    else {
      // It will be passed by value
      ps.print("sizeof(");
      CLGenKernelData.generateBaseCType(kd,ps);
      ps.print(")");
    }
  
    ps.print(",");    
    ps.print("(void *)&");
    ps.print(kd.getName());
    ps.println(");");
    ps.print(prefix);
    ps.print("oclCheckStatus(status,\"clSetKernelArg (");
    ps.print(kd.getName());
    ps.println(") failed.\");");
  }

  static public void generateKernelWrapperParamDeclaration(KernelData kd, PrintStream ps) {
    if (kd.shouldBeCached()) {
      // It will be passed as a buffer
      ps.print("cl_mem ");
      ps.print(kd.getName());
    }
    else {
      // It will be passed by value
      CLGenKernelData.generateBaseCType(kd,ps);
      ps.print(" ");
      ps.print(kd.getName());
    }
  }

}
