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
#   Example of separable 2D convolution with generic coefficients 
##################################################################

# Configuration
WG0 ?= 2
WG1 ?= 2
WI ?= 16
SIZE_X ?= 640
SIZE_Y ?= 480

#=============================================================================
# Test configuration
#=============================================================================
APP_NAME = SeparableConvolution
KG_SOURCE = SeparableConvolution
PROGRAM_NAME = SeparableConvolution

RUN_ARGS = -x $(SIZE_X) -y $(SIZE_Y) -wi $(WI) -wg0 $(WG0) -wg1 $(WG1)
KGFLAGS = --report

#=============================================================================
# Generic rules
#=============================================================================
SRC_DIR   ?= src
BUILD_DIR = $(CURDIR)/build_kg

ifndef KGCOMPILER 
KGCOMPILER = kgenc 
KG_TARGET_DEVICE = cpu
endif

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


# KernelGenius compilation
build-kg: $(BUILD_DIR)/$(PROGRAM_NAME).cl  $(BUILD_DIR)/$(PROGRAM_NAME).c  $(BUILD_DIR)/$(PROGRAM_NAME).h

# KernelGenius + OpenCL compilation
build:  $(BUILD_DIR)/$(PROGRAM_NAME).cl $(BUILD_DIR)/$(PROGRAM_NAME).c $(BUILD_DIR)/$(PROGRAM_NAME).h
	@make -f MakefileOpenCL.mk build SRC_DIR="$(SRC_DIR) $(BUILD_DIR)" APP_NAME="$(APP_NAME)"

# KernelGenius + OpenCL compilation + execution
run:  $(BUILD_DIR)/$(PROGRAM_NAME).cl $(BUILD_DIR)/$(PROGRAM_NAME).c $(BUILD_DIR)/$(PROGRAM_NAME).h
	@make -f MakefileOpenCL.mk run SRC_DIR="$(SRC_DIR) $(BUILD_DIR)" APP_NAME="$(APP_NAME)" RUN_ARGS="$(RUN_ARGS)"


# KernelGenius compilation rule
 $(BUILD_DIR)/$(PROGRAM_NAME).cl $(BUILD_DIR)/$(PROGRAM_NAME).c $(BUILD_DIR)/$(PROGRAM_NAME).h: $(SRC_DIR)/$(KG_SOURCE).kg
	@echo "--- Compiling KernelGenius file $<"
	@mkdir -p $(BUILD_DIR)
	$(KGCOMPILER) -o $(PROGRAM_NAME) --outdir $(BUILD_DIR) $(KGFLAGS) $<


# Cleanup rules
clean :
	rm -rf  $(BUILD_DIR)/$(PROGRAM_NAME).cl  $(BUILD_DIR)/$(PROGRAM_NAME).c  $(BUILD_DIR)/$(PROGRAM_NAME).h
	make -f MakefileOpenCL.mk clean

cleanall :
	rm -rf  $(BUILD_DIR)
	make -f MakefileOpenCL.mk cleanall
