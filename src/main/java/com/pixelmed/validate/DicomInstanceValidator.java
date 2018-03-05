/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.validate;

import com.pixelmed.dicom.*;

// JAXP packages
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.Document;

import java.io.*;

/**
 * <p>The {@link DicomInstanceValidator DicomInstanceValidator} class is
 * for validating composite storage SOP instances against the standard IOD for the corresponding storage SOP Class.</p>
 *
 * <p>Typically used by reading the list of attributes that comprise an object, validating them
 * and displaying the resulting string results to the user on the standard output, in a dialog
 * box or whatever. The basic implementation of the {@link #main main} method (that may be useful as a
 * command line utility in its own right) is as follows:</p>
 *
 * <pre>
 * 	AttributeList list = new AttributeList();
 * 	list.read(arg[0],null,true,true);
 * 	DicomInstanceValidator validator = new DicomInstanceValidator();
 * 	System.err.print(validator.validate(list));
 * </pre>
 *
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class DicomInstanceValidator {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/validate/DicomInstanceValidator.java,v 1.9 2017/01/24 10:50:52 dclunie Exp $";

	/***/
	private Transformer transformer;
	
	private class OurURIResolver implements URIResolver {
		/**
		 * @param	href
		 * @param	base
		 */
		public Source resolve(String href,String base) throws TransformerException {
//System.err.println("OurURIResolver.resolve() href="+href+" base="+base);
			InputStream stream = DicomInstanceValidator.class.getResourceAsStream("/com/pixelmed/validate/"+href);
			return new StreamSource(stream);
		}
	}

	/**
	 * <p>Create an instance of validator.</p>
	 *
	 * <p>Once created, a validator may be reused for as many validations as desired.</p>
	 *
	 * @throws	javax.xml.transform.TransformerConfigurationException
	 */
	public DicomInstanceValidator() throws javax.xml.transform.TransformerConfigurationException {
		InputStream transformStream = DicomInstanceValidator.class.getResourceAsStream("/com/pixelmed/validate/"+"DicomIODDescriptionsCompiled.xsl");
		Source transformSource = new StreamSource(transformStream);
		TransformerFactory tf = TransformerFactory.newInstance();
		tf.setURIResolver(new OurURIResolver());			// this helps us find the common rules in the jar file
		transformer = tf.newTransformer(transformSource);
	}

	/**
	 * <p>Validate a DICOM composite storage instance against the standard IOD for the appropriate storage SOP Class.</p>
	 *
	 * @param	list	the list of attributes comprising the DICOM composite storage instance to be validated
	 * @return		a string describing the results of the validation
	 * @throws	javax.xml.parsers.ParserConfigurationException
	 * @throws	javax.xml.transform.TransformerException
	 * @throws	java.io.UnsupportedEncodingException
	 */
	public String validate(AttributeList list) throws
			javax.xml.parsers.ParserConfigurationException,
			javax.xml.transform.TransformerException,
			java.io.UnsupportedEncodingException {
		Document inputDocument = new XMLRepresentationOfDicomObjectFactory().getDocument(list);
		Source inputSource = new DOMSource(inputDocument);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		StreamResult outputResult = new StreamResult(outputStream);
		transformer.transform(inputSource,outputResult);
		return outputStream.toString("UTF-8");
	}

	/**
	 * <p>Read the DICOM file specified on the command line and validate it against the standard IOD for the appropriate storage SOP Class.</p>
	 *
	 * <p>The result of the validation is printed to the standard output.</p>
	 *
	 * @param	arg	the name of the file containing the DICOM composite storage instance to be validated
	 */
	public static void main(String arg[]) {
		try {
			AttributeList list = new AttributeList();
			list.read(arg[0],TagFromName.PixelData);
			DicomInstanceValidator validator = new DicomInstanceValidator();
			System.out.print(validator.validate(list));	// no need to use SLF4J since command line utility/test
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

