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
#   Common KernelGenius test makefile for the OpenCL STHORM target
##################################################################


##########################################################
# Specific OpenCL test bench configuration
##########################################################

WG0 ?= 1
WG1 ?= 1
WI ?= 16
SIZE_X ?= 640
SIZE_Y ?= 480

RUN_ARGS_TEST = -x $(SIZE_X) -y $(SIZE_Y) -wi $(WI) -wg0 $(WG0) -wg1 $(WG1)


##########################################################
# The following variables can be redefined in the parent 
# makefile which includes this rule file
##########################################################

# Build directory for all objects and executables, used as a prefix
BUILD_DIR = build

# C flags for OpenCL kernels
CLCFLAGS ?= -O2 -g -Wall

# Arguments to pass to the executable
RUN_ARGS ?=

# Compilation flags for the host code
HOST_CFLAGS  ?= -O2 -g
HOST_LDFLAGS ?=

# Library containing all the host objects (optional)
HOST_LIB_NAME ?= lib$(APP_NAME)_OCL


##########################################################
# Target architecture configuration
##########################################################

ifndef CLAM_PKG
$(error CLAM_PKG is not defined)
endif

include $(CLAM_PKG)/rules/targets.mk


##########################################################
# Kernels configuration
##########################################################

# Find all kernels
KERNELS_SRCS  := $(PROGRAM_NAME).cl
KERNELS_BIN   := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).so
KERNELS_SRCSBIN   := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).cl

CLAMC = $(CLAM_PKG)/bin/clamc

# OpenCL compilation
CLCFLAGS += -target_device $(TARGET_DEVICE)

ifdef CLAM_KERNELFLAGS
CLCFLAGS += $(CLAM_KERNELFLAGS)
endif


##########################################################
# Host configuration
##########################################################

# Compilation options
HOST_CFLAGS += -Wall $(CLAM_CFLAGS) $(foreach sdir,$(SRC_DIR),-I$(sdir)) -I. -I..
HOST_LDFLAGS += -Wall $(CLAM_LDFLAGS)

# Source & objects
SRCS = $(SRC_DIR)/test_$(APP_NAME).c $(PROGRAM_NAME).c
OBJS := $(patsubst %.cpp,$(PLT_BUILD_DIR)/gen/%.o,$(patsubst %.c,$(PLT_BUILD_DIR)/gen/%.o,$(notdir $(SRCS))))


##########################################################
# Compilation rules
##########################################################

VPATH = $(SRC_DIR) .

# Final executables
EXEC := $(PLT_BUILD_DIR)/test_$(APP_NAME)


# First target, can be extended in the parent makefile
all:: build

# Default is off-line CL kernel compilation
ifndef ONLINE_CL_COMPILATION
build:: $(KERNELS_BIN)
HOST_CFLAGS += -DAHEAD_OF_TIME
else
build:: $(KERNELS_SRCSBIN)
endif

build:: $(EXEC) 

# Implicit rules
.PHONY: all build clean test package run
.SUFFIXES: .so .cl .o

$(PROGRAM_NAME).cl $(PROGRAM_NAME).c $(PROGRAM_NAME).h : $(KG_SOURCE_FILE)
	@echo "--- Compiling KernelGenius file $<"
	$(KG_COMPILATION_CMD) $<

$(PLT_BUILD_DIR)/gen/%.o: %.c
	@mkdir -p $(PLT_BUILD_DIR)/gen
	@echo "--- Compiling '$<'"
	$(HOST_CC) $(HOST_CFLAGS) -c $< -o $@

$(PLT_BUILD_DIR)/%.so: %.cl
	@mkdir -p $(PLT_BUILD_DIR)
	@echo "--- Compiling OpenCL kernels in $<"
	$(CLAMC) $(CLCFLAGS) -o $@ -- $<
	
$(PLT_BUILD_DIR)/%.cl: %.cl
	@mkdir -p $(PLT_BUILD_DIR)
	@echo "--- Copying OpenCL kernels in $<"
	cp $< $@


##########################################################
# Execution
##########################################################

ifdef P2012_FABRIC
# This is the P2012 device
ifeq "$(RUN_ARGS)" ""
CMD_EXEC = p12run $(P12RUN_OPT) --test=$(PLT_BUILD_DIR) --cmd="./$(notdir $(EXEC)) $(RUN_ARGS_TEST)"
else
CMD_EXEC = p12run $(P12RUN_OPT) --test=$(PLT_BUILD_DIR) --cmd="./$(notdir $(EXEC)) $(RUN_ARGS) $(RUN_ARGS_TEST)"
endif
CMD_EXEC_DEBUG = $(CMD_EXEC) --gdb=clgdb

else
# This is an other device (GPU)
CMD_EXEC = cd $(PLT_BUILD_DIR);./$(notdir $(EXEC)) $(RUN_ARGS) $(RUN_ARGS_TEST)
CMD_EXEC_DEBUG = cd $(PLT_BUILD_DIR);gdb $(EXEC) $(RUN_ARGS) $(RUN_ARGS_TEST)
endif

$(EXEC): $(OBJS)
	@echo "--- Linking $@"	
	$(HOST_CC) $(HOST_CFLAGS) $(HOST_LDFLAGS) $(RUNPTH) $^ -o $@

debug: build
	$(CMD_EXEC_DEBUG)

run: build
	$(CMD_EXEC)


##########################################################
# Cleaning
##########################################################

clean::
	rm -f $(EXEC)
	rm -f $(OBJS)
	rm -rf $(PLT_BUILD_DIR)
	rm -rf $(PROGRAM_NAME).c $(PROGRAM_NAME).cl $(PROGRAM_NAME).h
	rm -f $(KERNELS_BIN)
	rm -f $(KERNELS_SRCSBIN)


