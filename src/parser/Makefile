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
#  
#  Authors: Thierry Lepley
##################################################################

##################################################################
#   Compilation of ANTLR grammar files
##################################################################

# Sanity check
ifneq ($(MAKECMDGOALS),clean)
  ifeq ($(ROOT_DIR),)
    $(error ROOT_DIR variable must be defined)
  endif
endif

GENDIR = $(ROOT_DIR)/parser-build/parser
ANTLR_PATH = $(ROOT_DIR)/lib/antlr-3.4-complete.jar
SRCDIR = .

# Useful defines
ANTLR = java -classpath $(ANTLR_PATH) org.antlr.Tool


# Project definition
GENSRC = $(GENDIR)/DeviceConfigLexer.java $(GENDIR)/DeviceConfigParser.java $(GENDIR)/DeviceConfig.tokens \
	$(GENDIR)/KernelGeniusLexer.java $(GENDIR)/KernelGeniusParser.java $(GENDIR)/KernelGenius.tokens \
	$(GENDIR)/KernelGeniusEmitter.java $(GENDIR)/KernelGeniusEmitter.tokens

###################################################################
#     General rules
###################################################################

.PHONY : all clean

all : $(GENSRC)

clean : 
	rm -f $(GENSRC)


###################################################################
# Parser generation rules
###################################################################
#
# Note 1:
# ------
# We have to put a link to 'StdCParser.g' and 'GnuCTreeParser.g' in
# the build directory because ANTLR, when -glib option is set, looks
# for '<name>TokenTypes.txt' in the same directory as the .g file
#
# Note 2:
# ------
# The ANTLR driver seems to update output files only when they have
# changed. It means that the execution of ANTLR may not necessarily
# end up to the time stamp modification of files. To inform the makefile
# that they are up-to-date, we force a new time stamp after each call
# to ANTLR 
#


$(GENDIR)/KernelGenius.tokens $(GENDIR)/KernelGeniusLexer.java $(GENDIR)/KernelGeniusParser.java : $(SRCDIR)/KernelGenius.g
	mkdir -p $(GENDIR)
	$(ANTLR) -fo $(GENDIR) $<

 $(GENDIR)/KernelGeniusEmitter.tokens $(GENDIR)/KernelGeniusEmitter.java : $(SRCDIR)/KernelGeniusEmitter.g
	mkdir -p $(GENDIR)
	$(ANTLR) -fo $(GENDIR) $<

$(GENDIR)/DeviceConfig.tokens $(GENDIR)/DeviceConfigLexer.java $(GENDIR)/DeviceConfigParser.java : $(SRCDIR)/DeviceConfig.g
	mkdir -p $(GENDIR)
	$(ANTLR) -fo $(GENDIR) $<




