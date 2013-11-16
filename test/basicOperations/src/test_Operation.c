/*
  This file is part of KernelGenius.

  Copyright (C) 2013 STMicroelectronics

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  Lesser General Public License for more details.
 
  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, write to the Free
  Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
  Boston, MA 02110-1301 USA.
  
  Authors: Thierry Lepley
*/

/* This is the test of simple KernelGenius operators */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/time.h>

#include <CL/cl.h>
#include <common_ocl.h>

#include <Operation.h>

#ifdef DATA_INT
#define DATA_TYPE cl_int
#else
#define DATA_TYPE cl_float
#define PRECISION 0.01
#endif

#define fMIN(a,b) ((a)<(b)?(a):(b))
#define fMAX(a,b) ((a)>(b)?(a):(b))
#define fABS(a) ((a)<0?-(a):(a))

/* Border mode */
enum OPERATION_MODE { ABSDIFF, ADD, EUCLIDEANNORM, MIN, MAX, MULT, SUB};

/* Default Image dimensions */
static int IMAGE_X = 512;  /* x dimension of images */
static int IMAGE_Y = 512;  /* y dimension of images */

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;

/* Border configuration */
static enum OPERATION_MODE OPERATION=ADD;


DATA_TYPE compute(DATA_TYPE a, DATA_TYPE b) {
  switch(OPERATION) {
  case ABSDIFF:
    return fABS(a-b);
  case ADD:
    return a+b;
  case EUCLIDEANNORM:
    return sqrt(a*a+b*b);
  case MIN:
    return fMIN(a,b);
  case MAX:
    return fMAX(a,b);
  case MULT:
    return a*b;
  case SUB:
    return a-b;
  default:
    fprintf(stderr,"Error, unknown OPERATION %d\n", OPERATION);
    exit(1);
  }
}

void computeImage(DATA_TYPE *output,
		  DATA_TYPE *input0,
		  DATA_TYPE *input1,
		  int IMAGE_X, int IMAGE_Y
		  ) {
  int x,y;
  DATA_TYPE (*out) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
  DATA_TYPE (*in0) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input0;
  DATA_TYPE (*in1) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input1;
  
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;
  
  for(y=Y_MIN;y<Y_MAX;y++) {
    for(x=X_MIN;x<X_MAX;x++) {
      (*out)[y][x]= compute((*in0)[y][x],(*in1)[y][x]);
    }
  }
}


void printUsage(char *s) {
  printf("usage: %s [option]*\n",s);
  printf("\
options :\n\
  -h or --help : display this help\n\
  -x <num> : width of the matrix\n\
  -y <num> : height of the matrix\n\
  -operation <mode> : compute operation\n\
  -wi <num> : number of work-items per work-groups\n\
  -wg0 <num> : number of work-groups, x-axis\n\
  -wg1 <num> : number of work-groups, y-axis\n\
");
  exit(0);
}

void processOptions(int argc, char *argv[]) {
  int i;
  for(i=1;i<argc;i++) {

    if ((strcmp(argv[i],"-h")==0)||(strcmp(argv[i],"--help")==0)) {
      printUsage(argv[0]);
    }
    else if ((strcmp(argv[i],"-x")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      IMAGE_X=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-y")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      IMAGE_Y=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-wi")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      NB_WI=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-wg0")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      NB_WG0=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-wg1")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      NB_WG1=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-operation")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      if (strcmp(argv[i],"AbsDiff")==0) {
	OPERATION=ABSDIFF;
      }
      else if (strcmp(argv[i],"Add")==0) {
	OPERATION=ADD;
      }
      else if (strcmp(argv[i],"EuclideanNorm")==0) {
	OPERATION=EUCLIDEANNORM;
      }
      else if (strcmp(argv[i],"Min")==0) {
	OPERATION=MIN;
      }
      else if (strcmp(argv[i],"Max")==0) {
	OPERATION=MAX;
      }
      else if (strcmp(argv[i],"Mult")==0) {
	OPERATION=MULT;
      }
      else if (strcmp(argv[i],"Sub")==0) {
	OPERATION=SUB;
      }
      else {
	fprintf(stderr,"error : unknown operation mode '%s'\n",argv[i]);
	exit(1);
      }
    }
    else {
      fprintf(stderr,"error : unknown option '%s'\n",argv[i]);
      exit(1);
    }
  }
}


#define PRINT_SIZE 10

int main(int argc, char * argv[]) {
  // Manage options
  processOptions(argc,argv);
  
  // Print configuration
  printf("** Configuration **\n");
  printf("  - Image dimensions : [%d,%d]\n",IMAGE_X,IMAGE_Y);
  printf("  - Data type: ");
#ifdef DATA_INT
  printf("integer");
#else
  printf("floating point");
#endif
  printf("\n");
  printf("  - Operation mode: ");
  switch(OPERATION) {
  case ABSDIFF:
    printf("AbsDiff");
    break;
  case ADD:
    printf("Add");
    break;
  case EUCLIDEANNORM:
    printf("EuclideanNorm");
    break;
  case MIN:
    printf("Min");
    break;
  case MAX:
    printf("Max");
    break;
  case MULT:
    printf("Mult");
    break;
  case SUB:
    printf("Sub");
    break;
  }
  printf("\n");
  printf("  - Nb work-items per work-group: %d\n",NB_WI);
  printf("  - Nb work-groups: [%d,%d]\n",NB_WG0, NB_WG1);
  printf("\n");

  
  //==================================================================
  // OpenCL setup
  //==================================================================
  
  //printf("-> OpenCL host setup\n");
  
  /* Get the first OpenCL platform and print some infos */
  cl_platform_id platform = oclGetFirstPlatform();
  oclDisplayPlatformInfo(platform);
  
  /* Pickup the first available devices */
  cl_device_id device = oclGetFirstDevice(platform);
  
  /* Create context */
  //printf("-> Create context\n");
  cl_context context = oclCreateContext(platform,device);
  
  /* Create a command Queue  */
  //printf("-> Create command Queue\n");
  cl_command_queue commandQueue = oclCreateCommandQueue(context, device);
  
  
  //printf("-> Create CL Program\n");
  /* Create and compile the CL program */
#ifdef AHEAD_OF_TIME
  /* create a CL program using the kernel binary */
  cl_program program = createOperationProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createOperationProgramFromSource( context, device,
#ifdef DATA_INT
						     "-DDATA_INT"
#else
						     NULL
#endif
						     );
#endif
  
  
  //==================================================================
  // Create the kernel, configure it and prepare input data
  //==================================================================
  //printf("-> Create Input/Output Buffer\n");
  /* Create an input/output buffers mapped in the host address space */
  cl_mem inputBuffer0,inputBuffer1,outputBuffer;
  DATA_TYPE *input0 = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&inputBuffer0);
  DATA_TYPE *input1 = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&inputBuffer1);
  DATA_TYPE *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_WRITE_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&outputBuffer);
  DATA_TYPE *check_output = malloc(sizeof(DATA_TYPE)*IMAGE_Y*IMAGE_X);
  
  /* For using arrays instead of pointers */
  DATA_TYPE (*in0)[IMAGE_Y][IMAGE_X]  	   =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input0;
  DATA_TYPE (*in1)[IMAGE_Y][IMAGE_X]  	   =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input1;
  DATA_TYPE (*out)[IMAGE_Y][IMAGE_X]	   =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
  DATA_TYPE (*check_out)[IMAGE_Y][IMAGE_X]  =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])check_output;
  
  /* Initializes input images */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in0)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in1)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }

  /* Initializes output images */
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*out)[y][x] = (*check_out)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }

  
  /* Get a kernel object */
  //printf("-> Create Kernel\n");
  cl_kernel kernel = createKernel_Operation(program);
  
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_Operation(kernel,
			  NB_WG0, NB_WG1, NB_WI,
			  outputBuffer,					  
			  IMAGE_X,
			  IMAGE_Y,
			  inputBuffer0,
			  inputBuffer1
			 );



  //==================================================================
  // Execute the kernel on the device
  //==================================================================

  /* -------------------------------------------------------- */
  /* We want an NDRange with one work-item per PE in the      */
  /*                                                          */
  /* Check that the NDRange structure i supported for this    */
  /* kernel.			 			      */
  /* -------------------------------------------------------- */

  size_t globalThreads[2]= {NB_WI*NB_WG0,NB_WG1};
  size_t localThreads[2] = {NB_WI,1};

  cl_int status;

  /* Check intrinsec kernel and NDRange copatibility with the device */
  checkNDRangeWithDevice(device,kernel,2,globalThreads,localThreads);

  /* Synchronize buffers betzeen host and device*/
  //printf("-> Synchro buffers\n");
  cl_event unmap_event[3];
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer0,input0,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input0 failed.");
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer1,input1,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input1 failed.");
  status=clEnqueueUnmapMemObject(commandQueue,outputBuffer,output,0,NULL,&unmap_event[2]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject output failed.");
  status = clWaitForEvents(3,&unmap_event[0]);


  /* Enqueue a kernel run call */
  cl_event event;
#ifdef DATA_INT
  printf("-> Launching OpenCL kernel execution (int)\n");
#else
  printf("-> Launching OpenCL kernel execution (float)\n");
#endif
  
  {
    struct timeval start, end;
    gettimeofday(&start, NULL);
    
    status = clEnqueueNDRangeKernel(commandQueue,
				    kernel,
				    2,  // dimensions
				    NULL,  // no offset
				    globalThreads,
				    localThreads,
				    //0,NULL,
				    3,&unmap_event[0],
				    &event);
    oclCheckStatus(status,"clEnqueueNDRangeKernel failed.");
    status = clWaitForEvents(1, &event);
    
    gettimeofday(&end, NULL);
    printf("** OpenCL 'Operation' has completed. ** \n\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
  
  
  /* Get back the output buffer from the device memory (blocking read) */
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueReadBuffer failed.");\
  //printf("-> Read buffers\n");


  //==================================================================
  // Check
  //==================================================================

  {
	input0= clEnqueueMapBuffer(commandQueue,inputBuffer0,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
    oclCheckStatus(status,"clEnqueueReadBuffer input0 failed.");\
	input1= clEnqueueMapBuffer(commandQueue,inputBuffer1,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
    oclCheckStatus(status,"clEnqueueReadBuffer input1 failed.");\

	struct timeval start, end;
    gettimeofday(&start, NULL);
    
    // Compute reference data
    computeImage(check_output,input0,input1,IMAGE_X,IMAGE_Y);
    //printf("-> Computed ref\n");

    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
  
   in0 =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input0;
   in1 =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input1;
   out =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
 
  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
#ifdef DATA_INT
      if ( (*out)[y][x] != (*check_out)[y][x] ) {
	if (!nok) {
	  printf("First error : [%d , %d]  %d <> %d\n",x,y,(*out)[y][x],(*check_out)[y][x] );
	}
#else
      float diff=(*out)[y][x]-(*check_out)[y][x];
      if ( fABS(diff) > PRECISION ) {
	if (!nok) {
          printf("First error : [%d, %d]  %f <> %f\n",x,y,(*out)[y][x],(*check_out)[y][x] );
        }
#endif
        nok=1;
      }
    }
  }
  
  if (nok) {
    printf("ERROR on Operation filter verification !\n");
    // Input Matrix
    printf("\n");
    printf("Input0 sample \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t",(*in0)[y][x]);
#else
	printf(" %.2f\t",(*in0)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
    printf("Input1 sample \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t",(*in1)[y][x]);
#else
	printf(" %.2f\t",(*in1)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
    
    // Out Matrix 
    printf("Output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*out)[y][x]);
#else
	printf(" %.2f", (*out)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
    
    // Check Out Matrix 
    printf("Check output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*check_out)[y][x]);
#else
	printf(" %.2f", (*check_out)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
  }
  else {
     printf("Operation filter completed OK\n");		  
  }


  //==================================================================
  // Termination
  //==================================================================

  // Release mapped buffer
  clReleaseMemObject(inputBuffer0);
  clReleaseMemObject(inputBuffer1);
  clReleaseMemObject(outputBuffer);
  clReleaseEvent(event);
  clReleaseKernel(kernel);
  clReleaseProgram(program);
  clReleaseCommandQueue(commandQueue);
  clReleaseContext(context);

  //printf("-> OCL objects released\n");

  // Stop the OCL runtime
#ifdef __P2012__
  clUnloadRuntime();
#endif

  if (nok) {
    return 1;
  }
  else {
    return 0;
  }
}
