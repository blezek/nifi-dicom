/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.math.BigInteger;
import java.util.UUID;

import java.util.HashSet;		// for main() testing for uniqueness

/**
 * <p>A class for creating and convertin UUID based OIDs.</p>
 *
 * <p>See <a href="http://www.itu.int/ITU-T/studygroups/com17/oid/X.667-E.pdf">ITU X.667 Information technology - Open Systems Interconnection - Procedures for the operation of OSI Registration Authorities: Generation and registration of Universally Unique Identifiers (UUIDs) and their use as ASN.1 Object Identifier components</a>.</p>
 *
 * @author	dclunie
 */

public class UUIDBasedOID {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/UUIDBasedOID.java,v 1.11 2017/02/24 13:09:42 dclunie Exp $";

	protected static final String OID_PREFIX = "2.25";	// {joint-iso-itu-t uuid(25) <uuid-single-integer-value>}
	protected static final String OID_PREFIX_REMOVAL_REGEX = "^"+OID_PREFIX+".";
	
	protected UUID uuid;
	protected String oid;
	
	public static byte[] getByteArrayInNetworkByteOrderFromUUID(UUID uuid) {
		byte[] b = new byte[16];
		// network byte order is most significant byte first) ...
		long bits = uuid.getLeastSignificantBits();
		for (int i=15; i>=8; --i) {
			b[i] = (byte)(bits & 0xff);
			bits = bits >>> 8;		// note use of unsigned right shift to bring in 0 not sign bit
		}
		bits = uuid.getMostSignificantBits();
		for (int i=7; i>=0; --i) {
			b[i] = (byte)(bits & 0xff);
			bits = bits >>> 8;		// note use of unsigned right shift to bring in 0 not sign bit
		}
		return b;
	}
	
	/**
	 * <p>Construct a new OID with a Type 3 UUID (that is based on an MD5 hash of the supplied byte arrayss).</p>
	 *
	 * <p>Given the same bytes as input, the same OID will be returned every time.</p>
	 *
	 * <p>The name space represents the entity, an instance of which is being uniquely identified.</p>
	 *
	 * <p>The name bytes might, for example, be a UTF-8 encoding of a String that contains a bunch of
	 * attribute values separated by some delimiter like a "|"</p>
	 *
	 * @param		nameSpace	a non-null UUID defining the name space
	 * @param		bName		a non-null non-zero length array of bytes containing the "name" (any values)
	 */
	public UUIDBasedOID(UUID nameSpace,byte[] bName) {
	
		// per RFC 4122 ("http://tools.ietf.org/html/rfc4122#section-4.3"), use network byte order (most significant byte first) ...
		byte[] bNameSpace = getByteArrayInNetworkByteOrderFromUUID(nameSpace);
	
		byte[] b = new byte[bNameSpace.length + bName.length];
		System.arraycopy(bNameSpace,0,b,0,                bNameSpace.length);
		System.arraycopy(bName,     0,b,bNameSpace.length,bName.length);
		uuid = UUID.nameUUIDFromBytes(b);
		oid = createOIDFromUUIDCanonicalHexString(uuid.toString());
	}
	
	/**
	 * <p>Construct a new OID with a new random UUID.</p>
	 */
	public UUIDBasedOID() {
		uuid = UUID.randomUUID();
		oid = createOIDFromUUIDCanonicalHexString(uuid.toString());
	}
	
	/**
	 * <p>Construct an OID from an existing string representation of an OID.</p>
	 *
	 * @param		oid	a String of dotted numeric values in OID form {joint-iso-itu-t uuid(25) &lt;uuid-single-integer-value&gt;}
	 */
	public UUIDBasedOID(String oid) throws IllegalArgumentException, NumberFormatException {
		this.oid = oid;
		uuid = parseUUIDFromOID(oid);
	}
	
	/**
	 * <p>Get the string representation of the OID.</p>
	 *
	 * @return	the string representation of the OID
	 */
	public String getOID() { return oid; }
	
	/**
	 * <p>Get the UUID of the OID.</p>
	 *
	 * @return	the UUID
	 */
	public UUID getUUID() { return uuid; }
	
	/**
	 * <p>Extract the UUID from a UUID-based OID.</p>
	 *
	 * @param		oid							a String of dotted numeric values in OID form {joint-iso-itu-t uuid(25) &lt;uuid-single-integer-value&gt;}
	 * @return									the UUID
	 * @throws	IllegalArgumentException	if the OID is not in the {joint-iso-itu-t uuid(25)} arc
	 * @throws	NumberFormatException		if the OID does not contain a uuid-single-integer-value
	 */
	public static UUID parseUUIDFromOID(String oid) throws IllegalArgumentException, NumberFormatException {
		if (oid == null || ! oid.startsWith(OID_PREFIX)) {
			throw new IllegalArgumentException("OID "+oid+" does not start with "+OID_PREFIX);
		}
		String decimalString = oid.replaceFirst(OID_PREFIX_REMOVAL_REGEX,"");
		return parseUUIDFromDecimalString(decimalString);
	}
	
	/**
	 * <p>Extract the UUID from its single integer value decimal string representation.</p>
	 *
	 * @param		decimalString				single integer value decimal string representation 
	 * @return									the UUID
	 * @throws	NumberFormatException		if the OID does not contain a uuid-single-integer-value
	 */
	public static UUID parseUUIDFromDecimalString(String decimalString) throws NumberFormatException {
		BigInteger decimalValue = new BigInteger(decimalString);
		long leastSignificantBits = decimalValue.longValue();
		long mostSignificantBits  = decimalValue.shiftRight(64).longValue();
		return new UUID(mostSignificantBits,leastSignificantBits);
	}
	
	/**
	 * <p>Convert an unsigned value in a long to a BigInteger.</p>
	 *
	 * @param		unsignedLongValue			an unsigned long value (i.e., the sign bit is treated as part of the value rather than a sign) 
	 * @return									the BigInteger
	 */
	public static BigInteger makeBigIntegerFromUnsignedLong(long unsignedLongValue) {
//System.err.println("makeBigIntegerFromUnsignedLong(): unsignedLongValue = "+Long.toHexString(unsignedLongValue));
		BigInteger bigValue;
		if (unsignedLongValue < 0) {
			unsignedLongValue = unsignedLongValue & Long.MAX_VALUE;
			bigValue = BigInteger.valueOf(unsignedLongValue);
			bigValue = bigValue.setBit(63);
		}
		else {
			bigValue = BigInteger.valueOf(unsignedLongValue);
		}
//System.err.println("makeBigIntegerFromUnsignedLong(): bigValue = "+com.pixelmed.utils.HexDump.dump(bigValue.toByteArray()));
		return bigValue;
	}
	
	/**
	 * <p>Create an OID from the canonical hex string form of a UUID.</p>
	 *
	 * @param		hexString					canonical hex string form of a UUID 
	 * @return									the OID
	 * @throws	IllegalArgumentException	if name does not conform to the string representation
	 */
	public static String createOIDFromUUIDCanonicalHexString(String hexString) throws IllegalArgumentException {
		UUID uuid = UUID.fromString(hexString);
		long leastSignificantBits = uuid.getLeastSignificantBits();
		long mostSignificantBits  = uuid.getMostSignificantBits();
		BigInteger decimalValue = makeBigIntegerFromUnsignedLong(mostSignificantBits);
		decimalValue = decimalValue.shiftLeft(64);
		BigInteger bigValueOfLeastSignificantBits = makeBigIntegerFromUnsignedLong(leastSignificantBits);
		decimalValue = decimalValue.or(bigValueOfLeastSignificantBits);	// not add() ... do not want to introduce question of signedness of long
		return OID_PREFIX+"."+decimalValue.toString();
	}
	
	/**
	 * <p>Convert OIDs to UUIDs and UUIDs to OIDs or create a new one.</p>
	 *
	 * @param		args	a list of OIDs or UUIDs or empty if a new OID is to be created
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println(new UUIDBasedOID().getOID());
		}
		else {
			for (String arg : args) {
				if (arg.startsWith("2.25")) {
					System.err.println(arg+" = "+new UUIDBasedOID(arg).getUUID());
				}
				else {
					System.err.println(UUIDBasedOID.createOIDFromUUIDCanonicalHexString(arg)+" = "+arg);
				}
			}
		}
	}
}


