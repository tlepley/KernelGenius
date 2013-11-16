#**
#   Test for the Upsampling
#   
#   Redistribution of this file to outside parties is
#   strictly prohibited without the written consent
#   of the module owner indicated below.\n
#
#   \par  Module owner: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#
#   \par  Copyright STMicroelectronics (C) 2012
#
#   \par  Authors: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#**


# Test configuration
APP_NAME = Upsampling
KG_SOURCE = Upsampling
PROGRAM_NAME = Upsampling

RUN_ARGS = 
KGFLAGS = -DBORDER_MODE=duplicate

# Include the generic kernel test makefile
include $(KERNELGENIUS_DIR)/test/make/common.mk
