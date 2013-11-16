INTEL_OCL_INSTALL_DIR = /opt/intel/opencl/


# Offline OpenCL compilation
CLCOMPILER = $(INTEL_OCL_INSTALL_DIR)/bin/ioc64
CLCOMPILER_IN = -input=
CLCOMPILER_OUT= -ir=


# Host compilation
HOST_CC = gcc
OPENCL_CFLAGS += -I$(INTEL_OCL_INSTALL_DIR)/include
OPENCL_LDFLAGS += -L$(INTEL_OCL_INSTALL_DIR)/lib64 -lOpenCL

# Build directory
PLT_BUILD_DIR = $(BUILD_DIR)_cpu_intel
