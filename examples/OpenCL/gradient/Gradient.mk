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

##################################################################
#   Test of the KernelGenius Gradient
##################################################################


# Test configuration
APP_NAME = Gradient
KG_SOURCE = Gradient
PROGRAM_NAME = Gradient

RUN_ARGS = -x 640 -y 480 -wi 16 -wg 1
KGFLAGS = -DMODE=zero

# Include the generic kernel test makefile
include ../common.mk
