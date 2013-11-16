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
# Directories
##########################################################

# Absolute paths
RUNTIME_DIR= $(KERNELGENIUS_DIR)/runtime
TEST_DIR= $(KERNELGENIUS_DIR)/test
MAKE_DIR= $(TEST_DIR)/make

# Relative path
BUILD_DIR = build


##########################################################
# Target architecture configuration
##########################################################

# Include the generic kernel test makefile
ifeq ($(DEVICE_TYPE),cpu_intel)
include $(MAKE_DIR)/cpu_intel.mk
else ifeq ($(DEVICE_TYPE),gpu_nvidia)
include $(MAKE_DIR)/gpu_nvidia.mk
else ifeq ($(DEVICE_TYPE),sthorm)
include $(MAKE_DIR)/sthorm.mk
else
$(warning DEVICE_TYPE is not defined, taking cpu_intel)
include $(MAKE_DIR)/cpu_intel.mk
endif


# Check target configuration and user options
ifndef CLCOMPILER
ifndef ONLINE_CL_COMPILATION
$(error offline CL compilation not supported)
endif
endif

##########################################################
# Kernels configuration
##########################################################

# Find all kernels
KERNELS_SRCS  := $(PROGRAM_NAME).cl
KERNELS_BIN   := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).so
KERNELS_SRCSBIN := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).cl


##########################################################
# Host configuration
##########################################################

# Compilation options
HOST_CFLAGS += -Wall $(OPENCL_CFLAGS) $(foreach sdir,$(SRC_DIR),-I$(sdir)) -I. -I..
HOST_LDFLAGS += -Wall $(OPENCL_LDFLAGS)

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
	echo "--- Compiling KernelGenius file $<"
	$(KG_COMPILATION_CMD) $<

$(PLT_BUILD_DIR)/%.so: %.cl
	@echo "--- Compiling OpenCL kernels in $<"
	mkdir -p $(PLT_BUILD_DIR)
ifeq ($(DEVICE_TYPE),cpu_intel)
	$(CLCOMPILER) $(CLCFLAGS)  $(CLCOMPILER_IN)$< $(CLCOMPILER_OUT)$@
else
	$(CLCOMPILER) $(CLCFLAGS) $(CLCOMPILER_OUT) $@ $(CLCOMPILER_IN) $<
endif

$(PLT_BUILD_DIR)/%.cl: %.cl
	@echo "--- Copying OpenCL kernels in $<"
	mkdir -p $(PLT_BUILD_DIR)
	cp $< $@
	
$(PLT_BUILD_DIR)/gen/%.o: %.c
	@echo "--- Compiling '$<'"
	mkdir -p $(PLT_BUILD_DIR)/gen
	$(HOST_CC) $(HOST_CFLAGS) -I$(RUNTIME_DIR)/include -c $< -o $@
	
$(EXEC): $(OBJS)
	@echo "--- Linking $@"	
	$(HOST_CC) $(HOST_CFLAGS) -L$(RUNTIME_DIR)/lib $(RUNPTH) $^ -o $@ $(HOST_LDFLAGS) -lKgOclRuntime
	

##########################################################
# Execution
##########################################################

ifdef P2012_FABRIC
# This is the STHORM device
CMD_EXEC = p12run $(P12RUN_OPT) --test=$(PLT_BUILD_DIR) --cmd="./$(notdir $(EXEC)) $(RUN_ARGS) $(RUN_ARGS_TEST)"
CMD_EXEC_DEBUG = $(CMD_EXEC) --gdb=clgdb

else
# This is an other device (GPU, CPU)
CMD_EXEC = cd $(PLT_BUILD_DIR);./$(notdir $(EXEC)) $(RUN_ARGS) $(RUN_ARGS_TEST)
CMD_EXEC_DEBUG = cd $(PLT_BUILD_DIR);gdb $(EXEC) $(RUN_ARGS) $(RUN_ARGS_TEST)
endif


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

