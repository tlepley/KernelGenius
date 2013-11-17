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

#ifndef KG_OCL_RUNTIME_H
#define KG_OCL_RUNTIME_H

#include <CL/cl.h>

//=====================================================================================
//OpenCL context and base OpenCL objects
//=====================================================================================

/**
 * Get the first OpenCL platform of the system
 */
extern cl_platform_id oclGetFirstPlatform();

/**
 * Get the first OpenCL platform from a particular vendor
 */
cl_platform_id oclGetFirstPlatformFromVendor(const char *vendor);

/**
 * Get the first device of a platform
 */
extern cl_device_id oclGetFirstDevice(cl_platform_id platform);

/**
 * Create an OpenCL context for a given platform/device
 */
extern cl_context oclCreateContext(cl_platform_id platform, cl_device_id device);

/**
 * Create an in-order command queue for a given device in a context
 */
extern cl_command_queue oclCreateCommandQueue(cl_context context, cl_device_id device);

/**
 * Create an out-of-order command queue for a given device in a context
 */
extern cl_command_queue oclCreateCommandQueueOOO(cl_context context, cl_device_id device);

extern void * oclCreateMapBuffer(cl_context context,
		cl_command_queue queue,
		cl_mem_flags create_flags,
		cl_mem_flags map_flags,
		size_t size_elem,
		size_t nb_elem,
		cl_mem *buffer);


//=====================================================================================
//                           Program and Kernel creation
//=====================================================================================


/**
 * Read a kernel source from a file
 */
extern char *oclGetProgramSrcFromFile(const char *fileName, size_t *sizeFile);

/**
 * Read a kernel binary from a file
 */
extern unsigned char *oclGetProgramBinFromFile(const char *fileName, size_t *sizeFile);

/**
 * Read and build an OpenCL program from a source file
 */
extern cl_program oclCreateProgramFromSource(cl_context context, cl_device_id device,
		const char *filename, const char *options);
		
/**
 * Read and build an OpenCL program from a binary file
 */
extern cl_program oclCreateProgramFromBinary(cl_context context, cl_device_id device,
		const char *filename);
		
/**
 * Get the program path from the environment if possible. The OCL_APP_BUILD variable
 * is usually set by the Eclipse OpenCL Wizard plugin
 */
extern char *oclGetProgramPath(char *path, char *name, size_t path_size);


//=====================================================================================
//Debug and checks facilities
//=====================================================================================

/**
 * Print to stdout some information regarding the platform
 *
 * @param platform_id Identifier of the platform
 */
extern void oclDisplayPlatformInfo(cl_platform_id platform_id);

/**
 * Check if a kernel NDRange structure is compatible with a device
 */
extern void checkNDRangeWithDevice(cl_device_id device,
			    cl_kernel kernel,
			    int nb_dim,
			    const size_t globalThreads[],
			    const size_t localThreads[]);


/**
 * Check the status returned by an OpenCL API function. In case of error,
 * print the message passed in parameter, print the error related to the status
 * and exit the program
 */
extern void oclCheckStatus(cl_int status, char *message);


#endif

