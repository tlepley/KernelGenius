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

PREFIX=@
ifdef VERBOSE
PREFIX=
endif

##########################################################
# The following variables can be redefined in the parent 
# makefile which includes this rule file
##########################################################

ifndef APP_NAME
$(error APP_NAME must be specified in the parent makefile)
endif

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

KG_BUILD_DIR = build_kg
KG_SOURCE_FILE = $(KG_SOURCE).kg
KG_COMPILATION_CMD = $(KGCOMPILER) --outdir $(KG_BUILD_DIR) $(KGFLAGS) $(KG_OPT) -o $(PROGRAM_NAME)

.PHONY: all build build-kg clean cleanall
VPATH = $(SRC_DIR)

all:: build

build-kg: $(KG_BUILD_DIR)/$(PROGRAM_NAME).cl

$(KG_BUILD_DIR)/$(PROGRAM_NAME).cl \
$(KG_BUILD_DIR)/$(PROGRAM_NAME).c \
$(KG_BUILD_DIR)/$(PROGRAM_NAME).h : $(KG_SOURCE_FILE)
	@echo "--- Compiling KernelGenius file $<"
	$(PREFIX)$(KG_COMPILATION_CMD) $<


#=============================================================================
# Cleaning
#=============================================================================

clean::
	@rm -rf $(KG_BUILD_DIR)/$(PROGRAM_NAME).c  \
	        $(KG_BUILD_DIR)/$(PROGRAM_NAME).cl \
	        $(KG_BUILD_DIR)/$(PROGRAM_NAME).h

cleanall::
	@rm -rf $(KG_BUILD_DIR)


#=============================================================================
# Backend makefile
#=============================================================================

ifdef NO_OCL_COMPILATION

# Just KG compilation
build:: build-kg

else
# Include the generic kernel test makefile
include $(MAKE_DIR)/MakefileOpenCL.mk
endif
