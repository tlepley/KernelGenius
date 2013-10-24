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

#ifndef COMMON_OCL_H
#define COMMON_OCL_H

/**
 * Check the status returned by an OpenCL API function. In case of error,
 * print the message passed in parameter, print the error related to the status
 * and exit the program
 */
void oclCheckStatus(cl_int status, char *message) {
  if (status!=CL_SUCCESS) {
    fprintf(stderr,"%s\n",message);
    switch( status ){
    case CL_SUCCESS :
      break;
    case CL_DEVICE_NOT_FOUND :
      fprintf( stderr, "device not found" );
      break;
    case CL_DEVICE_NOT_AVAILABLE :
      fprintf( stderr, "device not available" );
      break;
    case CL_COMPILER_NOT_AVAILABLE :
      fprintf( stderr, "compiler not available" );
      break;
    case CL_MEM_OBJECT_ALLOCATION_FAILURE :
      fprintf( stderr, "memory object allocation failure" );
      break;
    case CL_OUT_OF_RESOURCES :
      fprintf( stderr, "out of resources" );
      break;
    case CL_OUT_OF_HOST_MEMORY :
      fprintf( stderr, "out of host memory" );
      break;
    case CL_PROFILING_INFO_NOT_AVAILABLE :
      fprintf( stderr, "profiling info not available" );
      break;
    case CL_MEM_COPY_OVERLAP :
      fprintf( stderr, "memory copy overlap" );
      break;
    case CL_IMAGE_FORMAT_MISMATCH :
      fprintf( stderr, "image format mismatch" );
      break;
    case CL_IMAGE_FORMAT_NOT_SUPPORTED :
      fprintf( stderr, "image format not supported" );
      break;
    case CL_BUILD_PROGRAM_FAILURE :
      fprintf( stderr, "build program failure" );
      break;
    case CL_MAP_FAILURE :
      fprintf( stderr, "map failure" );
      break;
    case CL_MISALIGNED_SUB_BUFFER_OFFSET :
      fprintf( stderr, "misaligned sub-buffer offset" );
      break;
    case CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST :
      fprintf( stderr, "execution status error for events in wait list" );
      break;
    case CL_INVALID_VALUE :
      fprintf( stderr, "invalid value" );
      break;
    case CL_INVALID_DEVICE_TYPE :
      fprintf( stderr, "invalid device type" );
      break;
    case CL_INVALID_PLATFORM :
      fprintf( stderr, "invalid platform" );
      break;
    case CL_INVALID_DEVICE :
      fprintf( stderr, "invalid device" );
      break;
    case CL_INVALID_CONTEXT :
      fprintf( stderr, "invalid context" );
      break;
    case CL_INVALID_QUEUE_PROPERTIES :
      fprintf( stderr, "invalid queue properties" );
      break;
    case CL_INVALID_COMMAND_QUEUE :
      fprintf( stderr, "invalid command queue" );
      break;
    case CL_INVALID_HOST_PTR :
      fprintf( stderr, "invalid host pointer" );
      break;
    case CL_INVALID_MEM_OBJECT :
      fprintf( stderr, "invalid memory object" );
      break;
    case CL_INVALID_IMAGE_FORMAT_DESCRIPTOR :
      fprintf( stderr, "invalid format descriptor" );
      break;
    case CL_INVALID_IMAGE_SIZE :
      fprintf( stderr, "invalid image size" );
      break;
    case CL_INVALID_SAMPLER :
      fprintf( stderr, "invalid sampler" );
      break;
    case CL_INVALID_BINARY :
      fprintf( stderr, "invalid binary" );
      break;
    case CL_INVALID_BUILD_OPTIONS :
      fprintf( stderr, "invalid build options" );
      break;
    case CL_INVALID_PROGRAM :
      fprintf( stderr, "invalid program" );
      break;
    case CL_INVALID_PROGRAM_EXECUTABLE :
      fprintf( stderr, "invalid program executable" );
      break;
    case CL_INVALID_KERNEL_NAME :
      fprintf( stderr, "invalid kernel name" );
      break;
    case CL_INVALID_KERNEL_DEFINITION :
      fprintf( stderr, "invalid kernel definition" );
      break;
    case CL_INVALID_KERNEL :
      fprintf( stderr, "invalid kernel" );
      break;
    case CL_INVALID_ARG_INDEX :
      fprintf( stderr, "invalid argument index" );
      break;
    case CL_INVALID_ARG_VALUE :
      fprintf( stderr, "invalid argument value" );
      break;
    case CL_INVALID_ARG_SIZE :
      fprintf( stderr, "invalid argument size" );
      break;
    case CL_INVALID_KERNEL_ARGS :
      fprintf( stderr, "invalid kernel arguments" );
      break;
    case CL_INVALID_WORK_DIMENSION :
      fprintf( stderr, "invalid work dimension" );
      break;
    case CL_INVALID_WORK_GROUP_SIZE :
      fprintf( stderr, "invalid work-group size" );
      break;
    case CL_INVALID_WORK_ITEM_SIZE :
      fprintf( stderr, "invalid work-item size" );
      break;
    case CL_INVALID_GLOBAL_OFFSET :
      fprintf( stderr, "invalid global offset" );
      break;
    case CL_INVALID_EVENT_WAIT_LIST :
      fprintf( stderr, "invalid event wait list" );
      break;
    case CL_INVALID_EVENT :
      fprintf( stderr, "invalid event" );
      break;
    case CL_INVALID_OPERATION :
      fprintf( stderr, "invalid operation" );
      break;
    case CL_INVALID_GL_OBJECT :
      fprintf( stderr, "invalid GL object" );
      break;
    case CL_INVALID_BUFFER_SIZE :
      fprintf( stderr, "invalid buffer size" );
      break;
    case CL_INVALID_MIP_LEVEL :
      fprintf( stderr, "invalid Mip level" );
      break;
    case CL_INVALID_GLOBAL_WORK_SIZE :
      fprintf( stderr, "invalid global work size" );
      break;
    default:
      fprintf( stderr, "Unknown error" );
    }
    fprintf(stderr,"\n");
    exit(1);
  }
}

/**
 * Get the first OpenCL platform of the OpenCL infrastructue
 */
cl_platform_id oclGetFirstPlatform() {
  cl_int status;

  /* Look available platforms */
  /* ------------------------ */
  cl_uint numPlatforms;
  cl_platform_id platforms[1];
  status = clGetPlatformIDs(1, platforms, &numPlatforms);
  oclCheckStatus(status,"clGetPlatformIDs failed.");
  
  /* At least one platform must be available */
  if (numPlatforms==0) {
    fprintf(stderr,"No OpenCL platform available");
    exit(1);
  }
  
  return platforms[0];
}

/**
 * Get the first device of a platform
 */
cl_device_id oclGetFirstDevice(cl_platform_id platform) {
  cl_int status;
  cl_device_id device;
  
  /* Pickup the first available device */
  cl_uint numDevices;
  status = clGetDeviceIDs(
			  platform,
			  CL_DEVICE_TYPE_ALL,
			  1,
			  &device,
			  &numDevices);
  oclCheckStatus(status,"clGetDeviceIDs failed.");
  
  /* At least one device must be available */
  if (numDevices==0) {
    fprintf(stderr,"No Device available in OpenCL platform");
    exit(1);
  }
  
  return device;
}

/**
 * Create an OpenCL context for a given platform/device
 */
cl_context oclCreateContext(cl_platform_id platform,
			    cl_device_id device) {
  cl_context context;
  cl_int status;

  cl_context_properties context_prop[3] = { CL_CONTEXT_PLATFORM,
					    (cl_context_properties)platform,
					    0 };
  
  context = clCreateContext(context_prop,
			    1, &device,
			    NULL,NULL,
			    &status);
  oclCheckStatus(status,"clCreateContext failed.");
  return context;
}

/**
 * Create an out-of-order command queue for a given device in a context
 */
cl_command_queue oclCreateCommandQueueOOO(cl_context context, cl_device_id device) {
  cl_int status;
  cl_command_queue commandQueue =
    clCreateCommandQueue(context, 
			 device, 
			 CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE, 
			 &status);
  oclCheckStatus(status,"clCreateCommandQueue OOO failed.");
  return commandQueue;
}

void * oclCreateMapBuffer(cl_context context,
		cl_command_queue queue,
		cl_mem_flags create_flags,
		cl_mem_flags map_flags,
		size_t size_elem,
		size_t nb_elem,
		cl_mem *buffer) {
	cl_int status;
	size_t size = sizeof(size_elem) * nb_elem;
	cl_mem b = clCreateBuffer(context, 
			create_flags,
			size,
			NULL, 
			&status);
	oclCheckStatus(status,"clCreateBuffer failed.");
	*buffer = b;
	void * p = clEnqueueMapBuffer(queue,
			b,
			CL_TRUE,
			map_flags,
			0,
			size,
			0, NULL,
			NULL,
			&status
			);
	oclCheckStatus(status,"clEnqueueMapBuffer failed.");
	return p;
}




#endif

