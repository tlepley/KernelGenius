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

package driver.options;

import java.util.LinkedList;

import common.CompilerError;
import driver.DriverHelper.STAGE;

public abstract class DriverOptions {
  

  static class OptionStorage {
    // Package installation directory
    public String installDir = null;

    // Compilation stage option
    public STAGE stopStage = STAGE.NO;

    // Compilation options
    //--------------------
    // Keep intermediate files
    public boolean keepIntermediateFiles = false;
    // Preprocessor directives
    public boolean noPreprocessor = false;
    // Link option
    public boolean forceLink = false;
    // Parse, analyze, but does not emit
    public boolean noEmit = false;

    // Regeneration options
    //---------------------
    // Debug information
    boolean debugInformation=false;
    // Output directory
    public String outputDirectoryName = ".";
    // Output file
    public String outputFileName = null;

    // Preprocessor compiler
    //----------------------
    public String preprocessorTool = "gcc"; // Default preprocessor compiler
    public LinkedList<String> preprocessorOptionList = new LinkedList<String>();
  }

  private static InheritableThreadLocal<OptionStorage> options = new InheritableThreadLocal<OptionStorage>() {
    @Override
    protected OptionStorage initialValue() {
      return new OptionStorage();
    }
  };

  // Option accessors
  public static String getInstallDir() {
    return options.get().installDir;
  }
  public static void setInstallDir(String s) {
    options.get().installDir=s;
  }
  public static STAGE getStopStage() {
    return options.get().stopStage;
  }
  public static void setStopStage(STAGE s) {
    options.get().stopStage=s;
  }
  public static boolean getKeepIntermediateFiles() {
    return options.get().keepIntermediateFiles;
  }
  public static void setKeepIntermediateFiles(boolean s) {
    options.get().keepIntermediateFiles=s;
  }
  public static boolean getNoPreprocessor() {
    return options.get().noPreprocessor;
  }
  public static void setNoPreprocessor(boolean s) {
    options.get().noPreprocessor=s;
  }
  public static boolean getForceLink() {
    return options.get().forceLink;
  }
  public static void setForceLink(boolean s) {
    options.get().forceLink=s;
  }
  public static boolean getNoEmit() {
    return options.get().noEmit;
  }
  public static void setNoEmit(boolean s) {
    options.get().noEmit=s;
  }
  public static boolean getDebugInformation() {
    return options.get().debugInformation;
  }
  public static void setDebugInformation(boolean s) {
    options.get().debugInformation=s;
  }
  public static String getOutputDirectoryName() {
    return options.get().outputDirectoryName;
  }
  public static void setOutputDirectoryName(String s) {
    options.get().outputDirectoryName=s;
  }
  public static String getOutputFileName() {
    return options.get().outputFileName;
  }
  public static void setOutputFileName(String s) {
    options.get().outputFileName=s;
  }
  public static String getPreprocessorTool() {
    return options.get().preprocessorTool;
  }
  public static void setPreprocessorTool(String s) {
    options.get().preprocessorTool=s;
  }
  public static LinkedList<String> getPreprocessorOptionList() {
    return options.get().preprocessorOptionList;
  }


  public static int parseOptions(String[] args, int i) {
    String option = args[i];

    if (option.startsWith("-")) {

      // Package installation directory 
      // ------------------------------
      if (option.equals("--install_dir")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <path> after option '" + option + "'");
        }
        setInstallDir(args[i]);
        return 2;
      }


      
      // Preprocessor device 
      else if (option.startsWith("-D") || option.startsWith("-I")) {
        getPreprocessorOptionList().add(args[i]);
        return 1;
      }

      // Compilation stages
      else if (option.equals("-E")) {
        if (getStopStage()!=STAGE.NO) {
          CompilerError.GLOBAL.raiseFatalError("cannot specify together -E, -C");
        }
        setStopStage(STAGE.PREPROC);
        return 1;
      }
      //      else if (option.equals("-C")) {
      //        if (getStopStage()!=STAGE.NO) {
      //          globalCompilerError.raiseFatalError("cannot specify together -E, -C");
      //        }
      //        setStopStage(STAGE.C2C);
      //        return 1;
      //      }

      else if (option.equals("--parse")) {
        setStopStage(STAGE.C2C);
        setNoEmit(true);
        return 1;
      }

      // Keep intermediate file
      else if (option.equals("--keep")) {
        setKeepIntermediateFiles(true);
        return 1;
      }

      // No preprocessor directive in the C2C generated file
      else if (option.equals("--nopreproc")) {
        setNoPreprocessor(true);
        return 1;
      }


      //  Regeneration options
      // ---------------------

      // Debug generation
      else if (option.equals("-g")) {
        setDebugInformation(true);
        getPreprocessorOptionList().add("-g");
        return 1;
      }
      else if (option.equals("-o")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing a program name after option '" + option + "'");
        }
        if (getOutputFileName()!=null) {
          CompilerError.GLOBAL.raiseFatalError("output option specified twice");
        }
        setOutputFileName(args[i]);
        return 2;
      }
      else if (option.equals("--outdir")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <name> after option '" + option + "'");
        }
        setOutputDirectoryName(args[i]);
        return 2;
      }
    }

    return 0;  
  }


  public static void printHelp() {
    CompilerError.GLOBAL.raiseMessage(
        "Driver options:\n" +
            "  -E                : stop the compilation process after the preprocessing\n" +
 //          "  -C                : stop the compilation process after the low-level kernel generation\n" +
            "  --keep            : keep intermediate files\n" +
            "  -o <programName>  : name of the generated program\n" +
            "  --outdir <name>   : specifies the output directory of generated files"
        );
  }
  public static void printHelpDevel() {
    CompilerError.GLOBAL.raiseMessage(
            "Driver options (for tool developers):\n" +
            "  --nopreproc       : does not regenerate preprocessing directives (development option)\n" +
            "  --parse           : parse and check but does not generate any output file"
       );
  }
  
}
