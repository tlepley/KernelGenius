################################################################################
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
################################################################################

This directory contains a set of KernelGenius examples with a test application
that checks for correct results.

All examples can be compiled and executed in the same manner:

make DEVICE_TYPE=<name> build-kg : run the KernelGenius compiler only
make DEVICE_TYPE=<name> build    : same as 'make build-kg', plus compiles the
								   generated OpenCL code and the test application
make DEVICE_TYPE=<name> run      : same as 'make build', plus runs the OpenCL
								   generated application

make DEVICE_TYPE=<name> clean    : remove generated files
make cleanall					 : removed all build directories


Currently, supported devices are :
 - cpu_intel
 - cpu_amd
 - gpu_nvidia
 - sthorm
 
The OpenCL toolkit of the target device must installed.
