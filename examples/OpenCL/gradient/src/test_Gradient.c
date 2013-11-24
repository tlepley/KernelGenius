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
*/

/* This an example of 'Gradient Filter' */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <sys/time.h>

#include <CL/cl.h>
#include "kg_ocl_runtime.h"

#include "Gradient.h"

static char * OPENCL_PLT_VENDOR = NULL;

#define MIN(a,b) ((a)<(b)?(a):(b))
#define ABS(a) ((a)<0?-(a):(a))


/* Result checking accuracy */
#define PRECISION 0.00008f

/* Default Image dimensions */
static int IMAGE_X = 512;
static int IMAGE_Y = 512;

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;


// Reference code
#define VL_EPSILON_F 1.19209290E-07F
#define VL_PI 3.141592653589793f
inline float vl_fast_resqrt_f (float x) {
  /* 32-bit version */
  union {
    float x ;
    int  i ;
  } u ;

  float xhalf = (float) 0.5 * x ;

  /* convert floating point value in RAW integer */
  u.x = x ;

  /* gives initial guess y0 */
  u.i = 0x5f3759df - (u.i >> 1);
  /*u.i = 0xdf59375f - (u.i>>1);*/

  /* two Newton steps */
  u.x = u.x * ( (float) 1.5f  - xhalf*u.x*u.x) ;
  u.x = u.x * ( (float) 1.5f  - xhalf*u.x*u.x) ;
  return u.x ;
}

inline float vl_fast_sqrt_f (float x) {
  return (x < 1e-8f) ? 0 : x * vl_fast_resqrt_f (x) ;
}

inline float
vl_mod_2pi_f (float x) {
  while (x > (float)(2 * VL_PI)) x -= (float) (2 * VL_PI) ;
  while (x < 0.0F) x += (float) (2 * VL_PI);
  return x ;
}

inline float vl_fast_atan2_f (float y, float x) {
  float angle, r ;
  float c3 = 0.1821F ;
  float c1 = 0.9675F ;
  float abs_y    = fabs(y) + VL_EPSILON_F ;

  if (x >= 0) {
    r = (x - abs_y) / (x + abs_y) ;
    angle = (float) (VL_PI / 4) ;
  } else {
    r = (x + abs_y) / (abs_y - x) ;
    angle = (float) (3 * VL_PI / 4) ;
  }
  angle += (c3*r*r - c1) * r ;
  return (y < 0) ? - angle : angle ;
}


static void
update_gradient  (float      *grad,
		  float const *src, int w, int h)
{
  int const xo    = 1 ;
  int const yo    = w ;
  
  const float *end;
  float gx, gy ;
  int y;
  

#define SAVE_BACK							\
  *grad++ = vl_fast_sqrt_f (gx*gx + gy*gy) ;				\
  *grad++ = vl_mod_2pi_f   (vl_fast_atan2_f (gy, gx) + 2*VL_PI) ;	\
  ++src ;
  
  /* first pixel of the first row */
  gx = src[+xo] - src[0] ;
  gy = src[+yo] - src[0] ;
  SAVE_BACK ;
  
  /* middle pixels of the  first row */
  end = (src - 1) + w - 1 ;
  while (src < end) {
    gx = 0.5 * (src[+xo] - src[-xo]) ;
    gy =        src[+yo] - src[0] ;
    SAVE_BACK ;
  }
  
  /* last pixel of the first row */
  gx = src[0]   - src[-xo] ;
  gy = src[+yo] - src[0] ;
  SAVE_BACK ;
  
  for (y = 1 ; y < h -1 ; ++y) {
    
    /* first pixel of the middle rows */
    gx =        src[+xo] - src[0] ;
    gy = 0.5 * (src[+yo] - src[-yo]) ;
    SAVE_BACK ;
    
    /* middle pixels of the middle rows */
    end = (src - 1) + w - 1 ;
    while (src < end) {
      gx = 0.5 * (src[+xo] - src[-xo]) ;
      gy = 0.5 * (src[+yo] - src[-yo]) ;
      SAVE_BACK ;
    }
    
    /* last pixel of the middle row */
    gx =        src[0]   - src[-xo] ;
    gy = 0.5 * (src[+yo] - src[-yo]) ;
    SAVE_BACK ;
  }
  
  /* first pixel of the last row */
  gx = src[+xo] - src[0] ;
  gy = src[  0] - src[-yo] ;
  SAVE_BACK ;
  
  /* middle pixels of the last row */
  end = (src - 1) + w - 1 ;
  while (src < end) {
    gx = 0.5 * (src[+xo] - src[-xo]) ;
    gy =        src[0]   - src[-yo] ;
    SAVE_BACK ;
  }
  
  /* last pixel of the last row */
  gx = src[0]   - src[-xo] ;
  gy = src[0]   - src[-yo] ;
  SAVE_BACK ;
}


static cl_platform_id   platform;
static cl_device_id     device;
static cl_context       context;
static cl_command_queue commandQueue;


static int TestGradient(cl_program program) {
  cl_int status;
 
  printf("** Setting up of the 'Gradient' kernel\n");

  /* Get the kernel */
  cl_kernel kernel = createKernel_Gradient3x3(program);
  
  
  //==================================================================
  // Create buffers
  //==================================================================


  cl_mem inputBuffer,outputBuffer;
  cl_float *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_float),IMAGE_X*IMAGE_Y,&inputBuffer);
  cl_float *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(cl_float),2*IMAGE_X*IMAGE_Y,&outputBuffer);
  cl_float *check_output = malloc(sizeof(cl_float)*IMAGE_Y*2*IMAGE_X);
  
  /* For using arrays instead of pointers */
  cl_float (*in)[IMAGE_Y][IMAGE_X]  	  =(cl_float (*)[IMAGE_Y][IMAGE_X])input;
  cl_float (*out)[IMAGE_Y][2*IMAGE_X]	  =(cl_float (*)[IMAGE_Y][2*IMAGE_X])output;
  cl_float (*check_out)[IMAGE_Y][2*IMAGE_X] =(cl_float (*)[IMAGE_Y][2*IMAGE_X])check_output;

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
  setKernelArgs_Gradient3x3(kernel,
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
  printf("** Execution of 'Gradient' kernel\n");
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
    printf("** The 'Gradient' kernel execution has completed.\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  /* Get back the output buffer from the device memory (blocking read) */
  output= clEnqueueMapBuffer(commandQueue,outputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_float)*2*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueMapBuffer output failed.");\
 
  //==================================================================
  // Check results
  //==================================================================
  
  {
    input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(cl_float)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
    oclCheckStatus(status,"clEnqueueMapBuffer input failed."); 

    struct timeval start, end;
    gettimeofday(&start, NULL);

    // Compute result from the reference code
    update_gradient((float*)check_output,(float*)input,IMAGE_X,IMAGE_Y);  
  
    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  in =(float (*)[IMAGE_Y][IMAGE_X])input;
  out =(float (*)[IMAGE_Y][2*IMAGE_X])output;

  // Check reference vs OCL
  int nok=0;int first=1;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<2*IMAGE_X;x++) {
      if (ABS((*out)[y][x]-(*check_out)[y][x])>PRECISION) {
	if (first) {
	  printf("!! 'Gradient' : first error [%d,%d], %f instead of %f\n",x,y,(*out)[y][x],(*check_out)[y][x]);
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
    for(y=0;y<MIN(8,IMAGE_Y);y++) {
      for(x=0;x<MIN(2*8,2*IMAGE_X);x++) {
	printf(" %f", (*out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    /* Print a sample of the correct output matrix */
    printf("Correct output Matrix sample\n");
    for(y=0;y<MIN(8,IMAGE_Y);y++) {
      for(x=0;x<MIN(2*8,2*IMAGE_X);x++) {
	printf(" %f", (*check_out)[y][x]);
      }
      printf("\n");
    }
    printf("\n");
    
    printf("!! 'Gradient' completed with ERROR\n");
  }
  else {
    printf(">> 'Gradient' completed OK\n");
  }
  

  //==================================================================
  // Termination
  //==================================================================
  
  clReleaseMemObject(inputBuffer);
  clReleaseMemObject(outputBuffer);
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
  printf("  - Nb work-items per work-group: %d\n",NB_WI);
  printf("  - Nb work-groups: [%d,%d]\n",NB_WG0, NB_WG1);
  printf("\n");


  //==================================================================
  // OpenCL setup
  //==================================================================
  
  //printf("-> OpenCL host setup\n");
  
  /* Get the OpenCL platform and print some infos */
  if (OPENCL_PLT_VENDOR==NULL) {
    platform=oclGetFirstPlatform();
  }
  else {
    platform=oclGetFirstPlatformFromVendor(OPENCL_PLT_VENDOR);
  }
  oclDisplayPlatformInfo(platform);
  
  /* Pickup the first available devices */
  device = oclGetFirstDevice(platform);
  //oclDisplayDeviceInfo(device);
  
  /* Create context */
  //printf("-> Create context\n");
  context = oclCreateContext(platform,device);
  
  /* Create a command Queue  */
  //printf("-> Create command Queue\n");
  commandQueue = oclCreateCommandQueueOOO(context, device);


  //==================================================================
  // Compile the program
  //==================================================================
  
  // Initialization of gradient kernels (load/compile program)
#ifdef AHEAD_OF_TIME
  cl_program program = createGradientProgramFromBinary(context,device);
#else
  cl_program program = createGradientProgramFromSource(context,device,NULL);
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
  //printf("-> OCL objects released\n");
  
  // Unload the OpenCL runtime for correct trace generation
#ifdef __P2012__
  clUnloadRuntime();
#endif
  
  return status;
}
