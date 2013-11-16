##################################################################
#  This file is part of KernelGenius.
#
#  Copyright (C) 2013 STMicroelectronics
#
#  This library is free software; you can redistribute it and/or
#  modify it under the terms of the GNU Lesser General Public
#  License as published by the Free Software Foundation; either
#  version 3 of the License, or (at your option) any later version.
# 
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
#  Lesser General Public License for more details.
# 
#  You should have received a copy of the GNU Lesser General Public
#  License along with this program; if not, write to the Free
#  Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
#  Boston, MA 02110-1301 USA.
##################################################################

##################################################################
#   Test for the Threshold kernel
##################################################################


# Test configuration
APP_NAME = Threshold
KG_SOURCE = Threshold
PROGRAM_NAME = Threshold

RUN_ARGS =
HOST_CFLAGS  = -DREVERSE -O2 -g
KGFLAGS =

# Include the generic kernel test makefile
include $(KERNELGENIUS_DIR)/test/make/common.mk
