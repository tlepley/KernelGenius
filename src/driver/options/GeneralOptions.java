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

import common.CompilerError;

public abstract class GeneralOptions {
  // Thread local storage
  static class OptionStorage {
    public int verboseLevel = 0;
    public int debugLevel = 0;
    public int optimizationLevel = 0;
  }

  private static InheritableThreadLocal<OptionStorage> options = new InheritableThreadLocal<OptionStorage>() {
    @Override
    protected OptionStorage initialValue() {
      return new OptionStorage();
    }
  };
  
  public static int getVerboseLevel() {
    return options.get().verboseLevel;
  }
  public static void setVerboseLevel(int s) {
    options.get().verboseLevel=s;
  }
  public static int getDebugLevel() {
    return options.get().debugLevel;
  }
  public static void setDebugLevel(int s) {
    options.get().debugLevel=s;
  }
  public static int getOptimizationLevel() {
    return options.get().optimizationLevel;
  }
  public static void setOptimizationLevel(int s) {
    options.get().optimizationLevel=s;
  }


  public static int parseOptions(String[] args, int i) {
    String option = args[i];

    if (option.startsWith("-")) {
      // Verbose
      if (option.equals("--verbose")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <level> after option '" + option + "'");
        }
        try {
          setVerboseLevel(Integer.valueOf(args[i]));
        }
        catch (NumberFormatException e) {
          CompilerError.GLOBAL.raiseWarning("while parsing options: the option verbose level '" + args[i] + "' is not a number");
          setVerboseLevel(0);
        }
        CompilerError.GLOBAL.setVerboseLevel(getVerboseLevel());
        return 2;
      }

      // Debug
      else if (option.equals("--debug")) {
        i++;
        if (i==args.length) {
          CompilerError.GLOBAL.raiseFatalError("while parsing options: missing <level> after option '" + option + "'");
        }
        try {
          setDebugLevel(Integer.valueOf(args[i]));
        }
        catch (NumberFormatException e) {
          CompilerError.GLOBAL.raiseWarning("while parsing options: the option debug level '" + args[i] + "' is not a number");
          setDebugLevel(0);
        }
        return 2;
      }

      else if (option.equals("-O1")) {
        setOptimizationLevel(1);
        return 1;
      }

    }

    return 0;  
  }

  public static void printHelp() {
    CompilerError.GLOBAL.raiseMessage(
        "  --verbose <level> : display more warnings for application developper"
        );
  }
  public static void printHelpDevel() {
    CompilerError.GLOBAL.raiseMessage(
        "  --debug <level>   : display information for tool developper"
        );
  }
  
  // Nothing to check
  public static void check() { }
  
  // Nothing to verbose
  public static void verbose() { }

}

