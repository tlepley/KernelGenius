OCL_INSTALL_DIR = /opt/AMDAPP

# Host compilation
HOST_CC = gcc
OPENCL_CFLAGS += -I$(OCL_INSTALL_DIR)/include
OPENCL_LDFLAGS += -L$(OCL_INSTALL_DIR)/lib/x86_64 -lOpenCL

# Execution
RUN_ARGS += -vendor Advanced

# Build directory
PLT_BUILD_DIR = $(BUILD_DIR)_cpu_amd
