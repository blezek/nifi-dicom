/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.PrintStream;
import java.text.NumberFormat;

/**
 * <p>Test creating and getting Attributes and their values from attributes and lists.</p>
 *
 * @author	dclunie
 */
class AttributeTest {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTest.java,v 1.5 2017/01/24 10:50:35 dclunie Exp $";

	boolean testArrayOfShortComparison(String testName,String methodName,short[] valuesSupplied,short[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				if (valuesSupplied[i] != valuesActual[i]) {
					log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valuesActual[i]+"\", but expected \""+valuesSupplied[i]+"\"");
					success = false;
				}
			}
		}
		return success;
	}

	boolean testArrayOfIntComparison(String testName,String methodName,int[] valuesSupplied,int[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				if (valuesSupplied[i] != valuesActual[i]) {
					log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valuesActual[i]+"\", but expected \""+valuesSupplied[i]+"\"");
					success = false;
				}
			}
		}
		return success;
	}

	boolean testArrayOfLongComparison(String testName,String methodName,long[] valuesSupplied,long[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				if (valuesSupplied[i] != valuesActual[i]) {
					log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valuesActual[i]+"\", but expected \""+valuesSupplied[i]+"\"");
					success = false;
				}
			}
		}
		return success;
	}

	boolean testArrayOfFloatComparison(String testName,String methodName,float[] valuesSupplied,float[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				if (valuesSupplied[i] != valuesActual[i]) {
					log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valuesActual[i]+"\", but expected \""+valuesSupplied[i]+"\"");
					success = false;
				}
			}
		}
		return success;
	}

	boolean testArrayOfDoubleComparison(String testName,String methodName,double[] valuesSupplied,double[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				if (valuesSupplied[i] != valuesActual[i]) {
					log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valuesActual[i]+"\", but expected \""+valuesSupplied[i]+"\"");
					success = false;
				}
			}
		}
		return success;
	}

	boolean testArrayOfStringComparison(String testName,String methodName,String[] valuesSupplied,String[] valuesActual,int vmSupplied,PrintStream log) {
		boolean success = true;
		int valuesActualLength = valuesActual.length;
		if (valuesActualLength != vmSupplied) {
			log.println(testName+": "+"Failed - "+methodName+" returned array of length "+valuesActualLength+" expected "+vmSupplied);
			success = false;
		}
		else {
			for (int i=0; i<vmSupplied; ++i) {
				String valueActual = valuesActual[i];
				if (valueActual == null) {
					if (valuesSupplied[i] != null) {
						log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as null, but expected \""+valuesSupplied[i]+"\"");
						success = false;
					}
					// else supplied null, so OK
				}
				else {
					if (!valuesSupplied[i].equals(valueActual)) {
						log.println(testName+": "+"Failed - "+methodName+" returned value ["+i+"] as \""+valueActual+"\", but expected \""+valuesSupplied[i]+"\"");
						success = false;
					}
				}
			}
		}
		return success;
	}
	
	boolean testSingleStringComparison(String testName,String methodName,String valueSupplied,String valueActual,PrintStream log) {
		boolean success = true;
		if (valueActual == null) {
			if (valueSupplied != null) {
				log.println(testName+": "+"Failed - "+methodName+" returned value  as null, but expected \""+valueSupplied+"\"");
				success = false;
			}
			// else supplied null, so OK
		}
		else {
			if (!valueSupplied.equals(valueActual)) {
				log.println(testName+": "+"Failed - "+methodName+" returned value as \""+valueActual+"\", but expected \""+valueSupplied+"\"");
				success = false;
			}
		}
		return success;
	}

	
	boolean testAttributeWithValues(String testName,Attribute a,String[] valuesSupplied,String delimitedValuesSupplied,PrintStream log,boolean isNumericAttribute,boolean isStringAttribute) {
		return testAttributeWithValues(testName,a,valuesSupplied,delimitedValuesSupplied,log,isNumericAttribute,isStringAttribute,null,null,null,null,null);
	}
	
	boolean testAttributeWithValues(String testName,Attribute a,String[] valuesSupplied,String delimitedValuesSupplied,PrintStream log,boolean isNumericAttribute,boolean isStringAttribute,short[] shortValues,int[] intValues,long[] longValues,float[] floatValues,double[] doubleValues) {
		int vmSupplied = valuesSupplied.length;
		for (int i=0; i<vmSupplied; ++i) {
			try {
				a.addValue(valuesSupplied[i]);
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - addValue() threw Exception "+e);
				return false;			// cannot proceed
			}
		}
		{
			int vmActual = a.getVM();
			if (vmActual != vmSupplied) {
				log.println(testName+": "+"Failed - getVM() returned "+vmActual+" expected "+vmSupplied);
				return false;			// cannot proceed
			}
		}
		
		boolean success = true;
		
		// Tests of fetching numeric values using non-static methods ...
		
		if (isNumericAttribute) {
			if (shortValues != null) {
				String methodName = "getShortValues()";
				try {
					boolean testWasSuccessful = testArrayOfShortComparison(testName,methodName,shortValues,a.getShortValues(),vmSupplied,log);
					success = success && testWasSuccessful;
				}
				catch (Exception e) {
					log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
					success = false;
				}
			}
			if (intValues != null) {
				String methodName = "getIntegerValues()";
				try {
					boolean testWasSuccessful = testArrayOfIntComparison(testName,methodName,intValues,a.getIntegerValues(),vmSupplied,log);
					success = success && testWasSuccessful;
				}
				catch (Exception e) {
					log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
					success = false;
				}
			}
			if (longValues != null) {
				String methodName = "getLongValues()";
				try {
					boolean testWasSuccessful = testArrayOfLongComparison(testName,methodName,longValues,a.getLongValues(),vmSupplied,log);
					success = success && testWasSuccessful;
				}
				catch (Exception e) {
					log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
					success = false;
				}
			}
			if (floatValues != null) {
				String methodName = "getFloatValues()";
				try {
					boolean testWasSuccessful = testArrayOfFloatComparison(testName,methodName,floatValues,a.getFloatValues(),vmSupplied,log);
					success = success && testWasSuccessful;
				}
				catch (Exception e) {
					log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
					success = false;
				}
			}
			if (doubleValues != null) {
				String methodName = "getDoubleValues()";
				try {
					boolean testWasSuccessful = testArrayOfDoubleComparison(testName,methodName,doubleValues,a.getDoubleValues(),vmSupplied,log);
					success = success && testWasSuccessful;
				}
				catch (Exception e) {
					log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
					success = false;
				}
			}
		}
		
		// Tests of fetching String[] using non-static methods ...

		if (isStringAttribute) {
			String methodName = "getOriginalStringValues()";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,a.getOriginalStringValues(),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		{
			String methodName = "getStringValues((NumberFormat)null)";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,a.getStringValues((NumberFormat)null),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		// Tests of fetching individual String values using non-static convenience methods ...
		
		{
			String methodName = "getSingleStringValueOrDefault(String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],a.getSingleStringValueOrDefault(dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "getSingleStringValueOrDefault(String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],a.getSingleStringValueOrDefault(dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		// Tests of fetching delimited String values using non-static convenience methods ...
		
		{
			String methodName = "getDelimitedStringValuesOrDefault(String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,a.getDelimitedStringValuesOrDefault(dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "getDelimitedStringValuesOrDefault(String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,a.getDelimitedStringValuesOrDefault(dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}




		// Tests of fetching String[] using static methods with Attribute argument ...
		
		{
			String methodName = "static getStringValues(Attribute)";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,Attribute.getStringValues(a),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		{
			String methodName = "static getStringValues(Attribute,(NumberFormat)null)";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,Attribute.getStringValues(a,(NumberFormat)null),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		// Tests of fetching individual String values using static convenience methods with Attribute argument ...
		
		{
			String methodName = "static getSingleStringValueOrDefault(Attribute,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrDefault(a,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrDefault(Attribute,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrDefault(a,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrEmptyString(Attribute)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrEmptyString(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(Attribute,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrEmptyString(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrNull(Attribute)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrNull(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrNull(Attribute,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrNull(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		// Tests of fetching delimited String values using static convenience methods with Attribute argument ...
		
		{
			String methodName = "static getDelimitedStringValuesOrDefault(Attribute,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrDefault(a,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrDefault(Attribute,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrDefault(a,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(Attribute)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrEmptyString(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(Attribute,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrEmptyString(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(Attribute)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrNull(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(Attribute,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrNull(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		AttributeList list = new AttributeList();
		list.put(a);
		AttributeTag tag = a.getTag();
		
		// Tests of fetching String[] using static methods with AttributeList and AttributeTag arguments ...
		
		{
			String methodName = "static getStringValues(AttributeList,AttributeTag)";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,Attribute.getStringValues(list,tag),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		{
			String methodName = "static getStringValues(AttributeList,AttributeTag,(NumberFormat)null)";
			try {
				boolean testWasSuccessful = testArrayOfStringComparison(testName,methodName,valuesSupplied,Attribute.getStringValues(list,tag,(NumberFormat)null),vmSupplied,log);
				success = success && testWasSuccessful;
			}
			catch (Exception e) {
				log.println(testName+": "+"Failed - "+methodName+" threw Exception "+e);
				success = false;
			}
		}
		
		// Tests of fetching individual String values using static convenience methods with AttributeList and AttributeTag arguments ...
		
		{
			String methodName = "static getSingleStringValueOrDefault(AttributeList,AttributeTag,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrDefault(list,tag,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrDefault(AttributeList,AttributeTag,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrDefault(list,tag,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrEmptyString(AttributeList,AttributeTag)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrEmptyString(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrEmptyString(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrNull(AttributeList,AttributeTag)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrNull(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrNull(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,valuesSupplied[0],Attribute.getSingleStringValueOrNull(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		// Tests of fetching delimited String values using static convenience methods with AttributeList and AttributeTag arguments ...
		
		{
			String methodName = "static getDelimitedStringValuesOrDefault(AttributeList,AttributeTag,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrDefault(list,tag,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrDefault(AttributeList,AttributeTag,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrDefault(list,tag,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(AttributeList,AttributeTag)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrEmptyString(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrEmptyString(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(AttributeList,AttributeTag)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrNull(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,delimitedValuesSupplied,Attribute.getDelimitedStringValuesOrNull(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}


		return success;
	}
	
	boolean testAttributeWithoutValues(String testName,Attribute a,PrintStream log,boolean isNumericAttribute,boolean isStringAttribute) {
		boolean success = true;
		
		// Tests of fetching individual String values using non-static convenience methods ...
		
		{
			String methodName = "getSingleStringValueOrDefault(String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,a.getSingleStringValueOrDefault(dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "getSingleStringValueOrDefault(String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,a.getSingleStringValueOrDefault(dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		// Tests of fetching individual String values static convenience methods with Attribute argument ...
		
		{
			String methodName = "static getSingleStringValueOrDefault(Attribute,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrDefault(a,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrDefault(Attribute,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrDefault(a,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(Attribute)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrEmptyString(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(Attribute,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrEmptyString(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrNull(Attribute)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrNull(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrNull(Attribute,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrNull(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		// Tests of fetching delimited String values using static convenience methods with Attribute argument ...
		
		{
			String methodName = "static getDelimitedStringValuesOrDefault(Attribute,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrDefault(a,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrDefault(Attribute,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrDefault(a,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(Attribute)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrEmptyString(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(Attribute,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrEmptyString(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(Attribute)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrNull(a),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(Attribute,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrNull(a,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		AttributeList list = new AttributeList();
		list.put(a);
		AttributeTag tag = a.getTag();
		
		// Tests of fetching individual String values static convenience methods with AttributeList and AttributeTag arguments ...
		
		{
			String methodName = "static getSingleStringValueOrDefault(AttributeList,AttributeTag,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrDefault(list,tag,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrDefault(AttributeList,AttributeTag,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrDefault(list,tag,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(AttributeList,AttributeTag)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrEmptyString(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrEmptyString(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrEmptyString(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		{
			String methodName = "static getSingleStringValueOrNull(AttributeList,AttributeTag)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrNull(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getSingleStringValueOrNull(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getSingleStringValueOrNull(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}
		
		// Tests of fetching delimited String values using static convenience methods with AttributeList and AttributeTag arguments ...
		
		{
			String methodName = "static getDelimitedStringValuesOrDefault(AttributeList,AttributeTag,String dflt)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrDefault(list,tag,dflt),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrDefault(AttributeList,AttributeTag,String dflt,(NumberFormat)null)";
			String dflt = "DEFAULT";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrDefault(list,tag,dflt,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(AttributeList,AttributeTag)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrEmptyString(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrEmptyString(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = "";
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrEmptyString(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(AttributeList,AttributeTag)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrNull(list,tag),log);
				success = success && testWasSuccessful;
			}
		}

		{
			String methodName = "static getDelimitedStringValuesOrNull(AttributeList,AttributeTag,(NumberFormat)null)";
			String dflt = null;
			{
				boolean testWasSuccessful = testSingleStringComparison(testName,methodName,dflt,Attribute.getDelimitedStringValuesOrNull(list,tag,(NumberFormat)null),log);
				success = success && testWasSuccessful;
			}
		}

		return success;
	}





	boolean test(PrintStream log) {
		boolean overallSucess = true;
		{
			String[] values = { "BLA1","BLA2" };
			String delimitedValues = "BLA1\\BLA2";
			boolean testWasSuccessful = testAttributeWithValues("CodeString",new CodeStringAttribute(TagFromName.ImageType),values,delimitedValues,log,false/*isNumericAttribute*/,true/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("CodeString",new CodeStringAttribute(TagFromName.ImageType),log,false/*isNumericAttribute*/,true/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1", "2", "0", "-2147483648", "2147483647" };
			String delimitedValues = "1\\2\\0\\-2147483648\\2147483647";
			short[] shortValues = { 1, 2, 0, 0, 0 };					// Note that the too large value is returned as 0, not -1 (contrast with SL)
			int[] intValues = { 1, 2, 0, -2147483648, 2147483647 };
			long[] longValues = { 1l, 2l, 0l, -2147483648l, 2147483647l };
			float[] floatValues = { 1f, 2f, 0f, -2147483648f, 2147483647f };
			double[] doubleValues = { 1d, 2d, 0d, -2147483648d, 2147483647d };
			boolean testWasSuccessful = testAttributeWithValues("IntegerString",new IntegerStringAttribute(TagFromName.SeriesNumber),values,delimitedValues,log,true/*isNumericAttribute*/,true/*isStringAttribute*/,
				shortValues,intValues,longValues,floatValues,doubleValues);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("IntegerString",new IntegerStringAttribute(TagFromName.SeriesNumber),log,true/*isNumericAttribute*/,true/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1", "2", "0", "65535" };
			String delimitedValues = "1\\2\\0\\65535";
			short[] shortValues = { 1, 2, 0, (short)65535 };
			int[] intValues = { 1, 2, 0, 65535 };
			long[] longValues = { 1l, 2l, 0l, 65535l };
			float[] floatValues = { 1f, 2f, 0f, 65535f };
			double[] doubleValues = { 1d, 2d, 0d, 65535d };
			boolean testWasSuccessful = testAttributeWithValues("UnsignedShort",new UnsignedShortAttribute(TagFromName.BitsStored),values,delimitedValues,log,true/*isNumericAttribute*/,false/*isStringAttribute*/,
				shortValues,intValues,longValues,floatValues,doubleValues);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("UnsignedShort",new UnsignedShortAttribute(TagFromName.BitsStored),log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1", "2", "0", "-32768", "32767" };
			String delimitedValues = "1\\2\\0\\-32768\\32767";
			short[] shortValues = { 1, 2, 0, (short)-32768, (short)32767 };
			int[] intValues = { 1, 2, 0, -32768, 32767 };
			long[] longValues = { 1l, 2l, 0l, -32768l, 32767l };
			float[] floatValues = { 1f, 2f, 0f, -32768f, 32767f };
			double[] doubleValues = { 1d, 2d, 0d, -32768d, 32767d };
			boolean testWasSuccessful = testAttributeWithValues("SignedShort",new SignedShortAttribute(TagFromName.BitsStored),values,delimitedValues,log,true/*isNumericAttribute*/,false/*isStringAttribute*/,
				shortValues,intValues,longValues,floatValues,doubleValues);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("SignedShort",new SignedShortAttribute(TagFromName.BitsStored),log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1", "2", "0", "4294967295" };
			String delimitedValues = "1\\2\\0\\4294967295";
			short[] shortValues = { 1, 2, 0, (short)0xffff };
			int[] intValues = { 1, 2, 0, 0xffffffff };
			long[] longValues = { 1l, 2l, 0l, 4294967295l };
			float[] floatValues = { 1f, 2f, 0f, 4294967295f };
			double[] doubleValues = { 1d, 2d, 0d, 4294967295d };
			boolean testWasSuccessful = testAttributeWithValues("UnsignedLong",new UnsignedLongAttribute(TagFromName.BitsStored),values,delimitedValues,log,true/*isNumericAttribute*/,false/*isStringAttribute*/,
				shortValues,intValues,longValues,floatValues,doubleValues);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("UnsignedLong",new UnsignedLongAttribute(TagFromName.BitsStored),log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1", "2", "0", "-2147483648", "2147483647" };
			String delimitedValues = "1\\2\\0\\-2147483648\\2147483647";
			short[] shortValues = { 1, 2, 0, 0, (short)0xffff };					// Note that the too large value is returned as -1, not 0 (contrast with IS)
			int[] intValues = { 1, 2, 0, -2147483648, 2147483647 };
			long[] longValues = { 1l, 2l, 0l, -2147483648l, 2147483647l };
			float[] floatValues = { 1f, 2f, 0f, -2147483648f, 2147483647f };
			double[] doubleValues = { 1d, 2d, 0d, -2147483648d, 2147483647d };
			boolean testWasSuccessful = testAttributeWithValues("SignedLong",new SignedLongAttribute(TagFromName.BitsStored),values,delimitedValues,log,true/*isNumericAttribute*/,false/*isStringAttribute*/,
				shortValues,intValues,longValues,floatValues,doubleValues);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("SignedLong",new SignedLongAttribute(TagFromName.BitsStored),log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1.5","2.5" };
			String delimitedValues = "1.5\\2.5";
			boolean testWasSuccessful = testAttributeWithValues("DecimalString",new DecimalStringAttribute(TagFromName.SliceThickness),values,delimitedValues,log,true/*isNumericAttribute*/,true/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("DecimalString",new DecimalStringAttribute(TagFromName.SliceThickness),log,true/*isNumericAttribute*/,true/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		{
			String[] values = { "1.5","2.5" };
			String delimitedValues = "1.5\\2.5";
			boolean testWasSuccessful = testAttributeWithValues("FloatSingle",new FloatSingleAttribute(TagFromName.BeamAngle),values,delimitedValues,log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}
		{
			boolean testWasSuccessful = testAttributeWithoutValues("FloatSingle",new FloatSingleAttribute(TagFromName.BeamAngle),log,true/*isNumericAttribute*/,false/*isStringAttribute*/);
			overallSucess = overallSucess && testWasSuccessful;
		}

		log.println("Overall: "+(overallSucess ? "Passed" : "Failed"));
		return overallSucess;
	}
	
	public static void main(String arg[]) {
		new AttributeTest().test(System.err);
	}
	
}

// 	public byte[]   getByteValues()              throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }
// 	public short[]  getShortValues()             throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }
// 	public int[]    getIntegerValues()           throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }
// 	public long[]   getLongValues()              throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }
// 	public float[]  getFloatValues()             throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }
// 	public double[] getDoubleValues()            throws DicomException { throw new DicomException("internal error - wrong value type for attribute "+tag); }

// 	public int getSingleIntegerValueOrDefault(int dflt) {
// 	public double getSingleDoubleValueOrDefault(double dflt) {
// 	public long getSingleLongValueOrDefault(long dflt) {

// 	public static int getSingleIntegerValueOrDefault(Attribute a,int dflt) {
// 	public static int[] getIntegerValues(Attribute a) {
// 	public static long getSingleLongValueOrDefault(Attribute a,long dflt) {
// 	public static long[] getLongValues(Attribute a) {
// 	public static double getSingleDoubleValueOrDefault(Attribute a,double dflt) {
// 	public static double[] getDoubleValues(Attribute a) {

// 	public static int getSingleIntegerValueOrDefault(AttributeList list,AttributeTag tag,int dflt) {
// 	public static int[] getIntegerValues(AttributeList list,AttributeTag tag) {
// 	public static long getSingleLongValueOrDefault(AttributeList list,AttributeTag tag,long dflt) {
// 	public static long[] getLongValues(AttributeList list,AttributeTag tag) {
// 	public static double getSingleDoubleValueOrDefault(AttributeList list,AttributeTag tag,double dflt) {
// 	public static double[] getDoubleValues(AttributeList list,AttributeTag tag) {


// AgeStringAttribute.java
// ApplicationEntityAttribute.java
// DateAttribute.java
// DateTimeAttribute.java
// DecimalStringAttribute.java
// FloatDoubleAttribute.java
// FloatSingleAttribute.java
// LongStringAttribute.java
// LongTextAttribute.java
// PersonNameAttribute.java
// ShortStringAttribute.java
// ShortTextAttribute.java
// SignedLongAttribute.java
// SignedShortAttribute.java
// TimeAttribute.java
// UniqueIdentifierAttribute.java
// UnlimitedTextAttribute.java
// UnsignedLongAttribute.java
// 
// UnknownAttribute.java
// 
// 
// 
