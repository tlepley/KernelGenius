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
#   This the makefile for compiling the OpenCL version of
##################################################################


###############     Run options     ##################

# Arguments to pass to the executable
RUN_ARGS ?=  -x 640 -y 480 -wi 16 -wg1 1 -wg2 1 -operation Sub 


################ Compilation options #################

APP_NAME ?= Operation

# Directory config
BUILD_DIR = $(CURDIR)/build
SRC_DIR   ?= src

ifeq "$(FABRIC_TYPE)" "posix"
CLCFLAGS = -O0 -g
HOST_CFLAGS = -O0 -g
else
CLCFLAGS = -O2
HOST_CFLAGS = -O2
endif

HOST_LIB_NAME =


# CLAM common makefile
ifndef CLAM_PKG
$(error CLAM_PKG is not defined, please source the P2012 SDK setup file before lauching Eclipse)
endif
include $(CLAM_PKG)/rules/clam.mk

