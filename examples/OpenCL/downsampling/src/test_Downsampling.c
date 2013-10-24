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

/* Example of simple Downsampling-by-2 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#include <CL/cl.h>
#include <common_ocl.h>

#include <Downsampling.h>

#ifdef DATA_INT
#define DATA_TYPE cl_int
#else
#define DATA_TYPE cl_float
#define PRECISION 0.01
#endif

#define MIN(a,b) ((a)<(b)?(a):(b))
#define MAX(a,b) ((a)>(b)?(a):(b))
#define ABS(a) ((a)<0?-(a):(a))


/* Default Image dimensions */
static int IMAGE_X = 512;
static int IMAGE_Y = 512;

/* OpenCL configuration */
static int NB_WI = 16;
static int NB_WG0 = 1;
static int NB_WG1 = 1;


//##################################################################
// copy_and_downsample
//
// Reference function for the kernel
//##################################################################
static void copy_and_downsample
(float       *dst,
 float const *src,
 int width, int height, int d)
{
  int x, y ;

  d = 1 << d ; /* d = 2^d */
  for(y = 0 ; y < height - (d-1) ; y+=d) {
    float const * srcrowp = src + y * width ;
    for(x = 0 ; x < width - (d-1) ; x+=d) {
      *dst++ = *srcrowp ;
      srcrowp += d ;
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
  
  printf("***********************************************************\n");
  printf("*            Example of Downsampling filter               *\n");
  printf("***********************************************************\n");
  printf("\n");

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
  cl_program program = createDownsamplingProgramFromBinary(context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createDownsamplingProgramFromSource(context, device,
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
#ifdef DATA_INT
  cl_int *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_int),IMAGE_X*IMAGE_Y,&inputBuffer);
  cl_int *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(cl_int),((IMAGE_X+1)>>1)*((IMAGE_Y+1)>>1),&outputBuffer);
#else
  cl_float *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(cl_float),IMAGE_X*IMAGE_Y,&inputBuffer);
  cl_float *output = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(cl_float),((IMAGE_X+1)>>1)*((IMAGE_Y+1)>>1),&outputBuffer);
#endif
  DATA_TYPE *check_output = malloc(sizeof(DATA_TYPE)*((IMAGE_Y+1)>>1)*((IMAGE_X+1)>>1));
  
  /* For using arrays instead of pointers */
  DATA_TYPE (*in)[IMAGE_Y][IMAGE_X]  	        =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  DATA_TYPE (*out)[(IMAGE_Y+1)>>1][(IMAGE_X+1)>>1]      =(DATA_TYPE (*)[(IMAGE_Y+1)>>1][(IMAGE_X+1)>>1])output;
  DATA_TYPE (*check_out)[(IMAGE_Y+1)>>1][(IMAGE_X+1)>>1]=(DATA_TYPE (*)[(IMAGE_Y+1)>>1][(IMAGE_X+1)>>1])check_output;

  /* Initializes input images */
  int x,y;
  for(y=0;y<IMAGE_Y;y++) {
    for(x=0;x<IMAGE_X;x++) {
      (*in)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }

  /* Initializes output images */
  for(y=0;y<((IMAGE_Y+1)>>1);y++) {
    for(x=0;x<((IMAGE_X+1)>>1);x++) {
      (*out)[y][x] = (*check_out)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }

  /* Get a kernel object */
  //printf("-> Create Kernel\n");
  cl_kernel kernel = createKernel_Downsampling(program);
  
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_Downsampling(kernel,
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
#ifdef DATA_INT
  status = clEnqueueReadBuffer(commandQueue,outputBuffer,CL_TRUE,0,sizeof(cl_int)*((IMAGE_X+1)>>1)*((IMAGE_Y+1)>>1),output,1,&event,NULL);
#else
  status = clEnqueueReadBuffer(commandQueue,outputBuffer,CL_TRUE,0,sizeof(cl_float)*((IMAGE_X+1)>>1)*((IMAGE_Y+1)>>1),output,1,&event,NULL);
#endif
  oclCheckStatus(status,"clEnqueueReadBuffer failed.");

  
  //==================================================================
  // End of the Host application, release now useless OpenCL objects
  //==================================================================  
  clReleaseEvent(event);
  clReleaseKernel(kernel);
  clReleaseProgram(program);
  clReleaseCommandQueue(commandQueue);
  clReleaseContext(context);
  //printf("-> OCL objects released\n");
  printf("** OpenCL 'Downsampling' has completed. ** \n\n");


  //==================================================================
  // Check
  //==================================================================

  // Compute result from the reference code
  copy_and_downsample(check_output,
		      input,
		      IMAGE_X,IMAGE_Y,
		      1);

  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN; y<((Y_MAX+1)>>1) ;y++) {
    for(x=X_MIN; x<((X_MAX+1)>>1) ;x++) {
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
    printf("ERROR on Downsampling filter verification !\n");
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
    for(y=0;y<(MIN(IMAGE_Y,PRINT_SIZE)>>1);y++) {
    for(x=0;x<(MIN(IMAGE_X,PRINT_SIZE)>>1);x++) {
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
    for(y=0;y<((MIN(IMAGE_Y,PRINT_SIZE)+1)>>1);y++) {
      for(x=0;x<((MIN(IMAGE_X,PRINT_SIZE)+1)>>1);x++) {
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
    printf("Downsampling filter completed OK\n");
  }

  //==================================================================
  // Termination
  //==================================================================

  // Release mapped buffer
  clReleaseMemObject(inputBuffer);
  clReleaseMemObject(outputBuffer);

  //printf("** OCL buffers released. **\n");

  printf("\n");
  printf("***********************************************************\n");
  printf("*                    End of example                       *\n");
  printf("***********************************************************\n");
  printf("\n");

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
