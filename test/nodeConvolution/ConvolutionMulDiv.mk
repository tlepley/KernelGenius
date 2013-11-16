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
#   Test for the Convolution node
##################################################################


BORDER ?= duplicate

# Test configuration
APP_NAME = Convolution
KG_SOURCE = ConvolutionMulDiv
PROGRAM_NAME = Convolution

KGFLAGS = -DCOEF_FILE="\"coefficients.h\"" -DMUL="2.04f" -DDIV="-1.82f" -DBORDER=$(BORDER)  
HOST_CFLAGS = -DCOEF_FILE="\"coefficients.h\"" -O2 -g

RUN_ARGS = -mul 2.04f -div -1.82f -border $(BORDER)


# Include the generic kernel test makefile
include $(KERNELGENIUS_DIR)/test/make/common.mk
