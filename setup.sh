#!/bin/bash

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
# Setup the KernelGenius SDK environment
##################################################################

export KERNELGENIUS_VER=2013.1

# Assume base package dir is one level up from this script
export KERNELGENIUS_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"

export PATH=$KERNELGENIUS_DIR/bin:$KERNELGENIUS_DIR/test/tools/bin:$PATH


if [ ! -n "$P2012_PKG_DIR" ]; then
   echo "WARNING : environment not yet configured for the STHORM sdk"
fi
