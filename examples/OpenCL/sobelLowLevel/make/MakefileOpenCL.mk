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
# Directories
##########################################################

# Absolute paths
RUNTIME_DIR= $(KERNELGENIUS_DIR)/runtime
MAKE_DIR=make

# Relative path
BUILD_DIR = build


##########################################################
# Target architecture configuration
##########################################################

# Include the generic kernel test makefile
ifeq ($(DEVICE_TYPE),cpu_intel)
include $(MAKE_DIR)/cpu_intel.mk
else ifeq ($(DEVICE_TYPE),cpu_amd)
include $(MAKE_DIR)/cpu_amd.mk
else ifeq ($(DEVICE_TYPE),gpu_nvidia)
include $(MAKE_DIR)/gpu_nvidia.mk
else ifeq ($(DEVICE_TYPE),sthorm)
include $(MAKE_DIR)/sthorm.mk
else
$(warning DEVICE_TYPE is not defined, taking cpu_intel)
DEVICE_TYPE=cpu_intel
include $(MAKE_DIR)/cpu_intel.mk
endif

# Check target configuration and user options
ifndef CLCOMPILER
ifndef ONLINE_CL_COMPILATION
$(error offline CL compilation not supported, please set 'ONLINE_CL_COMPILATION=1')
endif
endif


##########################################################
# Kernels configuration
##########################################################

# Find all kernels
KERNELS_SRCS  := $(KG_BUILD_DIR)/$(PROGRAM_NAME).cl
KERNELS_BIN   := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).so
KERNELS_SRCSBIN := $(PLT_BUILD_DIR)/$(PROGRAM_NAME).cl


##########################################################
# Host configuration
##########################################################

# Compilation options
HOST_CFLAGS += -Wall $(OPENCL_CFLAGS) $(foreach sdir,$(SRC_DIR),-I$(sdir))
HOST_LDFLAGS += -Wall $(OPENCL_LDFLAGS)
ifdef P2012_FABRIC
# 32 bit version for STHORM
HOST_LDFLAGS += -L$(RUNTIME_DIR)/lib32 -Wl,-rpath,$(RUNTIME_DIR)/lib32
else
# Actual host version for others
HOST_LDFLAGS += -L$(RUNTIME_DIR)/lib -Wl,-rpath,$(RUNTIME_DIR)/lib
endif
HOST_LDFLAGS += -lKgOclRuntime -lm

# Source & objects
SRCS = $(SRC_DIR)/test_$(APP_NAME).c $(KG_BUILD_DIR)/$(PROGRAM_NAME).c
OBJS := $(patsubst %.cpp,$(PLT_BUILD_DIR)/gen/%.o,$(patsubst %.c,$(PLT_BUILD_DIR)/gen/%.o,$(notdir $(SRCS))))


##########################################################
# Compilation rules
##########################################################

VPATH = $(SRC_DIR)

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
.PHONY: all build run debug clean cleanall
.SUFFIXES: .so .cl .o

$(PLT_BUILD_DIR)/%.so: $(KG_BUILD_DIR)/%.cl
	@echo "--- Compiling OpenCL kernels '$<' to '$@'"
	@mkdir -p $(PLT_BUILD_DIR)
ifeq ($(DEVICE_TYPE),cpu_intel)
	$(PREFIX)$(CLCOMPILER) $(CLCFLAGS) $(CLCOMPILER_IN)$< $(CLCOMPILER_OUT)$@
else
	$(PREFIX)$(CLCOMPILER) $(CLCFLAGS) $(CLCOMPILER_OUT) $@ $(CLCOMPILER_IN) $<
endif

$(PLT_BUILD_DIR)/%.cl: $(KG_BUILD_DIR)/%.cl
	@echo "--- Copying OpenCL kernels '$<' to '$@'"
	@mkdir -p $(PLT_BUILD_DIR)
	$(PREFIX)cp $< $@

$(PLT_BUILD_DIR)/gen/%.o: $(KG_BUILD_DIR)/%.c
	@echo "--- Compiling '$<' to '$@'"
	@mkdir -p $(PLT_BUILD_DIR)/gen
	$(PREFIX)$(HOST_CC) $(HOST_CFLAGS) -I$(KG_BUILD_DIR) -I$(RUNTIME_DIR)/include -c $< -o $@

$(PLT_BUILD_DIR)/gen/%.o: %.c
	@echo "--- Compiling '$<' to '$@'"
	@mkdir -p $(PLT_BUILD_DIR)/gen
	$(PREFIX)$(HOST_CC) $(HOST_CFLAGS) -I$(KG_BUILD_DIR) -I$(RUNTIME_DIR)/include -c $< -o $@


$(EXEC): $(OBJS)
	@echo "--- Linking $@"	
	$(PREFIX)$(HOST_CC) $(HOST_CFLAGS) $(RUNPTH) $^ -o $@ $(HOST_LDFLAGS)


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
	@echo "--- executes $(EXEC) in debug mode"
	$(PREFIX)$(CMD_EXEC_DEBUG)

run: build
	@echo "--- executes $(EXEC)"
	$(PREFIX)$(CMD_EXEC)


##########################################################
# Cleaning
##########################################################

clean::
	@rm -f $(EXEC)
	@rm -f $(OBJS)
	@rm -f $(KERNELS_BIN)
	@rm -f $(KERNELS_SRCSBIN)

cleanall::
	@rm -rf build_*

