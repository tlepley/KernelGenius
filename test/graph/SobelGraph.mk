#**
#   Sobel filter as a graph of nodes
#   
#   Redistribution of this file to outside parties is
#   strictly prohibited without the written consent
#   of the module owner indicated below.
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
APP_NAME = Sobel
KG_SOURCE = SobelGraph
PROGRAM_NAME = Sobel

RUN_ARGS = -border mirror
KGFLAGS = -O1 -DBORDER_MODE=mirror

# Include the generic kernel test makefile
include ../common.mk
