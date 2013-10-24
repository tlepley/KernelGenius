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

import target.ComputeDevice;
import codegen.CodeGenerator;
import common.CompilerError;

public abstract class CodegenOptions {

  public enum KERNEL_GRANULARITY_MODE {
    // Kernel inputs/outputs are the full dataset (image)
    // and must be tiled by the kernel itsef
    IMAGE,
    
    // Kernel inputs/outputs are tiles. Data movements across the
    // memory hierarchy are already handled by a middleware (like OpenVX)
    // and the kernel processes directly data from the 'global' memory
    TILE
  };
  
  public enum KERNEL_INTERNAL_TILING_MODE {
    // Process directly from the global memory
    NO_TILING,

    // Line-by-line tiling/processing, circular buffer
    LINE,
    
    // 2D blocks tiling with double buffer
    TILE
  }

  public enum NODE_MERGE_MODE {
    // No double buffer between nodes, barriers between nodes
    SYNC,
    // Double buffer between nodes, no barriers between nodes
    ASYNC
  };

  // Thread local storage
  static class OptionStorage {
    // Code generation reporting to the user
    boolean reportGeneration=false;

    // Target language for the device 
    String targetLanguage="OpenCL";

    // Target device
    public ComputeDevice targetDevice=null;
    String targetDeviceName = null;
    String targetSystemName = "middleware"; 

    KERNEL_GRANULARITY_MODE kernelGranularityMode=null;
    KERNEL_INTERNAL_TILING_MODE internalTilingMode=null;

    NODE_MERGE_MODE merge_mode=null;

    int nbWG=-1;
    int nbWI=-1;

    boolean fixDMA=false;
  }

  private static InheritableThreadLocal<OptionStorage> options = new InheritableThreadLocal<OptionStorage>() {
    @Override
    protected OptionStorage initialValue() {
      return new OptionStorage();
    }
  };

  public static String getTargetLanguage() {
    return options.get().targetLanguage;
  }
  public static void setTargetLanguage(String s) {
    options.get().targetLanguage=s;
  }

  public static ComputeDevice getTargetDevice() {
    return options.get().targetDevice;
  }
  public static void setTargetDevice(ComputeDevice cd) {
    options.get().targetDevice=cd;
  }

  public static boolean getReportGeneration() {
    return options.get().reportGeneration;
  }
  public static void setReportGeneration(boolean s) {
    options.get().reportGeneration=s;
  }

  public static String getTargetDeviceName() {
    return options.get().targetDeviceName;
  }
  public static void setTargetDeviceName(String s) {
    options.get().targetDeviceName=s;
  }
  public static String getTargetSystemName() {
    return options.get().targetSystemName;
  }
  public static void setTargetSystemName(String s) {
    options.get().targetSystemName=s;
  }

  public static KERNEL_GRANULARITY_MODE getKernelGranularityMode() {
    return options.get().kernelGranularityMode;
  }
  public static void setKernelGranularityMode(KERNEL_GRANULARITY_MODE s) {
    options.get().kernelGranularityMode=s;
  }
  
  public static KERNEL_INTERNAL_TILING_MODE getInternalTilingMode() {
    return options.get().internalTilingMode;
  }
  public static void setInternalTilingMode(KERNEL_INTERNAL_TILING_MODE s) {
    options.get().internalTilingMode=s;
  }

  public static NODE_MERGE_MODE getMergeMode() {
    return options.get().merge_mode;
  }
  public static void setMergeMode(NODE_MERGE_MODE s) {
    options.get().merge_mode=s;
  }

  public static int getNbWG() {
    return options.get().nbWG;
  }
  public static void setNbWG(int s) {
    options.get().nbWG=s;
  }
  public static int getNbWI() {
    return options.get().nbWI;
  }
  public static void setNbWI(int s) {
    options.get().nbWI=s;
  }
  public static boolean getFixDMA() {
    return options.get().fixDMA;
  }
  public static void setFixDMA() {
    options.get().fixDMA=true;
  }


  
  
  //========================================================
  // Command line processing
  //========================================================
 
  static public int parseOptions(String[] args, int i) {
    String option = args[i];

    if (option.startsWith("-")) {

      // Code generation report
      if (option.equals("--report")) {
        setReportGeneration(true);
        return 1;
      }

    
      // Target compute device 
      else if (option.equals("--target_device")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing device name after option '" + option + "'");
        }
        setTargetDeviceName(args[i]);
        return 2;
      }

      // Target language
      else if (option.equals("--target_language")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing device name after option '" + option + "'");
        }
        CodeGenerator.checkGeneratorFromName(args[i],CompilerError.GLOBAL);
        setTargetLanguage(args[i]);
        return 2;
      }

      // Target system (which application wrapper to generate) 
      else if (option.equals("--target_system")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing system name after option '" + option + "'");
        }
        setTargetSystemName(args[i]);
        return 2;
      }


      // Generated kernel specialization
      else if (option.equals("--nbWI")) {
        if (getNbWI()>1) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <level> after option '" + option + "'");
        }
        try {
          int optValue=Integer.valueOf(args[i]);
          if (optValue<=0) {
            CompilerError.GLOBAL.raiseWarning("while parsing options '"+option+": '" + args[i] + "' is not strictly positive");
          }
          else {
            setNbWI(optValue);
          }
        }
        catch (NumberFormatException e) {
          CompilerError.GLOBAL.raiseWarning("while parsing options '"+option+": '" + args[i] + "' is not a number");
        }
        return 2;
      }
      else if (option.equals("--nbWG")) {
        if (getNbWG()>1) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <level> after option '" + option + "'");
        }
        try {
          int optValue=Integer.valueOf(args[i]);
          if (optValue<=0) {
            CompilerError.GLOBAL.raiseWarning("while parsing options '"+option+": '" + args[i] + "' is not strictly positive");
          }
          else {
            setNbWG(optValue);
          }
        }
        catch (NumberFormatException e) {
          CompilerError.GLOBAL.raiseWarning("while parsing options '"+option+": '" + args[i] + "' is not a number");
        }
        return 2;
      }
      else if (option.equals("--targetWI")) {
        if (getNbWI()>1) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setNbWI(CodegenOptions.getTargetDevice().getComputeUnit().getNbComputeElements());
        return 1;
      }
      else if (option.equals("--targetWG")) {
        if (getNbWG()>1) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setNbWG(CodegenOptions.getTargetDevice().getNbComputeUnits());
        return 1;
      }

      // Kernel granularity
      else if (option.equals("--full_input")) {
        if (getKernelGranularityMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setKernelGranularityMode(KERNEL_GRANULARITY_MODE.IMAGE);
        return 1;
     }
      else if (option.equals("--tile_input")) {
        if (getKernelGranularityMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setKernelGranularityMode(KERNEL_GRANULARITY_MODE.TILE);
        return 1;
      }
      
      // Kernel internal tiling
      else if (option.equals("--no_internal_tiling")) {
        if (getInternalTilingMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setInternalTilingMode(KERNEL_INTERNAL_TILING_MODE.NO_TILING);
        return 1;
     }
      else if (option.equals("--line_internal_tiling")) {
        if (getInternalTilingMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setInternalTilingMode(KERNEL_INTERNAL_TILING_MODE.LINE);
        return 1;
      }
     else if (option.equals("--tile_internal_tiling")) {
        if (getInternalTilingMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined code generation option");
        }
        setInternalTilingMode(KERNEL_INTERNAL_TILING_MODE.TILE);
        return 1;
      }

      
      // Node merge mode
      else if (option.equals("--async")) {
        if (getMergeMode()!=null) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' overwrites a previously defined graph merge option");
        }
        setMergeMode(NODE_MERGE_MODE.ASYNC);
        return 1;
      }

      // Fix DMA problem with the STHORM board
      else if (option.equals("--fixDMA")) {
        if (getFixDMA()) {
          CompilerError.GLOBAL.raiseWarning("Option '" + option + "' defined twice");
        }
        setFixDMA();
        return 1;
      }

    }

    return 0;  
  }

  static public void printHelp() {
    CompilerError.GLOBAL.raiseMessage(
        "Code generation options:\n" +
        "  --report    : display a code generation report\n" +
        "  --nbWI <n>  : Generate a code specialized for 'n' work-items\n" +
        "  --targetWI  : Generate a code specialized for the target architecture in term of work-items\n" +
        "  --nbWG <n>  : Generate a code specialized for 'n' work-groups\n" +
        "  --targetWG  : Generate a code specialized for the target architecture in term of work-groups\n" +
        "  --async     : Merge the kernel graph in async mode"
        );
  }

  static public void printHelpDevel() {
    CompilerError.GLOBAL.raiseMessage(
        "Code generation options (for tool developers):\n" +
        "  --target_language <name> : target language\n"+
        "  --target_device <name> : target device\n"+
        "  --target_system <name> : target system\n" +
        "  --full_input : generate OpenCL kernels which processes the full data set (default)\n" +
        "  --tile_input : generate OpenCL kernels which processes a data tile\n"+
        "  --no_internal_tiling   : generate OpenCL kernels which processes inputs directly from the global memory\n" +
        "  --line_internal_tiling : generate OpenCL kernels which processes inputs line-by-line (default)\n" +
        "  --tile_internal_tiling : generate OpenCL kernels which processes inputs tile-by-tile"
        );
  }

  
  //========================================================
  // Query
  //========================================================
  
  
  // Kernel granularity
  public static boolean isImageKernelMode() {
    return getKernelGranularityMode()==KERNEL_GRANULARITY_MODE.IMAGE;
  }
  public static boolean isTileKernelMode() {
    return getKernelGranularityMode()==KERNEL_GRANULARITY_MODE.TILE;
  }

  // Kernel internal tiling
  public static boolean isNoKernelInternalTiling() {
    return getInternalTilingMode()==KERNEL_INTERNAL_TILING_MODE.NO_TILING;
  }
  public static boolean isLineKernelInternalTiling() {
    return getInternalTilingMode()==KERNEL_INTERNAL_TILING_MODE.LINE;
  }
  public static boolean isTileKernelInternalTiling() {
    return getInternalTilingMode()==KERNEL_INTERNAL_TILING_MODE.TILE;
  }

  // Node merge mode
  public static boolean isAsyncMergeMode() {
    return getMergeMode()==NODE_MERGE_MODE.ASYNC;
  }
  public static boolean isSyncMergeMode() {
    return getMergeMode()==NODE_MERGE_MODE.SYNC;
  }

}
