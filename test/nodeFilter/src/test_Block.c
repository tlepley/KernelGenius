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

/* This is the test of a block based algorithm */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <sys/time.h>

#include <CL/cl.h>
#include <common_ocl.h>

#include "Block.h"

#define MIN(a,b) ((a)<(b)?(a):(b))
#define ABS(a) ((a)<0?-(a):(a))

/* Border mode */
enum BORDER_MODE { CONST_VALUE, DUPLICATE, MIRROR, SKIP, UNDEF};

/* Result checking accuracy */
#define PRECISION 0.00008f

/* Default Image dimensions */
static int IMAGE_X = 512;
static int IMAGE_Y = 512;

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;

/* Border configuration */
static enum BORDER_MODE BORDER=UNDEF;
#ifdef DATA_INT
static int constBorderValue=0;
#else
static float constBorderValue=0.0f;
#endif

#define ELEM(t,y,x) ({ \
 int value=0; \
      if (border==CONST_VALUE)      { if (((x)<0||(x)>=IMAGE_X) || ((y)<0)||(y)>=IMAGE_Y) {value=constBorderValue;} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=IMAGE_Y)?IMAGE_Y-1:(y))][(x)<0?0:(((x)>=IMAGE_X)?IMAGE_X-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=IMAGE_Y)?2*IMAGE_Y-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=IMAGE_X)?2*IMAGE_X-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})

// Reference code
void computeBlock(float      *output,
		  float const *input,
		  int w, int h,
		  enum BORDER_MODE border) {
  cl_float (*in)[h][w]      =(cl_float (*)[h][w])input;
  cl_float (*out)[((h+3)/4)*2][((w+3)/4)*2] =(cl_float (*)[((h+3)/4)*2][((w+3)/4)*2])output;
  
  int x,y;
  for (y=0;y<((h+3)/4);y++) {
    for (x=0;x<((w+3)/4);x++) {
      (*out)[2*y+0][2*x+0]=0.125*(ELEM(in,4*y+0,4*x+0)+ELEM(in,4*y+0,4*x+1)+ELEM(in,4*y+0,4*x+2)+ELEM(in,4*y+0,4*x+3)+ELEM(in,4*y+1,4*x+0)+ELEM(in,4*y+1,4*x+1)+ELEM(in,4*y+1,4*x+2)+ELEM(in,4*y+1,4*x+3));
      (*out)[2*y+1][2*x+0]=0.125*(ELEM(in,4*y+2,4*x+0)+ELEM(in,4*y+2,4*x+1)+ELEM(in,4*y+2,4*x+2)+ELEM(in,4*y+2,4*x+3)+ELEM(in,4*y+3,4*x+0)+ELEM(in,4*y+3,4*x+1)+ELEM(in,4*y+3,4*x+2)+ELEM(in,4*y+3,4*x+3));
      (*out)[2*y+0][2*x+1]=0.125*(ELEM(in,4*y+0,4*x+0)+ELEM(in,4*y+1,4*x+0)+ELEM(in,4*y+2,4*x+0)+ELEM(in,4*y+3,4*x+0)+ELEM(in,4*y+0,4*x+1)+ELEM(in,4*y+1,4*x+1)+ELEM(in,4*y+2,4*x+1)+ELEM(in,4*y+3,4*x+1));
      (*out)[2*y+1][2*x+1]=0.125*(ELEM(in,4*y+0,4*x+2)+ELEM(in,4*y+1,4*x+2)+ELEM(in,4*y+2,4*x+2)+ELEM(in,4*y+3,4*x+2)+ELEM(in,4*y+0,4*x+3)+ELEM(in,4*y+1,4*x+3)+ELEM(in,4*y+2,4*x+3)+ELEM(in,4*y+3,4*x+3));
    }
  }
}


static cl_platform_id   platform;
static cl_device_id     device;
static cl_context       context;
static cl_command_queue commandQueue;


static int TestGradient(cl_program program) {
  cl_int status;
  
  printf("** Setting up of the 'Block' kernel\n");
  
  /* Get the kernel */
  cl_kernel kernel = createKernel_Block(program);
  
  
  //==================================================================
  // Create buffers
  //==================================================================
  
  cl_mem inputBuffer,outputBuffer;
  cl_float *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_float),IMAGE_X*IMAGE_Y,&inputBuffer);
  cl_float *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_WRITE_ONLY,CL_MAP_READ,sizeof(cl_float),(((IMAGE_X+3)/4)*2)*(((IMAGE_Y+3)/4)*2),&outputBuffer);
  cl_float *check_output = malloc(sizeof(cl_float)*(((IMAGE_Y+3)/4)*2)*(((IMAGE_X+3)/4)*2));
  
  /* For using arrays instead of pointers */
  cl_float (*in)[IMAGE_Y][IMAGE_X] =(cl_float (*)[IMAGE_Y][IMAGE_X])input;
  cl_float (*out)[((IMAGE_Y+3)/4)*2][((IMAGE_X+3)/4)*2]	=(cl_float (*)[((IMAGE_Y+3)/4)*2][((IMAGE_X+3)/4)*2])output;
  cl_float (*check_out)[((IMAGE_Y+3)/4)*2][((IMAGE_X+3)/4)*2] =(cl_float (*)[((IMAGE_Y+3)/4)*2][((IMAGE_X+3)/4)*2])check_output;
  
  /* Create an input buffer */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in)[y][x]=rand()&0xff; // Get not too big ints
    }
  }
  
  
  //==================================================================
  // Setup kernel arguments
  //==================================================================
  
  // Set kernel arguments
  setKernelArgs_Block(kernel,
		      NB_WG0, NB_WG1, NB_WI,
		      outputBuffer,
		      IMAGE_X,IMAGE_Y,
		      inputBuffer
		      );
  
  
  //==================================================================
  // Execute the kernel on the device (ND-Range)
  //==================================================================
  
  size_t globalThreads[2]= {NB_WI*NB_WG0,NB_WG1};
  size_t localThreads[2] = {NB_WI,1};
  
  /* Check Compatibility of kernel/NDRange with the device */
  checkNDRangeWithDevice(device,kernel,
			 2,globalThreads,localThreads);
  
  
  /* Synchronize buffers between host and device*/
  cl_event unmap_event[2];
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer,input,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input failed.");
  status=clEnqueueUnmapMemObject(commandQueue,outputBuffer,output,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input failed.");
  status = clWaitForEvents(2,&unmap_event);


  /* Enqueue a kernel run call */
  printf("** Execution of 'Block' kernel\n");
  cl_event event;
  {
    struct timeval start, end;
    gettimeofday(&start, NULL);
    
    status = clEnqueueNDRangeKernel(commandQueue,
				    kernel,
				    2,  // dimensions
				    NULL,  // no offset
				    globalThreads,
				    localThreads,
				    2,&unmap_event,
				    &event);
    oclCheckStatus(status,"clEnqueueNDRangeKernel failed.");
    status = clWaitForEvents(1, &event);
    
    gettimeofday(&end, NULL);
    printf("** The 'Block' kernel execution has completed.\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  /* Get back the output buffer from the device memory (blocking read) */
  output = clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_float)*((IMAGE_X+3)/4)*2*((IMAGE_Y+3)/4)*2,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer failed.");\
  
  //==================================================================
  // Check results
  //==================================================================
  
  {
    input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_float)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
    oclCheckStatus(status,"clEnqueueMapBuffer input failed.");	\

    struct timeval start, end;
    gettimeofday(&start, NULL);
    
    // Compute result from the reference code
    computeBlock((float*)check_output,(float*)input,IMAGE_X,IMAGE_Y,BORDER);
    
    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  in  =(cl_float (*)[IMAGE_Y][IMAGE_X])input;
  out =(cl_float (*)[((IMAGE_Y+3)/4)*2][((IMAGE_X+3)/4)*2])output;

  // Check reference vs OCL
  int nok=0;int first=1;
  for(y=0;y<((IMAGE_Y+3)/4)*2;y++) {
    for(x=0;x<((IMAGE_X+3)/4)*2;x++) {
      if (ABS((*out)[y][x]-(*check_out)[y][x])>PRECISION) {
	if (first) {
	  printf("!! 'Block' : first error [%d,%d], %f instead of %f\n",x,y,(*out)[y][x],(*check_out)[y][x]);
	  first=0;
	}
	nok=1;
      }
    }
  }
  
  // Final message
  if (nok) {
    /* Print a sample of the input matrix */
    printf("\n");
    printf("Input Matrix sample\n");
    for(y=0;y<MIN(8,IMAGE_Y);y++) {
      for(x=0;x<MIN(8,IMAGE_X);x++) {
	printf(" %f",(*in)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    /* Print a sample of the output matrix */
    printf("Output Matrix sample\n");
    for(y=0;y<MIN(8/2,((IMAGE_Y+3)/4)*2);y++) {
      for(x=0;x<MIN(8/2,((IMAGE_X+3)/4)*2);x++) {
	printf(" %f", (*out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    /* Print a sample of the correct output matrix */
    printf("Correct output Matrix sample\n");
    for(y=0;y<MIN(8/2,((IMAGE_Y+3)/4)*2);y++) {
      for(x=0;x<MIN(8/2,((IMAGE_X+3)/4)*2);x++) {
	printf(" %f", (*check_out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    printf("!! 'Block' completed with ERROR\n");
  }
  else {
    printf(">> 'Block' completed OK\n");
  }
  

  //==================================================================
  // Termination
  //==================================================================

  clReleaseEvent(event);
  clReleaseKernel(kernel);
  clReleaseProgram(program);

  //printf("-> OCL objects released\n");

  if (nok) {
    return 1;
  }
  else {
    return 0;
  }
}


void printUsage(char *s) {
  printf("usage: %s [option]*\n",s);
  printf("\
options :\n\
  -h or --help : display this help\n\
  -x <num> : width of the matrix\n\
  -y <num> : height of the matrix\n\
  -border <mode> : border mode\n\
  -wi <num> : number of work-items per work-groups\n\
  -wg <num> : number of work-groups\n\
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
    else if ((strcmp(argv[i],"-border")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      if (strcmp(argv[i],"duplicate")==0) {
	BORDER=DUPLICATE;
      }
      else if (strcmp(argv[i],"mirror")==0) {
	BORDER=MIRROR;
      }
      else if (strcmp(argv[i],"skip")==0) {
	BORDER=SKIP;
      }
      else if (strcmp(argv[i],"undef")==0) {
	BORDER=UNDEF;
      }
      else {
	// It should be a const integer
	char *endptr;
	
#ifdef DATA_INT
	int val=strtol(argv[i], &endptr, 10);
#else
	float val=strtof(argv[i], &endptr);
#endif
	if (endptr!=argv[i]+strlen(argv[i])) {
	  fprintf(stderr,"error : unknown border mode '%s'\n",argv[i]);
	  exit(1);
	}
	BORDER=CONST_VALUE;
	constBorderValue=val;
      }
    }
    else {
      fprintf(stderr,"error : unknown option '%s'\n",argv[i]);
      exit(1);
    }
  }
}

int main(int argc, char * argv[]) {
  int status;

  // Manage options
  processOptions(argc,argv);
  
  // Print configuration
  printf("** Configuration **\n");
  printf("  - Image dimensions : [%d,%d]\n",IMAGE_X,IMAGE_Y);
  printf("  - Border mode: ");
  switch(BORDER) {
  case CONST_VALUE:
#ifdef DATA_INT
    printf("const (%d)",constBorderValue);
#else
    printf("const (%f)",constBorderValue);
#endif
    break;
  case DUPLICATE:
    printf("duplicate");
    break;
  case MIRROR:
    printf("mirror");
    break;
  case SKIP:
    printf("skip");
    break;
  case UNDEF:
    printf("undef");
    break;
  }
  printf("\n");
  printf("  - Nb work-items per work-group: %d\n",NB_WI);
  printf("  - Nb work-groups: [%d,%d]\n",NB_WG0, NB_WG1);
  printf("\n");


  printf("** OpenCL global setup\n");

  /* OpenCL setup */
  /* ------------ */
  platform    = oclGetFirstPlatform();
  device      = oclGetFirstDevice(platform);
  context     = oclCreateContext(platform,device);
  commandQueue= oclCreateCommandQueueOOO(context,device);

  //==================================================================
  // Compile the program
  //==================================================================

  // Initialization of gradient kernels (load/compile program)
#ifdef AHEAD_OF_TIME
  cl_program program = createBlockProgramFromBinary(context,device);
#else
  cl_program program = createBlockProgramFromSource(context,device,NULL);
#endif
  
  //==================================================================
  // Check kernels
  //==================================================================
  // Gradient
  status = TestGradient(program);

  //==================================================================
  // Termination
  //==================================================================

  clReleaseCommandQueue(commandQueue);
  clReleaseContext(context);

#ifdef __P2012__
  clUnloadRuntime();
#endif

  return status;
}
