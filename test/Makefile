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
	@echo "************          STHORM Posix-Posix             ************ "
	@echo "*****************************************************************"
	runtest -testset=tests_sthorm.xml

	@echo
	@echo "*****************************************************************"
	@echo "************          STHORM Posix-XP70              ************ "
	@echo "*****************************************************************"
	runtest -testset=tests_sthorm_xp70.xml


cleanall : clean
	@echo "* Full cleanup of the test suite"
	@cd basicOperations; make cleanall
	@cd graph; make cleanall
	@cd nodeFilter; make cleanall
	@cd nodeOperator; make cleanall
	@cd nodeConvolution; make cleanall
	@cd language; make cleanall
	-rm -rf report target
	-rm -f testrun.log testrun.sum
	-rm -rf *~

