/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.dicom.BinaryInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.nio.charset.Charset;

import java.util.HashMap;
import java.util.Map;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for extracting NIfTI-1 image input format headers.</p>
 *
 * @author	dclunie
 */
public class NIfTI1Header {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/NIfTI1Header.java,v 1.9 2017/01/24 10:50:34 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(NIfTI1Header.class);
	
	public static final int FIXED_HEADER_LENGTH = 348;
	public static final int MAGIC_OFFSET = FIXED_HEADER_LENGTH - 4;
	
	enum Intent {
		NONE			((short)0),
		CORREL			((short)2),
		TTEST			((short)3),
		FTEST			((short)4),
		ZSCORE			((short)5),
		CHISQ			((short)6),
		BETA			((short)7),
		BINOM			((short)8),
		GAMMA			((short)9),
		POISSON			((short)10),
		NORMAL			((short)11),
		FTEST_NONC		((short)12),
		CHISQ_NONC		((short)13),
		LOGISTIC		((short)14),
		LAPLACE			((short)15),
		UNIFORM			((short)16),
		TTEST_NONC		((short)17),
		WEIBULL			((short)18),
		CHI				((short)19),
		INVGAUSS		((short)20),
		EXTVAL			((short)21),
		PVAL			((short)22),
		LOGPVAL			((short)23),
		LOG10PVAL		((short)24),
		ESTIMATE		((short)1001),
		LABEL			((short)1002),
		NEURONAME		((short)1003),
		GENMATRIX		((short)1004),
		SYMMATRIX		((short)1005),
		DISPVECT		((short)1006),
		VECTOR			((short)1007),
		POINTSET		((short)1008),
		TRIANGLE		((short)1009),
		QUATERNION		((short)1010),
		DIMLESS			((short)1011),
		TIME_SERIES		((short)2001),
		NODE_INDEX		((short)2002),
		RGB_VECTOR		((short)2003),
		RGBA_VECTOR		((short)2004),
		SHAPE			((short)2005);
		
		private short intent;
		
		Intent(short intent) {
			this.intent = intent;
		}
		
		boolean isStatistic() {
			// NIFTI_FIRST_STATCODE     2
			// NIFTI_LAST_STATCODE     24
			return intent >= CORREL.intent && intent <= LOG10PVAL.intent;
		}
		
		// http://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields

		static final Map<Short,Intent> map = new HashMap<Short,Intent>();
		
		static final Intent getIntent(short intent) {
			return map.get(new Short(intent));
		}
		
		static {
			for (Intent i : Intent.values()) {
				map.put(new Short(i.intent),i);
			}
		}
	}

	enum DataType {
		NONE		((short)0),
		UINT8		((short)2),
		INT16		((short)4),
		INT32		((short)8),
		FLOAT32		((short)16),
		COMPLEX64	((short)32),
		FLOAT64		((short)64),
		RGB24		((short)128),
		INT8		((short)256),
		UINT16		((short)512),
		UINT32		((short)768),
		INT64		((short)1024),
		UINT64		((short)1280),
		FLOAT128	((short)1536),
		COMPLEX128	((short)1792),
		COMPLEX256	((short)2048),
		RGBA32		((short)2304);
		
		private short datatype;
		
		DataType(short datatype) {
			this.datatype = datatype;
		}
		
		// http://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields

		static final Map<Short,DataType> map = new HashMap<Short,DataType>();
		
		static final DataType getDataType(short datatype) {
			return map.get(new Short(datatype));
		}
		
		static {
			for (DataType d : DataType.values()) {
				map.put(new Short(d.datatype),d);
			}
		}
	}
	
	enum SliceOrder {
		UNKNOWN		((byte)0),
		SEQ_INC		((byte)1),
		SEQ_DEC		((byte)2),
		ALT_INC		((byte)3),
		ALT_DEC		((byte)4),
		ALT_INC2	((byte)5),
		ALT_DEC2	((byte)6);	

		private byte slice_code;
		
		SliceOrder(byte slice_code) {
			this.slice_code = slice_code;
		}
		
		// http://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields

		static final Map<Byte,SliceOrder> map = new HashMap<Byte,SliceOrder>();
		
		static final SliceOrder getSliceOrder(byte slice_code) {
			return map.get(new Byte(slice_code));
		}
		
		static {
			for (SliceOrder o : SliceOrder.values()) {
				map.put(new Byte(o.slice_code),o);
			}
		}
	}
	
	enum Units {
		UNKNOWN		((byte)0),
		METER		((byte)1),
		MM			((byte)2),
		MICRON		((byte)3),
		SEC			((byte)8),
		MSEC		((byte)16),
		USEC		((byte)24),
		HZ			((byte)32),
		PPM			((byte)40),
		RADS		((byte)48);

		private byte units_code;
		
		Units(byte units_code) {
			this.units_code = units_code;
		}
		
		// http://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields

		static final Map<Byte,Units> map = new HashMap<Byte,Units>();
		
		static final Units getUnits(byte units_code) {
			return map.get(new Byte(units_code));
		}
		
		static {
			for (Units u : Units.values()) {
				map.put(new Byte(u.units_code),u);
			}
		}
	}

	enum CoordinateTransform {
		UNKNOWN			((byte)0),
		SCANNER_ANAT	((byte)1),
		ALIGNED_ANAT	((byte)2),
		TALAIRACH		((byte)3),
		MNI_152			((byte)4);

		private short xform_code;
		
		CoordinateTransform(short xform_code) {
			this.xform_code = xform_code;
		}
		
		// http://stackoverflow.com/questions/443980/why-cant-enums-constructor-access-static-fields

		static final Map<Short,CoordinateTransform> map = new HashMap<Short,CoordinateTransform>();
		
		static final CoordinateTransform getCoordinateTransform(short xform_code) {
			return map.get(new Short(xform_code));
		}
		
		static {
			for (CoordinateTransform x : CoordinateTransform.values()) {
				map.put(new Short(x.xform_code),x);
			}
		}
	}
	
	public byte[] bytes;
	
	public boolean bigEndian;
	
	// use (more or less) the same names as in "official" nifti1.h specification from NIH
	
	public int sizeof_hdr;
	public byte dim_info;
	public short[] dim = new short[8];
	public float intent_p1;
	public float intent_p2;
	public float intent_p3;
	public short intent_code;
	public Intent intent;
	public short datatype_code;
	public DataType datatype;
	public short bitpix;
	public short slice_start;
	public float[] pixdim = new float[8];
	public float vox_offset;
	public float scl_slope;
	public float scl_inter;
	public short slice_end;
	public byte slice_code;
	public SliceOrder slice_order;
	public byte xyzt_units_code;
	public byte xyzt_units_code_spatial;
	public Units xyzt_units_spatial;
	public byte xyzt_units_code_temporal;
	public Units xyzt_units_temporal;
	public float cal_max;
	public float cal_min;
	public float slice_duration;
	public float toffset;
	public byte[] description = new byte[80];
	public byte[] aux_file = new byte[24];
	public short qform_code;
	public CoordinateTransform qform;
	public short sform_code;
	public CoordinateTransform sform;
	public float quatern_b;
	public float quatern_c;
	public float quatern_d;
	public float qoffset_x;
	public float qoffset_y;
	public float qoffset_z;
	public float[] srow_x = new float[4];
	public float[] srow_y = new float[4];
	public float[] srow_z = new float[4];
	public byte[] intent_name = new byte[16];
	public byte[] magic = new byte[4];
	
	public static boolean isNIfTI1SingleFileMagicNumber(byte[] bytes,int offset) {
		return bytes[offset] == 0x6E /*'n'*/ && bytes[offset+1] == 0x2B /*'+'*/  && bytes[offset+2] == 0x31 /*'1'*/ && bytes[offset+3] == 0x00;
	}

	public static boolean isNIfTI1DualFileMagicNumber(byte[] bytes,int offset) {
		return bytes[offset] == 0x6E /*'n'*/ && bytes[offset+1] == 0x69 /*'i'*/  && bytes[offset+2] == 0x31 /*'1'*/ && bytes[offset+3] == 0x00;
	}
	
	public boolean isNIfTI1SingleFileMagicNumber() {
		return isNIfTI1SingleFileMagicNumber(bytes,MAGIC_OFFSET);
	}
	
	public boolean isNIfTI1DualFileMagicNumber() {
		return isNIfTI1DualFileMagicNumber(bytes,MAGIC_OFFSET);
	}
	
	public static File getImageDataFile(File headerFile) {
		return new File(headerFile.getParent(),headerFile.getName().replaceFirst("[.][^.]*$",".img"));
	}

	public NIfTI1Header(File inputFile) throws IOException, NIfTI1Exception {
		BinaryInputStream fhin = new BinaryInputStream(inputFile,false/*assume little endian to start*/);
		bytes = new byte[FIXED_HEADER_LENGTH];
		fhin.readInsistently(bytes,0,FIXED_HEADER_LENGTH);
		fhin.close();
//System.err.println("Entire header = "+com.pixelmed.utils.HexDump.dump(bytes));
//System.err.println("Magic number = "+com.pixelmed.utils.HexDump.dump(bytes,MAGIC_OFFSET,4));
		if (isNIfTI1SingleFileMagicNumber()
		 || isNIfTI1DualFileMagicNumber()) {
			BinaryInputStream hin = new BinaryInputStream(new ByteArrayInputStream(bytes),false/*assume little endian to start*/);
			hin.mark(4);
			// one is theoretically supposed to use dim[0] to determine byte order, but no reason not to use the earlier sizeof_hdr
			sizeof_hdr =  (int)hin.readUnsigned32();
			if (sizeof_hdr != FIXED_HEADER_LENGTH) {
				hin.setBigEndian();
				hin.reset();
				sizeof_hdr = (int)hin.readUnsigned32();
				if (sizeof_hdr != FIXED_HEADER_LENGTH) {
					throw new NIfTI1Exception("Cannot determine NIfTI-1 endianness from sizeof_hdr byte order since does not match required size");
				}
				bigEndian = true;
				slf4jlogger.info("Is BigEndian");
			}
			else {
				bigEndian = false;
				slf4jlogger.info("Is LittleEndian");
			}
			slf4jlogger.info("Have NIfTI-1 header");
			
			hin.skip(35);
			dim_info = (byte)hin.readUnsigned8();
			slf4jlogger.info("dim_info = {}",((int)dim_info&0xff));
			for (int i=0; i<dim.length; ++i) {
				dim[i] = (short)hin.readUnsigned16();
				slf4jlogger.info("dim{} = {}",i,dim[i]);
			}
			intent_p1 = hin.readFloat();
			slf4jlogger.info("intent_p1 = {}",intent_p1);
			intent_p2 = hin.readFloat();
			slf4jlogger.info("intent_p2 = {}",intent_p2);
			intent_p3 = hin.readFloat();
			slf4jlogger.info("intent_p3 = {}",intent_p3);
			intent_code = (short)hin.readUnsigned16();
			intent = Intent.getIntent(intent_code);
			slf4jlogger.info("intent = {} {}",((int)intent_code&0xffff),intent);
			datatype_code = (short)hin.readUnsigned16();
			datatype = DataType.getDataType(datatype_code);
			slf4jlogger.info("datatype = {} {}",((int)datatype_code&0xffff),datatype);
			bitpix = (short)hin.readUnsigned16();
			slf4jlogger.info("bitpix = {}",((int)bitpix&0xffff));
			slice_start = (short)hin.readUnsigned16();
			slf4jlogger.info("slice_start = {}",((int)slice_start&0xffff));
			for (int i=0; i<pixdim.length; ++i) {
				pixdim[i] = hin.readFloat();
				slf4jlogger.info("pixdim[{}] = {}",i,pixdim[i]);
			}
			vox_offset = hin.readFloat();
			slf4jlogger.info("vox_offset = {}",vox_offset);
			scl_slope = hin.readFloat();
			slf4jlogger.info("scl_slope = {}",scl_slope);
			scl_inter = hin.readFloat();
			slf4jlogger.info("scl_inter = {}",scl_inter);
			slice_end = (short)hin.readUnsigned16();
			slf4jlogger.info("slice_end = {}",((int)slice_end&0xffff));
			slice_code = (byte)hin.readUnsigned8();
			slice_order = SliceOrder.getSliceOrder(slice_code);
			slf4jlogger.info("slice_code = {} {}",((int)slice_code&0xff),slice_order);

			// From nifti1.h ...
			//	Bits 0..2 of xyzt_units specify the units of pixdim[1..3]
			//	(e.g., spatial units are values 1..7).
			//	Bits 3..5 of xyzt_units specify the units of pixdim[4]
			//	(e.g., temporal units are multiples of 8).
			xyzt_units_code = (byte)hin.readUnsigned8();
			xyzt_units_code_spatial  = (byte)(xyzt_units_code & 0x03);
			xyzt_units_code_temporal = (byte)(xyzt_units_code & 0x1c);
			xyzt_units_spatial  = Units.getUnits(xyzt_units_code_spatial);
			xyzt_units_temporal = Units.getUnits(xyzt_units_code_temporal);
			slf4jlogger.info("xyzt_units_code = {}",((int)xyzt_units_code&0xff));
			slf4jlogger.info("xyzt_units_spatial = {} {}",((int)xyzt_units_code_spatial&0xff),xyzt_units_spatial);
			slf4jlogger.info("xyzt_units_temporal = {} {}",((int)xyzt_units_code_temporal&0xff),xyzt_units_temporal);

			cal_max = hin.readFloat();
			slf4jlogger.info("cal_max = {}",cal_max);
			cal_min = hin.readFloat();
			slf4jlogger.info("cal_min = {}",cal_min);
			slice_duration = hin.readFloat();
			slf4jlogger.info("slice_duration = {}",slice_duration);
			toffset = hin.readFloat();
			slf4jlogger.info("toffset = {}",toffset);
	
			hin.skip(8);
			
			hin.readInsistently(description,0,description.length);
			slf4jlogger.info("description = {}",new String(description,Charset.forName("US-ASCII")));
			hin.readInsistently(aux_file,0,aux_file.length);
			slf4jlogger.info("aux_file = {}",new String(aux_file,Charset.forName("US-ASCII")));
			qform_code = (short)hin.readUnsigned16();
			qform = CoordinateTransform.getCoordinateTransform(qform_code);
			slf4jlogger.info("qform = {} {}",((int)qform_code&0xffff),qform);
			sform_code = (short)hin.readUnsigned16();
			sform = CoordinateTransform.getCoordinateTransform(sform_code);
			slf4jlogger.info("sform = {} {}",((int)sform_code&0xffff),sform);
			quatern_b = hin.readFloat();
			slf4jlogger.info("quatern_b = {}",quatern_b);
			quatern_c = hin.readFloat();
			slf4jlogger.info("quatern_c = {}",quatern_c);
			quatern_d = hin.readFloat();
			slf4jlogger.info("quatern_d = {}",quatern_d);
			qoffset_x = hin.readFloat();
			slf4jlogger.info("qoffset_x = {}",qoffset_x);
			qoffset_y = hin.readFloat();
			slf4jlogger.info("qoffset_y = {}",qoffset_y);
			qoffset_z = hin.readFloat();
			slf4jlogger.info("qoffset_z = {}",qoffset_z);
			for (int i=0; i<srow_x.length; ++i) {
				srow_x[i] = hin.readFloat();
				slf4jlogger.info("srow_x[{}] = {}",i,srow_x[i]);
			}
			for (int i=0; i<srow_y.length; ++i) {
				srow_y[i] = hin.readFloat();
				slf4jlogger.info("srow_y[{}] = {}",i,srow_y[i]);
			}
			for (int i=0; i<srow_z.length; ++i) {
				srow_z[i] = hin.readFloat();
				slf4jlogger.info("srow_z[{}] = {}",i,srow_z[i]);
			}
			hin.readInsistently(intent_name,0,intent_name.length);
			slf4jlogger.info("intent_name = {}",new String(intent_name,Charset.forName("US-ASCII")));
			hin.readInsistently(magic,0,magic.length);
			slf4jlogger.info("magic = {}",new String(magic,Charset.forName("US-ASCII")));

		}
		else {
			throw new NIfTI1Exception("Not a NIfTI-1 magic number");
		}
	}

	/**
	 * <p>Read a NIfTI-1 image input format files and dump header.</p>
	 *
	 * @param	arg	the inputFile,
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 1) {
				new NIfTI1Header(new File(arg[0]));
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: NIfTI1Header inputFile");
				System.exit(1);
			}
		}
		catch (Exception e) {
			slf4jlogger.error("",e);	// use SLF4J since may be invoked from script
		}
	}
}

