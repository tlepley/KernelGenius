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
#   Makefile running all tests
##################################################################

.PHONY: all build run test clean cleanall

run :
	runtest -testset=tests_sthorm.xml

test:
	@echo
	@echo "*****************************************************************"
	@echo "************        Compilation tests                ************ "
	@echo "*****************************************************************"
	runtest

clean :
	@echo
	@echo "*****************************************************************"
	@echo "************                struct                   ************ "
	@echo "*****************************************************************"
	$(MAKE) -f struct.mk clean
	@echo "*****************************************************************"
	@echo "************                 enum                    ************ "
	@echo "*****************************************************************"
	$(MAKE) -f enum.mk clean
	@echo "*****************************************************************"
	@echo "************                typedef                  ************ "
	@echo "*****************************************************************"
	$(MAKE) -f typedef.mk clean


cleanall : clean
	@echo "* Removing build directories for all targets"
	rm -rf build_*
	rm -rf target
	rm -rf *~
