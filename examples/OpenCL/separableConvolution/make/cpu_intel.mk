OCL_INSTALL_DIR = /opt/intel/opencl/

# Offline OpenCL compilation
CLCOMPILER = $(OCL_INSTALL_DIR)/bin/ioc64
CLCOMPILER_IN = -input=
CLCOMPILER_OUT= -ir=

# Host compilation
HOST_CC = gcc
OPENCL_CFLAGS += -I$(OCL_INSTALL_DIR)/include
OPENCL_LDFLAGS += -L$(OCL_INSTALL_DIR)/lib64 -lOpenCL

# Execution
RUN_ARGS += -vendor Intel

# Build directory
PLT_BUILD_DIR = $(BUILD_DIR)_cpu_intel
