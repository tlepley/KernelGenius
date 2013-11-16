# Check for correct STHORM SDK settings
ifndef CLAM_PKG
$(error CLAM_PKG is not defined)
endif

include $(CLAM_PKG)/rules/targets.mk

# Offline OpenCL compilation
CLCOMPILER = $(CLAM_PKG)/bin/clamc
CLCOMPILER_OUT = '-o '
CLCOMPILER_in = '-- '
CLCFLAGS += -O2 -g -Wall -target_device $(TARGET_DEVICE)

ifdef CLAM_KERNELFLAGS
CLCFLAGS += $(CLAM_KERNELFLAGS)
endif

# Host compilation
# (HOST_CC already defined by the STHORM SDK=
OPENCL_CFLAGS=$(CLAM_CFLAGS)
OPENCL_LDFLAGS=$(CLAM_LDFLAGS)

# Build directory
# (PLT_BUILD_DIR already defined by the STHORM SDK)