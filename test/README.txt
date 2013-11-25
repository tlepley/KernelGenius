##################################################################
# KernelGenius test suite
##################################################################

Note: Prior to test execution, do not forget to source
      setup.sh in the KernelGenius package root directory


=======================================
+ GPU (Nvidia)
=======================================
runtest -testset=tests_gpu.xml 


=======================================
+ CPU (Intel, AMD)
=======================================

- To test on Intel and AMD cpu
runtest -testset=tests_cpu.xml 

- To test offline OpenCL compilation only (Intel)
runtest -conf=offline -testset=tests_cpu.xml 

- To test online OpenCL compilation only (Intel/AMD)
runtest -conf=online -testset=tests_cpu.xml 


=======================================
+ STHORM many-core (STMicroelectronics)
=======================================

- Fast posix simulator
runtest -conf=posix -testset=tests_sthorm.xml 

- Instruction accurate simulator
runtest -conf=xp70 -testset=tests_sthorm.xml 

- STHORM board
runtest -conf=xp70 -testset=tests_sthorm.xml 
