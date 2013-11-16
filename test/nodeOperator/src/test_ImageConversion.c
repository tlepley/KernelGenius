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

/* This is the test of a image processing with a complex
   data type (struct) as base element. */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include <CL/cl.h>
#include "kg_ocl_runtime.h"

#include <ImageConversion.h>

#define fMIN(a,b) ((a)<(b)?(a):(b))
#define fMAX(a,b) ((a)>(b)?(a):(b))
#define fABS(a) ((a)<0?-(a):(a))
#define ABS(a) ((a)<0?-(a):(a))

/* Default Image dimensions */
static int IMAGE_X = 512;  /* x dimension of images */
static int IMAGE_Y = 512;  /* y dimension of images */

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;

typedef struct { unsigned char R ; unsigned char G ; unsigned char B ; } RGB ; 
typedef struct { unsigned char Y ; unsigned char U ; unsigned char V ; } YUV ; 


YUV computeRGB2YUV(RGB in) {
  YUV out;
  out.Y =  (0.257f * in.R) + (0.504f * in.G) + (0.098f * in.B) + 16;
  out.U = -(0.148f * in.R) - (0.291f * in.G) + (0.439f * in.B) + 128;
  out.V =  (0.439f * in.R) - (0.368f * in.G) - (0.071f * in.B) + 128;
  return out;
}

void computeImage(YUV *output,
		  RGB *input,
		  int IMAGE_X, int IMAGE_Y
		  ) {
  int x,y;
  YUV (*out) [IMAGE_Y][IMAGE_X] =(YUV (*)[IMAGE_Y][IMAGE_X])output;
  RGB (*in) [IMAGE_Y][IMAGE_X] =(RGB (*)[IMAGE_Y][IMAGE_X])input;

  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN;y<Y_MAX;y++) {
    for(x=X_MIN;x<X_MAX;x++) {
      (*out)[y][x]= computeRGB2YUV((*in)[y][x]);
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
  cl_program program = createImageConversionProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createImageConversionProgramFromSource( context, device,NULL);
#endif
  
  
  //==================================================================
  // Create the kernel, configure it and prepare input data
  //==================================================================
  //printf("-> Create Input/Output Buffer\n");
  /* Create an input/output buffers mapped in the host address space */
  cl_mem inputBuffer, outputBuffer;

  RGB *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(RGB),IMAGE_X*IMAGE_Y,&inputBuffer);
  YUV *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(YUV),IMAGE_X*IMAGE_Y,&outputBuffer);
  YUV *check_output = malloc(sizeof(YUV)*IMAGE_Y*IMAGE_X);
  
  /* For using arrays instead of pointers */
  RGB (*in)[IMAGE_Y][IMAGE_X]  =(RGB (*)[IMAGE_Y][IMAGE_X])input;
  YUV (*out)[IMAGE_Y][IMAGE_X]	     =(YUV (*)[IMAGE_Y][IMAGE_X])output;
  YUV (*check_out)[IMAGE_Y][IMAGE_X] =(YUV (*)[IMAGE_Y][IMAGE_X])check_output;
  
  /* Initializes input images */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in)[y][x].R = (rand()&0xFF);
      (*in)[y][x].G = (rand()&0xFF);
      (*in)[y][x].B = (rand()&0xFF);
    }
  }
  
  /* Initializes output images */
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*out)[y][x].Y = (*check_out)[y][x].Y =(rand()&0xFF) ;
      (*out)[y][x].U = (*check_out)[y][x].U =(rand()&0xFF) ;
      (*out)[y][x].V = (*check_out)[y][x].V =(rand()&0xFF) ;
    }
  }
  
  /* Get a kernel object */
  //printf("-> Create Kernel\n");
  cl_kernel kernel = createKernel_RGB2YUV(program);
  
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_RGB2YUV(kernel,
			NB_WG0, NB_WG1, NB_WI,
			outputBuffer,
			IMAGE_X,
			IMAGE_Y,
			inputBuffer
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

  /* Synchronize buffers between host and device*/
  //printf("-> Synchro buffers\n");
  cl_event unmap_event[2];
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer,input,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input failed.");
  status=clEnqueueUnmapMemObject(commandQueue,outputBuffer,output,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject output failed.");


  /* Enqueue a kernel run call */
  cl_event event;
  printf("-> Launching OpenCL kernel execution)\n");
  
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
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(YUV)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer output failed.");\

  printf("** OpenCL 'ImageConversion' has completed. ** \n\n");


  //==================================================================
  // Check results
  //==================================================================

  {
	input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(RGB)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
	oclCheckStatus(status,"clEnqueueMapBuffer input failed.");\
 
    struct timeval start, end;
    gettimeofday(&start, NULL);

    // Compute reference data
    computeImage(check_output,input,IMAGE_X,IMAGE_Y);
  

    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  in =(RGB (*)[IMAGE_Y][IMAGE_X])input;
  out =(YUV (*)[IMAGE_Y][IMAGE_X])output;

  // Check results
  int nb_errors=0;
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
      if ( ((*out)[y][x].Y != (*check_out)[y][x].Y) ||
	   ((*out)[y][x].U != (*check_out)[y][x].U) ||
	   ((*out)[y][x].V != (*check_out)[y][x].V)
	   ) {
	nb_errors++;
	if (!nok) {
	  printf("[%d,%d]  %d,%d,%d <> %d,%d,%d (in =%d,%d,%d)\n",x,y,
		 (*out)[y][x].Y,(*out)[y][x].U,(*out)[y][x].V,\
		 (*check_out)[y][x].Y,(*check_out)[y][x].U,(*check_out)[y][x].V,
		 (*in)[y][x].R,(*in)[y][x].G,(*in)[y][x].B);
	}
	// Check of the error is compatible with the expectations
	if (
	    ABS((*out)[y][x].Y-(*check_out)[y][x].Y)>1 ||
	    ABS((*out)[y][x].U-(*check_out)[y][x].U)>1 ||
	    ABS((*out)[y][x].V-(*check_out)[y][x].V)>1 
	    ) {
	  // Real error
	  nok=1;
	  printf("-> this is an error");
	}
      }
    }
  }
  
  if (nok) {
    printf("ERROR on ImageConversion filter verification !\n");
    // Input Matrix
    printf("\n");
    printf("Input sample \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d,%d,%d\t",(*in)[y][x].R,(*in)[y][x].G,(*in)[y][x].B);
      }
      printf("\n");
    }
    printf("\n");

    
    // Out Matrix 
    printf("Output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d,%d,%d\t",(*out)[y][x].Y,(*out)[y][x].U,(*out)[y][x].V);
      }
      printf("\n");
    }
    printf("\n");
    
    // Check Out Matrix 
    printf("Check output sample : \n");
    for(y=0;y<fMIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<fMIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d,%d,%d\t",(*check_out)[y][x].Y,(*check_out)[y][x].U,(*check_out)[y][x].V);
      }
      printf("\n");
    }
    printf("\n");
  }
  else {
     printf("ImageConversion kernel completed OK\n");		  
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
