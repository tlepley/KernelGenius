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


2. Project structure
====================

The structure of the package is the following:

  KernelGenius
  |-- AUTHORS.txt
  |-- LICENSE.txt
  |-- README.txt
  |-- bin/
  |-- doc/
  |-- lib/
  |-- examples/
  |-- test/
  |-- script/
  `-- targets/
 
+ The 'bin' directory contains the driver of the KernelGenius compiler
+ The 'doc' directory contains the KernelGenius documentation
+ The 'lib' directory contains external tools
+ The 'licenses' directory contains additional license files
+ The 'examples' directory contains a set of relevant examples
+ The 'test' directory contains the KernelGenius test-suite
+ The 'script' directory contains useful files for the binary package build
+ The 'target' directory contains internal target description files


3. Compile KernelGenius
=======================

KernelGenius is configured to be built with Eclipse

The KernelGenius build process dependencies are:
- Java SDK
- ant (ant.apache.org)
- make


KernelGenius can also be built from the command line:

build:
  ant build
  
clean:
  ant clean
  
Package creation for distribution:
  ant install


3. Execute KernelGenius
=======================

To use KernelGenius, please read the user README.TXT file from the 'script' directory


