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
# Upsampling-by-2 configuration
#=============================================================================

# Configuration
WG0 ?= 2
WG1 ?= 2
WI ?= 16
SIZE_X ?= 640
SIZE_Y ?= 480

APP_NAME = Upsampling
KG_SOURCE = Upsampling
PROGRAM_NAME = Upsampling

RUN_ARGS = -x $(SIZE_X) -y $(SIZE_Y) -wi $(WI) -wg0 $(WG0) -wg1 $(WG1)
KGFLAGS = --report


#=============================================================================
# Variables
#=============================================================================

# Source directory
SRC_DIR ?= src

# Flags for KernelGenius
ifndef KGCOMPILER 
KGCOMPILER = kgenc 
KG_TARGET_DEVICE = cpu
endif

KGFLAGS ?=
KG_OPT ?=

# KernelGenius compilation
ifdef KG_TARGET_LANGUAGE
KGFLAGS += --target_language $(KG_TARGET_LANGUAGE)
endif

ifdef KG_TARGET_DEVICE
KGFLAGS += --target_device $(KG_TARGET_DEVICE)
endif

ifdef ASYNC_MERGE
KGFLAGS += --async
endif


#=============================================================================
# Compilation rules
#=============================================================================

VPATH = $(SRC_DIR)

KG_BUILD_DIR = $(CURDIR)/build_kg
KG_SOURCE_FILE = $(KG_SOURCE).kg
KG_COMPILATION_CMD = $(KGCOMPILER) --outdir $(KG_BUILD_DIR) $(KGFLAGS) $(KG_OPT) -o $(PROGRAM_NAME)

all:: build

# Implicit rules
.SUFFIXES: .kg .cl .c .h

$(KG_BUILD_DIR)/$(PROGRAM_NAME).cl \
$(KG_BUILD_DIR)/$(PROGRAM_NAME).c \
$(KG_BUILD_DIR)/$(PROGRAM_NAME).h : $(KG_SOURCE_FILE)
	echo "--- Compiling KernelGenius file $<"
	$(KG_COMPILATION_CMD) $<


#=============================================================================
# Cleaning
#=============================================================================

clean::
	rm -rf $(KG_BUILD_DIR)/$(PROGRAM_NAME).c  \
	       $(KG_BUILD_DIR)/$(PROGRAM_NAME).cl \
	       $(KG_BUILD_DIR)/$(PROGRAM_NAME).h

cleanall::
	rm -rf $(KG_BUILD_DIR)


#=============================================================================
# Backend makefile
#=============================================================================

MAKE_DIR=make

ifdef NO_OCL_COMPILATION

# Just KG compilation
all:: build

build:: $(KG_BUILD_DIR)/$(PROGRAM_NAME).c

else
# Include the generic kernel test makefile
include $(MAKE_DIR)/MakefileOpenCL.mk
endif

