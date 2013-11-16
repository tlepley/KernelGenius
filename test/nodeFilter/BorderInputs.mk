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
#   Test of the Filter with inputs of different read patterns
#   as well as different border semantics 
##################################################################


WINDOW1 ?= 2
BORDER1 ?= "-13"
WINDOW2 ?= 4
BORDER2 ?= mirror

# Test configuration
APP_NAME = BorderInputs
KG_SOURCE = BorderInputs
PROGRAM_NAME = BorderInputs

RUN_ARGS = -border1 $(BORDER1) -border2 $(BORDER2) -window1 $(WINDOW1) -window2 $(WINDOW2)
KGFLAGS = -DBORDER1=$(BORDER1) -DWINDOW1=$(WINDOW1) -DBORDER2=$(BORDER2) -DWINDOW2=$(WINDOW2)

# Include the generic kernel test makefile
include $(KERNELGENIUS_DIR)/test/make/common.mk
