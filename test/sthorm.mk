ifndef CLAM_PKG
$(error CLAM_PKG is not defined)
endif

include $(CLAM_PKG)/rules/targets.mk

# OpenCL compilation
CLCOMPILER = $(CLAM_PKG)/bin/clamc
CLCFLAGS += -target_device $(TARGET_DEVICE)

ifdef CLAM_KERNELFLAGS
CLCFLAGS += $(CLAM_KERNELFLAGS)
endif
