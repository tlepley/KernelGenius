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
#
#  Authors: Thierry Lepley
##################################################################

#=============================================================================
# Threshold configuration
#=============================================================================

# Configuration
WG0 ?= 2
WG1 ?= 2
WI ?= 16
SIZE_X ?= 640
SIZE_Y ?= 480

APP_NAME = Threshold
KG_SOURCE = Threshold
PROGRAM_NAME = Threshold

RUN_ARGS = -x $(SIZE_X) -y $(SIZE_Y) -wi $(WI) -wg0 $(WG0) -wg1 $(WG1)
KGFLAGS = --report


#=============================================================================
# Compilation
#=============================================================================
MAKE_DIR=make
include $(MAKE_DIR)/MakefileKG.mk
