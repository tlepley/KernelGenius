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

/* This is the test of a 'Sobel Filter' */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/time.h>

#include <CL/cl.h>
#include "kg_ocl_runtime.h"

#include <Sobel.h>

static char * OPENCL_PLT_VENDOR = NULL;

#ifdef DATA_INT
#define DATA_TYPE cl_int
#else
#define DATA_TYPE cl_float
#define PRECISION 0.01
#endif

#define MIN(a,b) ((a)<(b)?(a):(b))
#define MAX(a,b) ((a)>(b)?(a):(b))
#define ABS(a) ((a)<0?-(a):(a))

/* Border mode */
enum BORDER_MODE { CONST_VALUE, DUPLICATE, MIRROR, SKIP, UNDEF};

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

// Reference code
#define ELEM(t,y,x) ({ \
 int value=0; \
      if (border==CONST_VALUE)      { if (((x)<0||(x)>=IMAGE_X) || ((y)<0)||(y)>=IMAGE_Y) {value=constBorderValue;} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=IMAGE_Y)?IMAGE_Y-1:(y))][(x)<0?0:(((x)>=IMAGE_X)?IMAGE_X-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=IMAGE_Y)?2*IMAGE_Y-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=IMAGE_X)?2*IMAGE_X-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})



void computeSobel3x3(DATA_TYPE *output,
		     DATA_TYPE *input,
		     int IMAGE_X, int IMAGE_Y,
		     enum BORDER_MODE border
		     ) {
  int x,y;
  DATA_TYPE (*out)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
  DATA_TYPE (*in) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;

  float filterPattern_X[9] = {-1.,0.,1., -2.,0. ,2., -1.,0.,1.};
  float filterPattern_Y[9] = {-1.,-2.,-1., 0.,0.,0., 1.,2.,1.};
  
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  if ((border==SKIP) || (border==UNDEF)) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
      DATA_TYPE gx=filterPattern_X[0]*ELEM(in,y-1,x-1) +
	 filterPattern_X[1]*ELEM(in,y-1,x)   +
	 filterPattern_X[2]*ELEM(in,y-1,x+1) +
	 filterPattern_X[3]*ELEM(in,y,x-1)  +
	 filterPattern_X[4]*ELEM(in,y,x)    +
	 filterPattern_X[5]*ELEM(in,y,x+1)  +
	 filterPattern_X[6]*ELEM(in,y+1,x-1) +
	 filterPattern_X[7]*ELEM(in,y+1,x)   +
	 filterPattern_X[8]*ELEM(in,y+1,x+1) ;
      
      DATA_TYPE gy=filterPattern_Y[0]*ELEM(in,y-1,x-1) +
	 filterPattern_Y[1]*ELEM(in,y-1,x)   +
	 filterPattern_Y[2]*ELEM(in,y-1,x+1) +
	 filterPattern_Y[3]*ELEM(in,y,x-1)   +
	 filterPattern_Y[4]*ELEM(in,y,x)     + 
	 filterPattern_Y[5]*ELEM(in,y,x+1)   +
	 filterPattern_Y[6]*ELEM(in,y+1,x-1) +
	 filterPattern_Y[7]*ELEM(in,y+1,x)   +
	 filterPattern_Y[8]*ELEM(in,y+1,x+1) ;
      
      (*out)[y][x] = sqrt( gx*gx + gy*gy );
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
  -border <mode> : border mode\n\
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
  cl_program program = createSobelProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createSobelProgramFromSource( context, device,
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
  cl_mem inputBuffer,outputBuffer;
  DATA_TYPE *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&inputBuffer);
  DATA_TYPE *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&outputBuffer);
  DATA_TYPE *check_output   = malloc(sizeof(DATA_TYPE)*(IMAGE_Y)*(IMAGE_X));
  
  /* For using arrays instead of pointers */
  DATA_TYPE (*in)[IMAGE_Y][IMAGE_X]  =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  DATA_TYPE (*out)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
  DATA_TYPE (*check_out)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])check_output;
  
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
  cl_kernel kernel = createKernel_Sobel3x3(program);

  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_Sobel3x3(kernel,
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
  status = clWaitForEvents(2,&unmap_event[0]);


  /* Enqueue a kernel run call */
  cl_event event;
#ifdef DATA_INT
  //printf("-> Launching OpenCL kernel execution (int)\n");
#else
  //printf("-> Launching OpenCL kernel execution (float)\n");
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
				    2,&unmap_event[0],
				    &event);
    oclCheckStatus(status,"clEnqueueNDRangeKernel failed.");
    status = clWaitForEvents(1, &event);
    
    gettimeofday(&end, NULL);
    printf("** OpenCL 'Sobel' has completed. ** \n\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  /* Get back the output buffer from the device memory (blocking read) */
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer output failed.");\


  //==================================================================
  // Check
  //==================================================================

  {
	input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
	oclCheckStatus(status,"clEnqueueMapBuffer input failed.");\

	struct timeval start, end;
    gettimeofday(&start, NULL);
    
    // Compute reference data
    computeSobel3x3(check_output,input,IMAGE_X,IMAGE_Y,BORDER);
    
    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
  
  in =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  out =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])output;
  
  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;
  if ((BORDER==UNDEF)) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
#ifdef DATA_INT
      if ( (*out)[y][x] != (*check_out)[y][x] ) {
	if (!nok) {
	  printf("First error : [%d , %d]  %d <> %d\n",x,y,(*out)[y][x],(*check_out)[y][x] );
	}
#else
      float diff=(*out)[y][x]-(*check_out)[y][x];
      if ( ABS(diff) > PRECISION ) {
	if (!nok) {
          printf("First error : [%d, %d]  %f <> %f\n",x,y,(*out)[y][x],(*check_out)[y][x] );
        }
#endif
        nok=1;
      }
    }
  }
  
  if (nok) {
    printf("ERROR on SOBEL filter verification !\n");
    // Input Matrix
    printf("\n");
    printf("Input sample \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
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
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
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
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
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
     printf("SOBEL filter completed OK\n");		  
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
