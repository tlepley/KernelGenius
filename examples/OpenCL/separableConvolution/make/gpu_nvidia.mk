ifndef OCL_INSTALL_DIR
OCL_INSTALL_DIR = /usr/local/cuda
endif

# Host compilation
HOST_CC = gcc
OPENCL_CFLAGS += -I$(OCL_INSTALL_DIR)/include
OPENCL_LDFLAGS += -L$(OCL_INSTALL_DIR)/lib64 -lOpenCL

# Execution
RUN_ARGS += -vendor NVIDIA

# Build directory
PLT_BUILD_DIR = $(BUILD_DIR)_gpu_nvidia
