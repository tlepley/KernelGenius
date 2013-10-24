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

/* Driver for the KernelGenius compiler */


package driver;

import codegen.CodeGenerator;
import codegen.OpenCL.Generator;
import common.CompilerError;
import common.CompilerExit;
import common.ResourceManager;
import ir.base.Kernel;
import ir.base.Program;
import driver.options.GeneralOptions;
import driver.options.DriverOptions;
import driver.options.CodegenOptions;

import java.io.File;
import java.util.LinkedList;
import java.util.UUID;

import parser.DeviceParser;
import parser.ProgramParser;
import utility.antlr.antlrHelper;


public class KernelGenius {
  
  // Additional preprocessing options (builtins)
  LinkedList<String> additionalPreprocessingOptions=new LinkedList<String>();

  // List of input files
  //--------------------
  LinkedList<File> inputFileList = new LinkedList<File>();

  
  // Version
  static public int versionMajor = 1;
  static public int versionMinor = 3;
  static public int versionSubMinor = 0;
  
  static public void printVersion() {
    CompilerError.GLOBAL.raiseMessage(
        "version : " + versionMajor + "." + versionMinor + "." + versionSubMinor
        );
  }


  // ******************************************************************
  //  Help display:
  // ******************************************************************
  private void printHelp() {
    CompilerError.GLOBAL.raiseMessage("KernelGenius compiler");
    printVersion();
    CompilerError.GLOBAL.raiseMessage("Command : kgenc [options]* [input files]+");
    CompilerError.GLOBAL.raiseMessage(
        "General options:\n" +
            "  -v                : display compiler version\n" +
            "  --help            : help"
        );
    GeneralOptions.printHelp();
    DriverOptions.printHelp();
    CodegenOptions.printHelp();
  }
  private void printHelpDevel() {
    printHelp();
    CompilerError.GLOBAL.raiseMessage(
        "General options (for tool developers):\n" +
            "  --help-devel      : help"
        );
    GeneralOptions.printHelpDevel();
    DriverOptions.printHelpDevel();
    CodegenOptions.printHelpDevel();
  }

 
  // ******************************************************************
  // interpolate:
  //
  // Substitute reference to environment variables by their content
  // in the string
  // 
  // ******************************************************************
  static protected String interpolate(CompilerError cp, final String s) {
    final int i = s.indexOf("${");
    if (i == -1) return s;

    final int j = s.indexOf("}", i);
    if (j == -1) {
      cp.raiseFatalError("while parsing configuration file options: wrong environment variable (missing '}')");
      return s;
    }
    final String varName = s.substring(i + 2, j);
    String value = System.getProperty(varName);
    if (value == null) value = System.getenv(varName);

    if (value == null) {
      cp.raiseFatalError("while parsing configuration file options: unknown environment variable '"+varName+"'");
      // Error, unknown variable
      value = "";
    }
    return interpolate(cp,s.substring(0, i) + value + s.substring(j + 1));
  }


  // ******************************************************************
  //  processOptions:
  //
  //  Process options of the command line
  // 
  // ******************************************************************
  private void processOptions(String[] args) {
    for (int i=0; i<args.length; i++) {

      // General options
      int nb_general=GeneralOptions.parseOptions(args,i);
      if (nb_general!=0) {
        i+=nb_general-1;
        continue;
      }
      // Driver options
      int nb_driver=DriverOptions.parseOptions(args,i);
      if (nb_driver!=0) {
        i+=nb_driver-1;
        continue;
      }
     // Code generation options
      int nb_codegen=CodegenOptions.parseOptions(args,i);
      if (nb_codegen!=0) {
        i+=nb_codegen-1;
        continue;
      }
 
      String option = args[i];

      // Command line help options
      // -------------------------

      // Help
      if (option.equals("--help")) {
        printHelp();
        CompilerError.exitNormally();
      }
      else if (option.equals("--help-devel")) {
        printHelpDevel();
        CompilerError.exitNormally();
      }
      // Version
      else if (option.equals("-v")) {
        printVersion();
        CompilerError.exitNormally();
      }
      
      else {
        // Unrecognized option
        //--------------------

        // Put in the list to process only if the file exists
        File file=new File(args[i]);
        if (DriverHelper.checkInputFile(file)) {
          inputFileList.add(file);
        }
      }
    }


    // Perform global checks
    //----------------------

    // Check that at least one file is to be processed
    if (inputFileList.size() == 0) {
      CompilerError.GLOBAL. 
      raiseError("no file to compile in the command line");
    }

    if ((inputFileList.size()>1)&&(DriverOptions.getOutputFileName()!=null)) {
      CompilerError.GLOBAL.
      raiseError("cannot specify -o with multiple files");
    }

    // By default, we stop at the C2C stage
    if (DriverOptions.getStopStage()==DriverHelper.STAGE.NO) {
      DriverOptions.setStopStage(DriverHelper.STAGE.C2C);
    }
    
    
    // Code generation options
    // -----------------------
    
    if (CodegenOptions.getTargetDeviceName()==null) {
      CompilerError.GLOBAL. 
      raiseError("No target device defined");
    }
    if (CodegenOptions.getTargetLanguage()==null) {
      CompilerError.GLOBAL. 
      raiseError("No target language defined");
    }
    
    // Kernel Granularity
    if (CodegenOptions.getKernelGranularityMode()==null) {
      // Default kernel granularity is the full dataset
      CodegenOptions.setKernelGranularityMode(CodegenOptions.KERNEL_GRANULARITY_MODE.IMAGE);
    }
    
    // Internal kernel tiling mode
    if (CodegenOptions.getInternalTilingMode()==null) {
      // Default kernel granularity is the full dataset
      CodegenOptions.setInternalTilingMode(CodegenOptions.KERNEL_INTERNAL_TILING_MODE.LINE);
    }

    // Node merge mode
    if (CodegenOptions.getMergeMode()==null) {
      // Default graph merge mode is SYNC
      CodegenOptions.setMergeMode(CodegenOptions.NODE_MERGE_MODE.SYNC);
    }
  }



  // ##################################################################
  //
  //    			 Main
  //
  // ##################################################################

  // For execution from external shell
  public static void main(String[] args) {
    // Hooking the termination in case the code stops with a System.exit() call
    /*    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        ResourceManager.shutdown();
      }
    }); */

    try { new KernelGenius().mainNonStatic(args); }
    catch (CompilerExit e) {
      ResourceManager.shutdown();
      System.exit(e.getReturnStatus());
    }
    // In case of normal termination
    ResourceManager.shutdown();
  }

  // For execution from java proxy
  public static int mainNonExit(String[] args) {
    int returnValue=0;

    try { new KernelGenius().mainNonStatic(args); }
    catch (CompilerExit e) {
      returnValue=e.getReturnStatus();
    }
    ResourceManager.shutdown();
    return returnValue;
  }


  // ===================================================================
  // Main function
  // ===================================================================

  public void mainNonStatic(String[] args) {

    //==================================================================
    //  Parse the command line and process options
    //==================================================================
    processOptions(args);
    CompilerError.GLOBAL.exitIfError();

    //==================================================================
    // Check for correct input files
    //==================================================================

    for (File fileToProcess : inputFileList) { 
      DriverHelper.checkInputFile(fileToProcess);
    }
    CompilerError.GLOBAL.exitIfError();

    
    //==================================================================
    //  Create the output directory if necessary
    //==================================================================
    DriverHelper.createOutputDirectory(DriverOptions.getOutputDirectoryName());

    //==================================================================
    //  Create the temporary directory if necessary
    //==================================================================
    String tempDirName="_KG"+UUID.randomUUID().toString();
    File tempDir=new File(tempDirName);
    try {
      tempDir.mkdirs();
    }
    catch (Exception e) {
      CompilerError.GLOBAL.raiseFatalError("Can not create the temporary directory: "
          + tempDir.getPath());
    } 
    ResourceManager.registerTempDirectory(tempDir);
    if (GeneralOptions.getDebugLevel() > 0) {
      CompilerError.GLOBAL.raiseMessage("  ... Temporary directory =  '"
          + tempDir.getPath() + "'");
    }


    // ==================================================================
    // Parse the target configuration file
    // ==================================================================

    // Parse the device information
    String confFilename=DriverOptions.getInstallDir()+"/targets/"+CodegenOptions.getTargetDeviceName()+".cfg";
    File fileToParse=new File(confFilename);
    DeviceParser dp=new DeviceParser(GeneralOptions.getVerboseLevel(),fileToParse); 
    if (GeneralOptions.getDebugLevel() > 0) {
      CompilerError.GLOBAL.raiseMessage("   -> parsing target device configuration file '"
          + fileToParse.getName() + "'");
    }   
    dp.parse();
    CodegenOptions.setTargetDevice(dp.getDevice());
    if (GeneralOptions.getDebugLevel() > 2) {
      DriverHelper.print(CodegenOptions.getTargetDevice(), fileToParse, ": target device");
    }

    
    //==================================================================
    //  Build File array for inputs files to process
    //==================================================================
    LinkedList<File> kgFileList = new LinkedList<File>();
    LinkedList<File> ikgFileList = new LinkedList<File>();

    for (File file:inputFileList) {
      String path=file.getPath();
      if (path.endsWith(".kg")) {
        kgFileList.add(file);
      }
      else if (path.endsWith(".ikg")) {
        ikgFileList.add(file);
      }
      else {
        CompilerError.GLOBAL.raiseFatalError("Language of file '"
            + path +"' not recognized");
      }       
    }

    
    //==================================================================
    // Preprocessing
    //==================================================================

    if (!kgFileList.isEmpty()) {

      LinkedList<File> kgPreprocOutputFileList;
      kgPreprocOutputFileList=DriverHelper.runPreprocessor(kgFileList,"kg","ikg",tempDir,
          DriverOptions.getPreprocessorTool(),
          DriverOptions.getPreprocessorOptionList(),
          additionalPreprocessingOptions
          );

      // Copy back temporary files
      try {
        DriverHelper.copyBackTempFiles(DriverHelper.STAGE.PREPROC, DriverOptions.getStopStage(), DriverOptions.getKeepIntermediateFiles(),
            kgPreprocOutputFileList, null,
            null, DriverOptions.getOutputDirectoryName());
      } catch (Exception e) {
        CompilerError.GLOBAL.raiseFatalError("Can not copy back temporary file to "+DriverOptions.getOutputDirectoryName());
      }

      // Add the temporary files to the existing ikg file list
      ikgFileList.addAll(kgPreprocOutputFileList);
    }


    //====================================================================
    // Parsing: syntactic grammar check and semantic check (type, symbol)
    //====================================================================

    // List of file engines
    final LinkedList<ProgramParser> parserList = new LinkedList<ProgramParser>();

    if ((!ikgFileList.isEmpty())&&(DriverOptions.getStopStage()!=DriverHelper.STAGE.PREPROC)) {

      for (File fileToProcess : ikgFileList) { 
        // Check for correct input file
        if (!DriverHelper.checkInputFile(fileToProcess)) {
          continue;
        }

        String programName=DriverOptions.getOutputFileName();
        if (programName==null) {
          // Take the file name radix as default
          String fileName=fileToProcess.getName();  
          programName=fileName.substring(0,fileName.length()-".ikg".length());   
        }
        ProgramParser mp=new ProgramParser(GeneralOptions.getVerboseLevel(),fileToProcess,programName);

        if (GeneralOptions.getDebugLevel() > 0) {
          CompilerError.GLOBAL.raiseMessage("   -> parsing input file '"
              + fileToProcess.getName() + "'");
        }   
        try {
          mp.parse();
        } catch (Exception e) {
          mp.getCompilerError().raiseMessage(e.getMessage());
          mp.getCompilerError().raiseFatalError("parse Error, stopping the compilation process");
        }

        // We do not continue if some errors occurred and are still pending
        mp.getCompilerError().exitIfError();

        // Keep the parser for later work in the compiler
        parserList.add(mp);
      } // for ikgFileList

      
      // ==================================================================
      // Complete the IR and check it
      // ==================================================================
     
      for(ProgramParser mp:parserList) {
        Program prog=mp.getProgram();
        CompilerError ce=mp.getCompilerError();

        // Check for program correctness
         prog.completeAndCheck(ce);   

        // Optionally print the IR
        if (GeneralOptions.getDebugLevel() > 2) {
          System.out.println("============================================================================");
          DriverHelper.print(mp.getProgram(), mp.getInputFile(), ": IR");
          System.out.println();
          DriverHelper.print(mp.getSymbolTable(),mp.getInputFile(),": Symbol Table");

          if (GeneralOptions.getDebugLevel() > 3) {
            System.out.println();
            System.out.println("Abstract Syntax Tree");
            antlrHelper.printTree(mp.getAST(),0);
          }
          System.out.println("============================================================================");
        }

        // We do not continue if some errors occurred and are still pending
        ce.exitIfError();       
      }
      
      // ==================================================================
      // Performs optimizations
      // ==================================================================
      for(ProgramParser mp:parserList) {
        Program prog=mp.getProgram();
        CompilerError ce=mp.getCompilerError();
        
        if (GeneralOptions.getDebugLevel() > 0) {
          CompilerError.GLOBAL.raiseMessage("   -> Optimizing program '"+prog.getName()+"'");
        }

        for(int i=0;i<prog.getNbKernels();i++) {
          Kernel k=prog.getKernel(i);
          k.optimize(ce);
        }
        
        // We do not continue if some errors occurred and are still pending
        ce.exitIfError();
      }

      // ==================================================================
      // Performs analysis, scheduling and buffer allocation
      // ==================================================================

      // Compute data access pattern for kernel data
      for(ProgramParser mp:parserList) {
        Program prog=mp.getProgram();
        CompilerError ce=mp.getCompilerError();

        if (GeneralOptions.getDebugLevel() > 0) {
          CompilerError.GLOBAL.raiseMessage("   -> Analyzing program '"+prog.getName()+"'");
        }

        // Kernel Analyze
        for(int i=0;i<prog.getNbKernels();i++) {
          Kernel k=prog.getKernel(i);
          k.analyze(ce);
        }

        // We do not continue if some errors occurred and are still pending
        ce.exitIfError();
      }

      
      // ==================================================================
      // Code generation
      // ==================================================================

      if (!DriverOptions.getNoEmit()) {
        // Generate the OpenCL code for programs
        for(ProgramParser mp:parserList) {
          final LinkedList<File> generatedFiles= new LinkedList<File>();
          
          CodeGenerator gen=Generator.getNewGeneratorFromName(CodegenOptions.getTargetLanguage(),CompilerError.GLOBAL);
         // Generate Report if needed
          if (CodegenOptions.getReportGeneration()) {
            gen.generateReport(mp.getProgram(), System.out);
          }
          // Generate output files in the temporary directory
          gen.generate(mp.getProgram(),generatedFiles,tempDir);

          // Copy back files
          try {
            DriverHelper.copyBackTempFiles(DriverHelper.STAGE.C2C, 
                DriverOptions.getStopStage(), DriverOptions.getKeepIntermediateFiles(),
                generatedFiles, null,
                null, DriverOptions.getOutputDirectoryName());
          } catch (Exception e) {
            CompilerError.GLOBAL.raiseFatalError("Can not copy back temporary file to "+DriverOptions.getOutputDirectoryName());
          }

        }
      }

      // Some verbosing
      if (GeneralOptions.getDebugLevel() > 1) {
        for(ProgramParser mp:parserList) {
          Program prog=mp.getProgram();
          CompilerError.GLOBAL.raiseMessage("-----------------------------------------------------------");
          CompilerError.GLOBAL.raiseMessage("Tiling Analysis report for program '"+prog.getName()+"'");
          for(int i=0;i<prog.getNbKernels();i++) {
            Kernel k=prog.getKernel(i);
            if (GeneralOptions.getDebugLevel() > 1) {
              k.generateTilingReport();
            }
          }
          CompilerError.GLOBAL.raiseMessage("-----------------------------------------------------------");
        }
      }

    } // if (optionStage!=STAGE.PREPROC)

  } // main()

}



