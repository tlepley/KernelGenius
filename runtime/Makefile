##################################################################
#  This file is part of KernelGenius.
#
#  Copyright (C) 2013 Thierry Lepley
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

HOST_CC = gcc
HOST_CFLAGS = -O2 -fPIC
HOST_LDFLAGS = -shared

INC_DIR = include
INC_DIR_EXTRA = include_extra
SRC_DIR = src
LIB_DIR = lib
LIB_DIR32 = lib32
BUILD_DIR = build
BUILD_DIR32 = build32

# Source & objects
SRCS = $(wildcard $(SRC_DIR)/*.c)
OBJS = $(patsubst %.c,$(BUILD_DIR)/%.o,$(notdir $(SRCS)))
OBJS32 = $(patsubst %.c,$(BUILD_DIR32)/%.o,$(notdir $(SRCS)))
LIB = libKgOclRuntime.so

.PHONY: all build clean package run

all:: build

build:: $(LIB_DIR)/$(LIB) $(LIB_DIR32)/$(LIB)

clean::
	rm -rf $(LIB_DIR) $(LIB_DIR32) $(BUILD_DIR)

$(LIB_DIR)/$(LIB): $(OBJS)
	echo "--- Compiling library $<"
	mkdir -p $(LIB_DIR)
	$(HOST_CC) $(HOST_LDFLAGS) -o $@ $<

$(LIB_DIR32)/$(LIB): $(OBJS32)
	echo "--- Compiling library $<"
	mkdir -p $(LIB_DIR32)
	$(HOST_CC) -m32  $(HOST_LDFLAGS) -o $@ $<

# Implicit rules
.SUFFIXES: .so .o .c

$(BUILD_DIR)/%.o: $(SRC_DIR)/%.c
	echo "--- Compiling '$<'"
	mkdir -p $(BUILD_DIR)
	$(HOST_CC) $(HOST_CFLAGS) -I$(INC_DIR) -I$(INC_DIR_EXTRA) -c $< -o $@
	
$(BUILD_DIR32)/%.o: $(SRC_DIR)/%.c
	echo "--- Compiling '$<'"
	mkdir -p $(BUILD_DIR32)
	$(HOST_CC) -m32 $(HOST_CFLAGS) -I$(INC_DIR) -I$(INC_DIR_EXTRA) -c $< -o $@

