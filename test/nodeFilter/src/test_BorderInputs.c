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

/* Test of the Filter with inputs of different read patterns
   as well as different border semantics */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/time.h>

#include <CL/cl.h>
#include "kg_ocl_runtime.h"

#include <BorderInputs.h>

#define MIN(a,b) ((a)<(b)?(a):(b))
#define MAX(a,b) ((a)>(b)?(a):(b))

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
static enum BORDER_MODE BORDER1=UNDEF;
static int constBorderValue1=0;
static enum BORDER_MODE BORDER2=DUPLICATE;
static int constBorderValue2=0;

static int WINDOW1=1;
static int WINDOW2=2;

// Reference code
#define ELEM(t,y,x,border,constBorderValue) ({			\
 int value=0; \
 if (border==CONST_VALUE)      { if (((x)<0||(x)>=IMAGE_X) || ((y)<0)||(y)>=IMAGE_Y) {value=(constBorderValue);} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=IMAGE_Y)?IMAGE_Y-1:(y))][(x)<0?0:(((x)>=IMAGE_X)?IMAGE_X-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=IMAGE_Y)?2*IMAGE_Y-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=IMAGE_X)?2*IMAGE_X-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})

static int lut1x_ref[8] = {1,1,0,-1,-1,-1,0,1};
static int lut1y_ref[8] = {0,1,1,1,0,-1,-1,-1};

static int lut2x_ref[8] = {-1,-1,0,1,1,1,0,-1};
static int lut2y_ref[8] = {0,-1,-1,-1,0,1,1,1};


void computeBorderInputs(int *p_out,
			 char *p_dir, int *p_val,
			 int IMAGE_X, int IMAGE_Y,
			 enum BORDER_MODE border1,
			 enum BORDER_MODE border2,
			 int window1,
			 int window2
			 ) {
  int x,y;
  int i;

  int  (*out)[IMAGE_Y][IMAGE_X] =(int  (*)[IMAGE_Y][IMAGE_X])p_out;
  char (*dir)[IMAGE_Y][IMAGE_X] =(char (*)[IMAGE_Y][IMAGE_X])p_dir;
  int  (*val)[IMAGE_Y][IMAGE_X] =(int  (*)[IMAGE_Y][IMAGE_X])p_val;

  int lut1x[8],lut1y[8],lut2x[8],lut2y[8];
  for(i=0;i<8;i++) {
    lut1x[i]=lut1x_ref[i]*window1;
    lut1y[i]=lut1y_ref[i]*window1;
    lut2x[i]=lut2x_ref[i]*window2;
    lut2y[i]=lut2y_ref[i]*window2;
  }

  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  int WINDOW=0;
  if ((border1==SKIP)||(border1==UNDEF)) { WINDOW=MAX(WINDOW,WINDOW1); }
  if ((border2==SKIP)||(border2==UNDEF)) { WINDOW=MAX(WINDOW,WINDOW2); }
  X_MIN+=WINDOW;X_MAX-=WINDOW;
  Y_MIN+=WINDOW;Y_MAX-=WINDOW;

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {

      int x1=lut1x[ELEM(dir,y,x,border1,constBorderValue1)%8];
      int y1=lut1y[ELEM(dir,y,x,border1,constBorderValue1)%8];
      int x2=lut1x[ELEM(val,y,x,border2,constBorderValue2)%8];
      int y2=lut1y[ELEM(val,y,x,border2,constBorderValue2)%8];

      (*out)[y][x] = ELEM(dir,y+y1,x+x1,border1,constBorderValue1) +
	             ELEM(val,y+y2,x+x2,border2,constBorderValue2);
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
  -border1 <mode> : border mode\n\
  -border2 <mode> : border mode\n\
  -window1 <num>  : window width\n\
  -window2 <num>  : window width\n\
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
    else if ((strcmp(argv[i],"-window1")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      WINDOW1=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-window2")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      WINDOW2=atoi(argv[i]);
    }
    else if ((strcmp(argv[i],"-border1")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      if (strcmp(argv[i],"duplicate")==0) {
	BORDER1=DUPLICATE;
      }
      else if (strcmp(argv[i],"mirror")==0) {
	BORDER1=MIRROR;
      }
      else if (strcmp(argv[i],"skip")==0) {
	BORDER1=SKIP;
      }
      else if (strcmp(argv[i],"undef")==0) {
	BORDER1=UNDEF;
      }
      else {
	// It should be a const integer
	char *endptr;
        int val=strtol(argv[i], &endptr, 10);
	if (endptr!=argv[i]+strlen(argv[i])) {
	  fprintf(stderr,"error : unknown border mode '%s'\n",argv[i]);
	  exit(1);
	}
	BORDER1=CONST_VALUE;
	constBorderValue1=val;
      }
    }
    else if ((strcmp(argv[i],"-border2")==0)) {
      if (i==argc-1) {
	fprintf(stderr,"error : missing number after option '%s'\n",argv[i]);
	exit(1);
      }
      i++;
      if (strcmp(argv[i],"duplicate")==0) {
	BORDER2=DUPLICATE;
      }
      else if (strcmp(argv[i],"mirror")==0) {
	BORDER2=MIRROR;
      }
      else if (strcmp(argv[i],"skip")==0) {
	BORDER2=SKIP;
      }
      else if (strcmp(argv[i],"undef")==0) {
	BORDER2=UNDEF;
      }
      else {
	// It should be a const integer
	char *endptr;
        int val=strtol(argv[i], &endptr, 10);
	if (endptr!=argv[i]+strlen(argv[i])) {
	  fprintf(stderr,"error : unknown border mode '%s'\n",argv[i]);
	  exit(1);
	}
	BORDER2=CONST_VALUE;
	constBorderValue2=val;
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
  printf("  - Window1: %d\n", WINDOW1);
  printf("  - Window2: %d\n", WINDOW2);
  printf("  - Border1 mode: ");
  switch(BORDER1) {
  case CONST_VALUE:
    printf("const (%d)",constBorderValue1);
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
  printf("  - Border2 mode: ");
  switch(BORDER2) {
  case CONST_VALUE:
    printf("const (%d)",constBorderValue2);
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
  cl_program program = createBorderInputsProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createBorderInputsProgramFromSource( context, device, NULL);
#endif
  
  
  //==================================================================
  // Create the kernel, configure it and prepare inputdata
  //==================================================================
  //printf("-> Create Input/Output Buffer\n");
  /* Create an input/output buffers mapped in the host address space */
  cl_mem dirBuffer,valBuffer,outputBuffer;

  cl_char *direction = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_char),IMAGE_X*IMAGE_Y,&dirBuffer);
  cl_int *value = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_int),IMAGE_X*IMAGE_Y,&valBuffer);
  cl_int *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(cl_int),IMAGE_X*IMAGE_Y,&outputBuffer);
  int *check_output  = malloc(sizeof(int)*IMAGE_Y*IMAGE_X);
  
  /* For using arrays instead of pointers */
  char (*dir)[IMAGE_Y][IMAGE_X]      = (char (*)[IMAGE_Y][IMAGE_X])direction;
  int (*val)[IMAGE_Y][IMAGE_X]	     = (int (*)[IMAGE_Y][IMAGE_X])value;
  int (*out)[IMAGE_Y][IMAGE_X]	     = (int (*)[IMAGE_Y][IMAGE_X])output;
  int (*check_out)[IMAGE_Y][IMAGE_X] = (int (*)[IMAGE_Y][IMAGE_X])check_output;
  
  /* Initializes input images */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*dir)[y][x] = rand()%8;
      (*val)[y][x] = rand()%8;
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
  cl_kernel kernel = createKernel_BorderInputs(program);
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_BorderInputs(kernel,
			     NB_WG0, NB_WG1, NB_WI,
			     outputBuffer,
			     IMAGE_X,
			     IMAGE_Y,
			     dirBuffer,
			     valBuffer
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
  cl_event unmap_event[3];
  status=clEnqueueUnmapMemObject(commandQueue,dirBuffer,direction,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject dir failed.");
  status=clEnqueueUnmapMemObject(commandQueue,valBuffer,value,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject val failed.");
  status=clEnqueueUnmapMemObject(commandQueue,outputBuffer,output,0,NULL,&unmap_event[2]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject output failed.");
  status = clWaitForEvents(3,&unmap_event[0]);


  /* Enqueue a kernel run call */
  cl_event event;
  //printf("-> Launching OpenCL kernel execution \n");
  
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
    printf("** OpenCL 'BorderInputs' has completed. ** \n\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  /* Get back the output buffer from the device memory (blocking read) */
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_int)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer outputBuffer failed.");\


  //==================================================================
  // Check
  //==================================================================

  
  {
	direction = clEnqueueMapBuffer(commandQueue,dirBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_char)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
	oclCheckStatus(status,"clEnqueueMapBuffer dirBuffer failed.");\
	value = clEnqueueMapBuffer(commandQueue,valBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_int)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
	oclCheckStatus(status,"clEnqueueMapBuffer valBuffer failed.");\

	struct timeval start, end;
    gettimeofday(&start, NULL);
    
    // Compute reference data
    computeBorderInputs(check_output,direction,value,IMAGE_X,IMAGE_Y,
			BORDER1,BORDER2,WINDOW1,WINDOW2);
     
    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
  
  dir= (char (*)[IMAGE_Y][IMAGE_X])direction;
  val= (int (*)[IMAGE_Y][IMAGE_X])value;
  out= (int (*)[IMAGE_Y][IMAGE_X])output;

  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  int WINDOW=0;
  if (BORDER1==UNDEF) { WINDOW=MAX(WINDOW,WINDOW1); }
  if (BORDER2==UNDEF) { WINDOW=MAX(WINDOW,WINDOW2); }
  X_MIN+=WINDOW;X_MAX-=WINDOW;
  Y_MIN+=WINDOW;Y_MAX-=WINDOW;

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
      if ( (*out)[y][x] != (*check_out)[y][x] ) {
	if (!nok) {
	  printf("First error : [%d , %d]  %d <> %d\n",x,y,(*out)[y][x],(*check_out)[y][x] );
	}
        nok=1;
      }
    }
  }
  
  if (nok) {
    printf("ERROR on BorderInputs filter verification !\n");
    // Input Matrix
    printf("\n");
    printf("Dir sample \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t",(*dir)[y][x]);
      }
      printf("\n");
    }
    printf("Val sample \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t",(*val)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    // Out Matrix
    printf("Output sample : \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t", (*out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    // Check Out Matrix
    printf("Check output sample : \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
	printf(" %d\t", (*check_out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
  }
  else {
     printf("BorderInputs filter completed OK\n");
  }


  //==================================================================
  // Termination
  //==================================================================

  clReleaseMemObject(dirBuffer);
  clReleaseMemObject(valBuffer);
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
