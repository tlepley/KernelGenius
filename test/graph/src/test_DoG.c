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

/* This is the test of a 'Difference of Gaussian' filter */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/time.h>

#include <CL/cl.h>
#include <common_ocl.h>

#include <DoG.h>

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
#define ELEM(t,y,x) ({				\
 int value=0; \
      if (border==CONST_VALUE)      { if (((x)<0||(x)>=IMAGE_X) || ((y)<0)||(y)>=IMAGE_Y) {value=constBorderValue;} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=IMAGE_Y)?IMAGE_Y-1:(y))][(x)<0?0:(((x)>=IMAGE_X)?IMAGE_X-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=IMAGE_Y)?2*IMAGE_Y-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=IMAGE_X)?2*IMAGE_X-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})

#define ELEM2(t,y,x) ({				\
 int value=0; \
      if (border==CONST_VALUE)      { if (((x)<0||(x)>=((IMAGE_X+1)/2)) || ((y)<0)||(y)>=((IMAGE_Y+1)/2)) {value=constBorderValue;} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=((IMAGE_Y+1)/2))?((IMAGE_Y+1)/2)-1:(y))][(x)<0?0:(((x)>=((IMAGE_X+1)/2))?((IMAGE_X+1)/2)-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=((IMAGE_Y+1)/2))?2*((IMAGE_Y+1)/2)-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=((IMAGE_X+1)/2))?2*((IMAGE_X+1)/2)-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})

#define ELEM4(t,y,x) ({				\
 int value=0; \
      if (border==CONST_VALUE)      { if (((x)<0||(x)>=((((IMAGE_X+1)/2)+1)/2)) || ((y)<0)||(y)>=((((IMAGE_Y+1)/2)+1)/2)) {value=constBorderValue;} else {value=(*t)[y][x];} } \
 else if (border==DUPLICATE) { value=(*t)[(y)<0?0:( ((y)>=((((IMAGE_Y+1)/2)+1)/2))?((((IMAGE_Y+1)/2)+1)/2)-1:(y))][(x)<0?0:(((x)>=((((IMAGE_X+1)/2)+1)/2))?((((IMAGE_X+1)/2)+1)/2)-1:(x))]; } \
 else if (border==MIRROR)    { value=(*t)[(y)<0?(-(y))-1:( ((y)>=((((IMAGE_Y+1)/2)+1)/2))?2*((((IMAGE_Y+1)/2)+1)/2)-(y)-1:(y))][(x)<0?(-(x))-1:(((x)>=((((IMAGE_X+1)/2)+1)/2))?2*((((IMAGE_X+1)/2)+1)/2)-(x)-1:(x))]; } \
 else { value=(*t)[y][x]; } \
 value; \
})



static void copy_and_downsample
(float       *dst,
 float const *src,
 int width, int height, int d)
{
  int x, y ;

  d = 1 << d ; /* d = 2^d */
  //  for(y = 0 ; y < height - (d-1) ; y+=d) {
  for(y = 0 ; y < height ; y+=d) {
    float const * srcrowp = src + y * width ;
    //    for(x = 0 ; x < width - (d-1) ; x+=d) {
    for(x = 0 ; x < width  ; x+=d) {
      *dst++ = *srcrowp ;
      srcrowp += d ;
    }
  }
}



void computeDoG(DATA_TYPE *s1Matrix, DATA_TYPE *s2Matrix, DATA_TYPE *s3Matrix,
		DATA_TYPE *input,
		int IMAGE_X, int IMAGE_Y,
		enum BORDER_MODE border
		) {
  int x,y;
  DATA_TYPE (*in) [IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;

  float filterPattern_X[9] = {-1.,0.,1., -2.,0. ,2., -1.,0.,1.};
  float filterPattern_Y[9] = {-1.,-2.,-1., 0.,0.,0., 1.,2.,1.};
  
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;

  if ((border==SKIP) || (border==UNDEF)) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  DATA_TYPE *g1Matrix=malloc(sizeof(DATA_TYPE)*IMAGE_Y*IMAGE_X);
  DATA_TYPE *g2Matrix=malloc(sizeof(DATA_TYPE)*IMAGE_Y*IMAGE_X);
  DATA_TYPE (*g1)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])g1Matrix;
  DATA_TYPE (*g2)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])g2Matrix;
  DATA_TYPE (*s1)[IMAGE_Y][IMAGE_X] =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])s1Matrix;

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
      (*g1)[y][x]=filterPattern_X[0]*ELEM(in,y-1,x-1) +
	 filterPattern_X[1]*ELEM(in,y-1,x)   +
	 filterPattern_X[2]*ELEM(in,y-1,x+1) +
	 filterPattern_X[3]*ELEM(in,y,x-1)  +
	 filterPattern_X[4]*ELEM(in,y,x)    +
	 filterPattern_X[5]*ELEM(in,y,x+1)  +
	 filterPattern_X[6]*ELEM(in,y+1,x-1) +
	 filterPattern_X[7]*ELEM(in,y+1,x)   +
	 filterPattern_X[8]*ELEM(in,y+1,x+1) ;
    }
  }

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
     (*g2)[y][x]=filterPattern_Y[0]*ELEM(g1,y-1,x-1) +
	 filterPattern_Y[1]*ELEM(g1,y-1,x)   +
	 filterPattern_Y[2]*ELEM(g1,y-1,x+1) +
	 filterPattern_Y[3]*ELEM(g1,y,x-1)   +
	 filterPattern_Y[4]*ELEM(g1,y,x)     +
	 filterPattern_Y[5]*ELEM(g1,y,x+1)   +
	 filterPattern_Y[6]*ELEM(g1,y+1,x-1) +
	 filterPattern_Y[7]*ELEM(g1,y+1,x)   +
	 filterPattern_Y[8]*ELEM(g1,y+1,x+1) ;
    }
  }

  X_MIN=0, X_MAX=IMAGE_X;
  Y_MIN=0, Y_MAX=IMAGE_Y;

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
     (*s1)[y][x]=(*g2)[y][x] - (*g1)[y][x];
    }
  }

  DATA_TYPE *d1Matrix=malloc(sizeof(DATA_TYPE)*(((IMAGE_Y+1)/2))*(((IMAGE_X+1)/2)));
  DATA_TYPE *h1Matrix=malloc(sizeof(DATA_TYPE)*(((IMAGE_Y+1)/2))*(((IMAGE_X+1)/2)));
  DATA_TYPE *h2Matrix=malloc(sizeof(DATA_TYPE)*(((IMAGE_Y+1)/2))*(((IMAGE_X+1)/2)));
  DATA_TYPE (*d1)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)] =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])d1Matrix;
  DATA_TYPE (*h1)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)] =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])h1Matrix;
  DATA_TYPE (*h2)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)] =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])h2Matrix;
  DATA_TYPE (*s2)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)] =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])s2Matrix;

  copy_and_downsample(d1Matrix,
		      input,
		      IMAGE_X,IMAGE_Y,
		      1);

  X_MIN=0, X_MAX=((IMAGE_X+1)/2);
  Y_MIN=0, Y_MAX=((IMAGE_Y+1)/2);

  if ((border==SKIP) || (border==UNDEF)) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
      (*h1)[y][x]=filterPattern_X[0]*ELEM2(d1,y-1,x-1) +
	filterPattern_X[1]*ELEM2(d1,y-1,x)   +
	filterPattern_X[2]*ELEM2(d1,y-1,x+1) +
	filterPattern_X[3]*ELEM2(d1,y,x-1)  +
	filterPattern_X[4]*ELEM2(d1,y,x)    +
	filterPattern_X[5]*ELEM2(d1,y,x+1)  +
	filterPattern_X[6]*ELEM2(d1,y+1,x-1) +
	filterPattern_X[7]*ELEM2(d1,y+1,x)   +
	filterPattern_X[8]*ELEM2(d1,y+1,x+1) ;
    }
  }
  
  for(y=Y_MIN;y<Y_MAX;y++) {
    for(x=X_MIN;x<X_MAX;x++) {
      (*h2)[y][x]=filterPattern_Y[0]*ELEM2(h1,y-1,x-1) +
	filterPattern_Y[1]*ELEM2(h1,y-1,x)   +
	filterPattern_Y[2]*ELEM2(h1,y-1,x+1) +
	filterPattern_Y[3]*ELEM2(h1,y,x-1)   +
	filterPattern_Y[4]*ELEM2(h1,y,x)     +
	filterPattern_Y[5]*ELEM2(h1,y,x+1)   +
	filterPattern_Y[6]*ELEM2(h1,y+1,x-1) +
	filterPattern_Y[7]*ELEM2(h1,y+1,x)   +
	filterPattern_Y[8]*ELEM2(h1,y+1,x+1) ;
    }
  }

  X_MIN=0, X_MAX=((IMAGE_X+1)/2);
  Y_MIN=0, Y_MAX=((IMAGE_Y+1)/2);

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
     (*s2)[y][x]=(*h2)[y][x] - (*h1)[y][x];
    }
  } 


  DATA_TYPE *d2Matrix=malloc(sizeof(DATA_TYPE)*(((((IMAGE_Y+1)/2)+1)/2))*(((((IMAGE_X+1)/2)+1)/2)));
  DATA_TYPE *i1Matrix=malloc(sizeof(DATA_TYPE)*(((((IMAGE_Y+1)/2)+1)/2))*(((((IMAGE_X+1)/2)+1)/2)));
  DATA_TYPE *i2Matrix=malloc(sizeof(DATA_TYPE)*(((((IMAGE_Y+1)/2)+1)/2))*(((((IMAGE_X+1)/2)+1)/2)));
  DATA_TYPE (*d2)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)] =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])d2Matrix;
  DATA_TYPE (*i1)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)] =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])i1Matrix;
  DATA_TYPE (*i2)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)] =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])i2Matrix;
  DATA_TYPE (*s3)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)] =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])s3Matrix;

  copy_and_downsample(d2Matrix,
		      d1Matrix,
		      ((IMAGE_X+1)/2),((IMAGE_Y+1)/2),
		      1);

  X_MIN=0, X_MAX=((((IMAGE_X+1)/2)+1)/2);
  Y_MIN=0, Y_MAX=((((IMAGE_Y+1)/2)+1)/2);

  if ((border==SKIP) || (border==UNDEF)) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
      (*i1)[y][x]=filterPattern_X[0]*ELEM2(d2,y-1,x-1) +
	filterPattern_X[1]*ELEM2(d2,y-1,x)   +
	filterPattern_X[2]*ELEM2(d2,y-1,x+1) +
	filterPattern_X[3]*ELEM2(d2,y,x-1)  +
	filterPattern_X[4]*ELEM2(d2,y,x)    +
	filterPattern_X[5]*ELEM2(d2,y,x+1)  +
	filterPattern_X[6]*ELEM2(d2,y+1,x-1) +
	filterPattern_X[7]*ELEM2(d2,y+1,x)   +
	filterPattern_X[8]*ELEM2(d2,y+1,x+1) ;
    }
  }
  
  for(y=Y_MIN;y<Y_MAX;y++) {
    for(x=X_MIN;x<X_MAX;x++) {
      (*i2)[y][x]=filterPattern_Y[0]*ELEM2(i1,y-1,x-1) +
	filterPattern_Y[1]*ELEM2(i1,y-1,x)   +
	filterPattern_Y[2]*ELEM2(i1,y-1,x+1) +
	filterPattern_Y[3]*ELEM2(i1,y,x-1)   +
	filterPattern_Y[4]*ELEM2(i1,y,x)     +
	filterPattern_Y[5]*ELEM2(i1,y,x+1)   +
	filterPattern_Y[6]*ELEM2(i1,y+1,x-1) +
	filterPattern_Y[7]*ELEM2(i1,y+1,x)   +
	filterPattern_Y[8]*ELEM2(i1,y+1,x+1) ;
    }
  }

  X_MIN=0, X_MAX=((((IMAGE_X+1)/2)+1)/2);
  Y_MIN=0, Y_MAX=((((IMAGE_Y+1)/2)+1)/2);

  for(y=Y_MIN;y<Y_MAX;y++) {  
    for(x=X_MIN;x<X_MAX;x++) {
     (*s3)[y][x]=(*i2)[y][x] - (*i1)[y][x];
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
  cl_program program = createDoGProgramFromBinary( context, device);
#else
  /* create a CL program using the kernel source */
  cl_program program = createDoGProgramFromSource( context, device,
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
  cl_mem inputBuffer,s1Buffer,s2Buffer,s3Buffer;
  DATA_TYPE *input = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_ONLY,CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&inputBuffer);
  DATA_TYPE *s1Matrix = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(DATA_TYPE),IMAGE_X*IMAGE_Y,&s1Buffer);
  DATA_TYPE *s2Matrix = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(DATA_TYPE),((IMAGE_X+1)/2)*((IMAGE_Y+1)/2),&s2Buffer);
  DATA_TYPE *s3Matrix = oclCreateMapBuffer(context,commandQueue,CL_MEM_READ_WRITE,CL_MAP_READ|CL_MAP_WRITE,sizeof(DATA_TYPE),((((IMAGE_X+1)/2)+1)/2)*((((IMAGE_Y+1)/2)+1)/2),&s3Buffer);
  DATA_TYPE *check_s1Matrix   = malloc(sizeof(DATA_TYPE)*IMAGE_Y*IMAGE_X);
  DATA_TYPE *check_s2Matrix   = malloc(sizeof(DATA_TYPE)*((IMAGE_Y+1)/2)*((IMAGE_X+1)/2));
  DATA_TYPE *check_s3Matrix   = malloc(sizeof(DATA_TYPE)*((((IMAGE_Y+1)/2)+1)/2)*((((IMAGE_X+1)/2)+1)/2));

  /* For using arrays instead of pointers */
  DATA_TYPE (*in)[IMAGE_Y][IMAGE_X]  	      =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  DATA_TYPE (*s1)[IMAGE_Y][IMAGE_X]	      =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])s1Matrix;
  DATA_TYPE (*s2)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)]	      =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])s2Matrix;
  DATA_TYPE (*s3)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)]	      =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])s3Matrix;
  DATA_TYPE (*check_s1)[IMAGE_Y][IMAGE_X]    =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])check_s1Matrix;
  DATA_TYPE (*check_s2)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)]    =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])check_s2Matrix;
  DATA_TYPE (*check_s3)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)]    =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])check_s3Matrix;
  
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
      (*s1)[y][x] = (*check_s1)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }
  for(y=0;y<((IMAGE_Y+1)/2);y++) {
    for(x=0;x<((IMAGE_X+1)/2);x++) {
      (*s2)[y][x] = (*check_s2)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }
  for(y=0;y<((((IMAGE_Y+1)/2)+1)/2);y++) {
    for(x=0;x<((((IMAGE_X+1)/2)+1)/2);x++) {
      (*s3)[y][x] = (*check_s3)[y][x] = (rand()&0x3f)*(((rand()&0xff)>128)?1:-1);
    }
  }

  
  /* Get a kernel object */
  //printf("-> Create Kernel\n");
  cl_kernel kernel = createKernel_DoG(program);
  
  
  /* Set Kernel arguments */
  //printf("-> Sets Kernel Args\n");
  setKernelArgs_DoG(kernel,
		    NB_WG0, NB_WG1, NB_WI,
		    s1Buffer,
		    s2Buffer,
		    //		    s3Buffer,
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
  cl_event unmap_event[4];
  status=clEnqueueUnmapMemObject(commandQueue,inputBuffer,input,0,NULL,&unmap_event[0]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject input failed.");
  status=clEnqueueUnmapMemObject(commandQueue,s1Buffer,s1,0,NULL,&unmap_event[1]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject s1Matrix failed.");
  status=clEnqueueUnmapMemObject(commandQueue,s2Buffer,s2,0,NULL,&unmap_event[2]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject s2Matrix failed.");
  status=clEnqueueUnmapMemObject(commandQueue,s3Buffer,s3,0,NULL,&unmap_event[3]);
  oclCheckStatus(status,"clEnqueueUnmapMemObject s3Matrix failed.");
  status = clWaitForEvents(4,&unmap_event[0]);


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
				    4,&unmap_event[0],
				    &event);
    oclCheckStatus(status,"clEnqueueNDRangeKernel failed.");
    status = clWaitForEvents(1, &event);
    
    gettimeofday(&end, NULL);
    printf("** OpenCL 'DoG' has completed. ** \n\n");
    printf("OpenCL Kernel execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }
 
  /* Get back the output buffer from the device memory (blocking read) */
  s1Matrix = clEnqueueMapBuffer(commandQueue,s1Buffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueReadBuffer failed.");
  s2Matrix = clEnqueueMapBuffer(commandQueue,s2Buffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*((IMAGE_X+1)/2)*((IMAGE_Y+1)/2),1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueReadBuffer failed.");
  s3Matrix = clEnqueueMapBuffer(commandQueue,s3Buffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*((((IMAGE_X+1)/2)+1)/2)*((((IMAGE_Y+1)/2)+1)/2),1,&event,NULL,&status);
  oclCheckStatus(status,"clEnqueueReadBuffer failed.");


  //==================================================================
  // Check
  //==================================================================

  {
    input= clEnqueueMapBuffer(commandQueue,inputBuffer,CL_TRUE,CL_MAP_READ,0,sizeof(DATA_TYPE)*IMAGE_X*IMAGE_Y,0,NULL,NULL,&status);
	oclCheckStatus(status,"clEnqueueReadBuffer input failed.");\

	struct timeval start, end;
    gettimeofday(&start, NULL);

    // Compute reference data
    computeDoG(check_s1Matrix,check_s2Matrix,check_s3Matrix,input,IMAGE_X,IMAGE_Y,BORDER);
    
    gettimeofday(&end, NULL);
    printf("Reference code execution time: %ld (microseconds)\n", ((end.tv_sec * 1000000 + end.tv_usec) - (start.tv_sec * 1000000 + start.tv_usec)));
  }

  in =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])input;
  s1 =(DATA_TYPE (*)[IMAGE_Y][IMAGE_X])s1Matrix;
  s2 =(DATA_TYPE (*)[((IMAGE_Y+1)/2)][((IMAGE_X+1)/2)])s2Matrix;
  s3 =(DATA_TYPE (*)[((((IMAGE_Y+1)/2)+1)/2)][((((IMAGE_X+1)/2)+1)/2)])s3Matrix;

  // Check results
  int nok=0;
  int X_MIN=0, X_MAX=IMAGE_X;
  int Y_MIN=0, Y_MAX=IMAGE_Y;
  if (BORDER==UNDEF) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }

  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
#ifdef DATA_INT
      if ( (*s1)[y][x] != (*check_s1)[y][x] ) {
	if (!nok) {
	  printf("First error S1 : [%d , %d]  %d <> %d\n",x,y,(*s1)[y][x],(*check_s1)[y][x] );
	}
#else
      float diff=(*s1)[y][x]-(*check_s1)[y][x];
      if ( ABS(diff) > PRECISION ) {
	if (!nok) {
          printf("First error S1 : [%d, %d]  %f <> %f\n",x,y,(*s1)[y][x],(*check_s1)[y][x] );
        }
#endif
        nok=1;
      }
    }
  }

  X_MIN=0, X_MAX=((IMAGE_X+1)/2);
  Y_MIN=0, Y_MAX=((IMAGE_Y+1)/2);
  if (BORDER==UNDEF) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }
  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
#ifdef DATA_INT
      if ( (*s2)[y][x] != (*check_s2)[y][x] ) {
	if (!nok) {
	  printf("First error S2 : [%d , %d]  %d <> %d\n",x,y,(*s2)[y][x],(*check_s2)[y][x] );
	}
#else
      float diff=(*s2)[y][x]-(*check_s2)[y][x];
      if ( ABS(diff) > PRECISION ) {
	if (!nok) {
          printf("First error S2 : [%d, %d]  %f <> %f\n",x,y,(*s2)[y][x],(*check_s2)[y][x] );
        }
#endif
        nok=1;
      }
    }
  }

  X_MIN=0, X_MAX=((((IMAGE_X+1)/2)+1)/2);
  Y_MIN=0, Y_MAX=((((IMAGE_Y+1)/2)+1)/2);
  if (BORDER==UNDEF) {
    X_MIN++;X_MAX--;
    Y_MIN++;Y_MAX--;
  }
  for(y=Y_MIN; y<Y_MAX ;y++) {
    for(x=X_MIN; x<X_MAX ;x++) {
#ifdef DATA_INT
      //      if ( (*s3)[y][x] != (*check_s3)[y][x] ) {
      //	if (!nok) {
      //	  printf("First error S3 : [%d , %d]  %d <> %d\n",x,y,(*s3)[y][x],(*check_s3)[y][x] );
      //	}
#else
      //      float diff=(*s2)[y][x]-(*check_s2)[y][x];
      //      if ( ABS(diff) > PRECISION ) {
      //	if (!nok) {
      //          printf("First error S3 : [%d, %d]  %f <> %f\n",x,y,(*s3)[y][x],(*check_s3)[y][x] );
      //        }
#endif
      //        nok=1;
      //      }
    }
  }

  if (nok) {
    printf("ERROR on DoG filter verification !\n");
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
    
    // S1 Matrix 
    printf("S1 Output sample : \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*s1)[y][x]);
#else
	printf(" %.2f", (*s1)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
    
    // S1 Out Matrix 
    printf("Check S1 output sample : \n");
    for(y=0;y<MIN(IMAGE_Y,PRINT_SIZE);y++) {
      for(x=0;x<MIN(IMAGE_X,PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*check_s1)[y][x]);
#else
	printf(" %.2f", (*check_s1)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");

   // S2 Matrix 
    printf("S2 Output sample : \n");
    for(y=0;y<MIN(((IMAGE_Y+1)/2),PRINT_SIZE);y++) {
      for(x=0;x<MIN(((IMAGE_X+1)/2),PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*s2)[y][x]);
#else
	printf(" %.2f", (*s2)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");
    
    // S2 Out Matrix 
    printf("Check S2 output sample : \n");
    for(y=0;y<MIN(((IMAGE_Y+1)/2),PRINT_SIZE);y++) {
      for(x=0;x<MIN(((IMAGE_X+1)/2),PRINT_SIZE);x++) {
#ifdef DATA_INT
	printf(" %d\t", (*check_s2)[y][x]);
#else
	printf(" %.2f", (*check_s2)[y][x]);
#endif
      }
      printf("\n");
    }
    printf("\n");

   // S3 Matrix 
    //    printf("S3 Output sample : \n");
    //    for(y=0;y<MIN(((((IMAGE_Y+1)/2)+1)/2),PRINT_SIZE);y++) {
    //     for(x=0;x<MIN(((((IMAGE_X+1)/2)+1)/2),PRINT_SIZE);x++) {
#ifdef DATA_INT
    //	printf(" %d\t", (*s3)[y][x]);
#else
    //	printf(" %.2f", (*s3)[y][x]);
#endif
    //     }
    //     printf("\n");
    //   }
    //   printf("\n");

    // S3 Out Matrix 
    //    printf("Check S3 output sample : \n");
    //    for(y=0;y<MIN(((((IMAGE_Y+1)/2)+1)/2),PRINT_SIZE);y++) {
    //     for(x=0;x<MIN(((((IMAGE_X+1)/2)+1)/2),PRINT_SIZE);x++) {
#ifdef DATA_INT
    //	printf(" %d\t", (*check_s3)[y][x]);
#else
    //	printf(" %.2f", (*check_s3)[y][x]);
#endif
    //      }
    //      printf("\n");
    //    }
    //    printf("\n");

  }
  else {
     printf("DoG filter completed OK\n");
  }


  //==================================================================
  // Termination
  //==================================================================

  // Release mapped buffer
  clReleaseMemObject(inputBuffer);
  clReleaseMemObject(s1Buffer);
  clReleaseMemObject(s2Buffer);
  clReleaseMemObject(s3Buffer);
  clReleaseEvent(event);
  clReleaseKernel(kernel);
  clReleaseProgram(program);
  clReleaseCommandQueue(commandQueue);
  clReleaseContext(context);

  //printf("** OCL buffers released. **\n");

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
