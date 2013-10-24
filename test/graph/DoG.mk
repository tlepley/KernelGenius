#**
#   Test for the DoG kernel
#   
#   Redistribution of this file to outside parties is
#   strictly prohibited without the written consent
#   of the module owner indicated below.
#
#   \par  Module owner: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#
#   \par  Copyright STMicroelectronics (C) 2013
#
#   \par  Authors: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#**

BORDER ?= duplicate

# Test configuration
APP_NAME = DoG
KG_SOURCE = DoG
PROGRAM_NAME = DoG

RUN_ARGS = -border $(BORDER)
KGFLAGS = -DBORDER=$(BORDER)

# Include the generic kernel test makefile
include ../common.mk
