##################################################################
#  This file is part of KernelGenius.
#
#  Copyright (C) 2013 STMicroelectronics
#
#  This library is free software; you can redistribute it and/or
#  modify it under the terms of the GNU Lesser General Public
#  License as published by the Free Software Foundation; either
#  version 3 of the License, or (at your option) any later version.
# 
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
#  Lesser General Public License for more details.
# 
#  You should have received a copy of the GNU Lesser General Public
#  License along with this program; if not, write to the Free
#  Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
#  Boston, MA 02110-1301 USA.
##################################################################


1. Functionality
================

KernelGenius is a tool that allows describing image processing graphs
natural way for algorithm developers and optimize them for parallel
architectures.

In its initial version, KernelGenius generates OpenCL code tuned for the
STHORM many-core of STMicroelectronics.


2. Package Installation and setup
=================================

a) Package
----------
The structure of the package is the following:

  KernelGenius
  |-- setup.sh
  |-- LICENSE
  |-- CHANGELOG.txt
  |-- README.txt
  |-- bin/
  |-- doc/
  |-- jar/
  |-- examples/
  `-- targets/
 
+ The 'bin' directory contains the driver of the KernelGenius compiler
+ The 'jar' directory contains jar libraries of the KernelGenius compiler
+ The 'doc' directory contains the KernelGenius documentation
+ The 'examples' directory contains a set of relevant examples
+ The 'target' directory contains internal target description files


b) Environment setup
--------------------

KernelGenius requires the bash shell environment.

For using the KernelGenius tool, the user must configure its environment by
1- make sure that 'bash' is the used shell
2- source the 'setup.sh' file of the KernelGenius package
3- Ensure that the Java runtime environment is installed and that the 'java'
   command is in the PATH. JAVA_HOME can optionally be set to the java jre/
   jdk installation directory

Examples have a makefile configured to compile and run the generated OpenCL
code on the STHORM many-core. Running these examples then requires having
installed and setup the STHORM SDK.


3. Compiling with KernelGenius
==============================

a) KernelGenius execution
--------------------------

The compiler infrastructure has a driver which handles the preprocessing, the
parsing, and the code generation. The command line for executing the
KernelGenius compiler is the following:

  kgenc [option]* <source file>+ 


b) KernelGenius options
-----------------------

User options:
  -v                : display compiler version
  --report          : display a code generation report (memory usage)
  --verbose <level> : display more warnings for application developer
        1: display additional warnings
  --help            : help
  -E                : stop the compilation process after the preprocessing
  --keep            : keep intermediate files
  -o <prog name>    : specifies the name of the output OpenCL program. By default,
  		      the program has the name of the input kg file
  --outdir <name>   : specifies the output directory of generated files

Code generation options:
  --async           : Merge the kernel graph in async mode



4. Some Limitations
===================

+ The reporting of syntax errors are in the current version relatively simple
  and may not be very explicit in all cases
+ KernelGenius is creating temporary directory to handle temporary files. In
  some rare cases like syntax of errors in the input file, the compiler may not
  delete the temporary directory. In this case the user will have to delete it
  manually (directory starting with '_KG')
