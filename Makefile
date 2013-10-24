SHELL=bash

.PHONY: build clean package test

build:	
	ant -f $(CURDIR)/build.xml install

clean:
	ant -f $(CURDIR)/build.xml clean

package:
	script/INSTALL

test:
	cd test && runtest -testset=tests_sthorm.xml
