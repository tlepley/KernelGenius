ifndef OCL_INSTALL_DIR
OCL_INSTALL_DIR = /usr
endif

# Host compilation
HOST_CC = gcc
OPENCL_CFLAGS += -I$(OCL_INSTALL_DIR)/include/nvidia-current
OPENCL_LDFLAGS += -L$(OCL_INSTALL_DIR)/lib -lOpenCL

# Execution
RUN_ARGS += -vendor Nvidia

# Build directory
PLT_BUILD_DIR = $(BUILD_DIR)_gpu_nvidia
