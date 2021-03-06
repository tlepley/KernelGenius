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

/* This is the test of the Operator node */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include <CL/cl.h>
#include "kg_ocl_runtime.h"

#include <Threshold.h>

static char * OPENCL_PLT_VENDOR = NULL;

#ifdef DATA_INT
#define DATA_TYPE cl_int
#else
#define DATA_TYPE cl_float
#define PRECISION 0.01
#endif

#define fMIN(a,b) ((a)<(b)?(a):(b))
#define fMAX(a,b) ((a)>(b)?(a):(b))
#define fABS(a) ((a)<0?-(a):(a))

/* Default Image dimensions */
static int IMAGE_X = 512;  /* x dimension of images */
static int IMAGE_Y = 512;  /* y dimension of images */

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;

static int THRESHOLD=15.5f;


int computeThreshold(DATA_TYPE a, DATA_TYPE t) {
  return ((a>=t)?1:0);
}

void computeImage(int *output,
		  DATA_TYPE *input,
		  DATA_TYPE threshold,
		  int IMAGE_X, int IMAGE_Y
		  ) {
  int x,y;
  int (*out) [IMAGE_Y][IMAGE_X] =(int (*)[IMAGE_Y][IMAGE_X])output;
  DATA_TYPE (*in) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;
  
  for(y=Y_MIN;y<Y_MAX;y++) {
    for(x=X_MIN;x<X_MAX;x++) {
      (*out)[y][x]= computeThreshold((*in)[y][x],threshold);
    }
  }
}


void printUsage(char *s) {
  printf("usage: %s [option]*\n",s);
  printf("\
options :\n\
  -h or --help : display this help\n\
  -vendor <name> : vendor name\n\
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
    else if ((strcmp(argv[i],"-vendor")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      OPENCL_PLT_VENDOR=argv[i];
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
  printf("  - Nb work-items per work-group: %d\n",NB_WI);
  printf("  - Nb work-groups: [%d,%d]\n",NB_WG0, NB_WG1);
  printf("\n");

  
//==================================================================
  // OpenCL setup
  //==================================================================
  
  //printf("-> OpenCL host setup\n");
  
  /* Get the OpenCL platform and print some infos */
  cl_platform_id platform;
  if (OPENCL_PLT_VENDOR==NULL) {
    platform=oclGetFirstPlatform();
  }
  else {
    platform=oclGetFirstPlatformFromVendor(OPENCL_PLT_VENDOR);
  }
  oclDisplayPlatformInfo(platform);
  
  /* Pickup the first available devices */
  cl_device_id device = oclGetFirstDevice(platform);
  //oclDisplayDeviceInfo(device);
  
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
  cl_program program = createThresholdProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createThresholdProgramFromSource( context, device,
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
  cl_mem inputBuffer, outputBuffer;
  DATA_TYPE *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&inputBuffer);
  cl_int *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(cl_int),IMAGE_X*IMAGE_Y,&outputBuffer);
  int *check_output   = malloc(sizeof(int)*IMAGE_Y*IMAGE_X);
  
  /* For using arrays instead of pointers */
  DATA_TYPE (*in)[IMAGE_Y][IMAGE_X]  =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  int (*out)[IMAGE_Y][IMAGE_X]	     =(int (*)[IMAGE_Y][IMAGE_X])output;
  int (*check_out)[IMAGE_Y][IMAGE_X] =(int (*)[IMAGE_Y][IMAGE_X])check_output;
  
  /* Initializes input images */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
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
#ifdef REVERSE
  cl_kernel kernel = createKernel_Threshold2(program);
#else
  cl_kernel kernel = createKernel_Threshold(program);
#endif
  
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
#ifdef REVERSE
  setKernelArgs_Threshold2(kernel,
#else
  setKernelArgs_Threshold(kernel,
#endif
			  NB_WG0, NB_WG1, NB_WI,
			  outputBuffer,					  
			  IMAGE_X,
			  IMAGE_Y,
#ifdef REVERSE
			  THRESHOLD,
			  inputBuffer
#else
			  inputBuffer,
			  THRESHOLD
#endif
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

  //printf("-> Check NDRange\n");
  /* Check intrinsec kernel and NDRange copatibility with the device */
  checkNDRangeWithDevice(device,kernel,2,globalThreads,localThreads);

  /* Synchronize buffers betzeen host and device*/
  //printf("-> Synchro buffers\n");
  cl_event unmap_event[2];
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer,input,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input failed.");
  status=clEnqueueUnmapMemObject(commandQueue,outputBuffer,output,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject output failed.");


  /* Enqueue a kernel run call */
  cl_event event;
#ifdef DATA_INT
  //printf("-> Launching OpenCL kernel execution (int)\n");
#else
  //printf("-> Launching OpenCL kernel execution (float)\n");
#endif
  
  status = clEnqueueNDRangeKernel(commandQueue,
				  kernel,
				  2,  // dimensions
				  NULL,  // no offset
				  globalThreads,
				  localThreads,
				  //0,NULL,
	       			  2,&unmap_event[0],
				  &event);
  oclCheckStatus(status,"clEnqueueNDRangeKernel failed.");
  
  /* Get back the output buffer from the device memory (blocking read) */
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer output failed.");\

  printf("** OpenCL 'Threshold' has completed. ** \n\n");


  //==================================================================
  // Check results
  //==================================================================

  {
    input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
    oclCheckStatus(status,"clEnqueueMapBuffer input failed.");	\
 
    struct timeval start, end;
    gettimeofday(&start, NULL);

    // Compute reference data
    computeImage(check_output,input, THRESHOLD, IMAGE_X,IMAGE_Y);

    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
  
  in =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  out =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;

  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
      if ( (*out)[y][x] != (*check_out)[y][x] ) {
	if (!nok) {
	  printf("First error : [%d , %d]  %d <> %d (in =%.2f)\n",x,y,(*out)[y][x],(*check_out)[y][x] ,(*in)[y][x]);
	}
        nok=1;
      }
    }
  }
  
  if (nok) {
    printf("ERROR on Threshold filter verification !\n");
    // Input Matrix
    printf("\n");
    printf("Input sample \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t",(*in)[y][x]);
#else
	printf(" %.2f\t",(*in)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");

    
    // Out Matrix 
    printf("Output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t", (*out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    // Check Out Matrix 
    printf("Check output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t", (*check_out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
  }
  else {
     printf("Threshold kernel completed OK\n");		  
  }


  //==================================================================
  // Termination
  //==================================================================
  
  clReleaseMemObject(inputBuffer);
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
