#**
#   Makefile running all tests
#   
#   Redistribution of this file to outside parties is
#   strictly prohibited without the written consent
#   of the module owner indicated below.\n
#
#   \par  Module owner: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#
#   \par  Copyright STMicroelectronics (C) 2012-2013
#
#   \par  Authors: 
#   Thierry Lepley, STMicroelectronics (thierry.lepley@st.com)
#**

.PHONY: all build run clean


.PHONY: all build run test clean cleanall


run :
	runtest

test:
	@echo
	@echo "*****************************************************************"
	@echo "************            Posix-Posix                  ************ "
	@echo "*****************************************************************"
	runtest
	@echo
	@echo "*****************************************************************"
	@echo "************             Posix-XP70                  ************ "
	@echo "*****************************************************************"
	runtest -testset=tests_xp70.xml

clean :
	@echo
	@echo "*****************************************************************"
	@echo "************             Sobel Graph                 ************ "
	@echo "*****************************************************************"
	$(MAKE) -f SobelGraph.mk clean
	@echo "*****************************************************************"
	@echo "************       Separable convolution Graph       ************ "
	@echo "*****************************************************************"
	$(MAKE) -f SeparableConvolution.mk clean
	@echo "*****************************************************************"
	@echo "************               Upsampling                ************ "
	@echo "*****************************************************************"
	$(MAKE) -f Upsampling.mk clean
	@echo "*****************************************************************"
	@echo "************                   DoG                   ************ "
	@echo "*****************************************************************"
	$(MAKE) -f DoG.mk clean


cleanall : clean
	@echo "* Removing build directories for all targets"
	rm -rf build_*
	rm -rf target
	rm -rf *~
