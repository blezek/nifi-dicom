/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Shape;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.BinaryOutputStream;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.OtherByteAttribute;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.OtherByteAttributeMultipleFilesOnDisk;
import com.pixelmed.dicom.OtherByteAttributeMultipleFrameArrays;
import com.pixelmed.dicom.OtherByteAttributeOnDisk;
import com.pixelmed.dicom.OtherWordAttribute;
import com.pixelmed.dicom.OtherWordAttributeMultipleFilesOnDisk;
import com.pixelmed.dicom.OtherWordAttributeMultipleFrameArrays;
import com.pixelmed.dicom.OtherWordAttributeOnDisk;
import com.pixelmed.dicom.Overlay;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UnsignedShortAttribute;
import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>
 * A class of utility methods for editing image pixel data.
 * </p>
 *
 * @author dclunie
 */

public class ImageEditUtilities {
  private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/ImageEditUtilities.java,v 1.34 2017/01/24 10:50:40 dclunie Exp $";

  private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageEditUtilities.class);

  // protected static long
  // usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = 800*600*3*2l; //
  // set to -1l to disable temporary files of any kind
  // protected static long
  // usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = 4390716*50*2l; //
  // set to -1l to disable temporary files of any kind
  protected static long usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = Runtime.getRuntime().maxMemory() / 2;
  // protected static long
  // usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = -1l; // set to -1l
  // to disable temporary files of any kind

  protected static long useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = -1l; // set
                                                                                              // to
                                                                                              // -1l
                                                                                              // to
                                                                                              // disable
                                                                                              // use
                                                                                              // of
                                                                                              // multiple
                                                                                              // rather
                                                                                              // than
                                                                                              // single
                                                                                              // temporary
                                                                                              // file
  // protected static long
  // useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan =
  // usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan;

  static {
    slf4jlogger.info("Using usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = {}", usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan);
    slf4jlogger.info("Using useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan = {}", useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan);
  }

  private ImageEditUtilities() {
  }

  // static protected boolean isInShapes(Vector shapes,int x,int y) {
  // Iterator it = shapes.iterator();
  // while (it.hasNext()) {
  // Shape shape = (Shape)it.next();
  // if (shape.contains(x,y)) {
  // slf4jlogger.trace("isInShapes(): found ({},{})",x,y);
  // return true;
  // }
  // }
  // return false;
  // }

  /**
   * <p>
   * Blackout JPEG encoded blocks of specified regions in an image, for example
   * to remove burned in identification.
   * </p>
   *
   * <p>
   * Other JPEG blocks remain untouched, i.e., to avoid loss involved in
   * decompression and recompression of blocks that do not intersect with the
   * specified regions.
   * </p>
   *
   * <p>
   * Overlays are not burned in.
   * </p>
   *
   * <p>
   * The replacement pixel value is not controllable
   * </p>
   *
   * <p>
   * The accompanying attribute list will be updated with new Pixel Data and
   * other Image Pixel Module attributes will be unchanged.
   * </p>
   *
   * @param srcFile
   *        the DICOM file containing the JPEG compressed image to be blacked
   *        out
   * @param dstFile
   *        the DICOM file containing the JPEG compressed image with the blocks
   *        intersecting the specified regions blacked out
   * @param shapes
   *        a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape
   *        java.awt.Shape}, specifed in image-relative coordinates
   * @throws DicomException
   *         if something bad happens handling the attribute list
   * @throws IOException
   *         if something bad happens reading or writing the files
   * @throws Exception
   *         if something bad happens during processing of the JPEG blocks
   */
  static public void blackoutJPEGBlocks(File srcFile, File dstFile, Vector shapes) throws DicomException, IOException, Exception {
    DicomInputStream i = new DicomInputStream(srcFile);
    AttributeList list = new AttributeList();
    list.setDecompressPixelData(false);
    list.read(i);
    String transferSyntaxUID = Attribute.getSingleStringValueOrEmptyString(list, TagFromName.TransferSyntaxUID);
    if (!transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)) {
      throw new DicomException("ImageEditUtilties.blackoutJPEGBlocks() can only be applied to DICOM files in JPEG Baseline TransferSyntax");
    }
    i.close();

    Attribute aPixelData = list.getPixelData();
    if (aPixelData != null) {
      if (shapes != null && shapes.size() > 0) {
        if (aPixelData instanceof OtherByteAttributeMultipleCompressedFrames) {
          // Pixel Data was not decompressed so can redact it without loss
          // outside the redacted regions
          slf4jlogger.debug("blackoutJPEGBlocks(): lossless redaction of JPEG pixels");
          byte[][] frames = ((OtherByteAttributeMultipleCompressedFrames) aPixelData).getFrames();
          for (int f = 0; f < frames.length; ++f) {
            ByteArrayInputStream fbis = new ByteArrayInputStream(frames[f]);
            ByteArrayOutputStream fbos = new ByteArrayOutputStream();
            // com.pixelmed.codec.jpeg.Parse.parse(fbis,fbos,shapes);
            frames[f] = fbos.toByteArray(); // hmmm :(
          }
        } else {
          throw new DicomException("Unable to obtain compressed JPEG bit stream");
        }
      } else {
        throw new DicomException("No redaction shapes specified");
      }
    } else {
      throw new DicomException("Not an image");
    }

    list.removeGroupLengthAttributes();
    String aeTitle = Attribute.getSingleStringValueOrEmptyString(list, TagFromName.SourceApplicationEntityTitle);
    list.removeMetaInformationHeaderAttributes();
    FileMetaInformation.addFileMetaInformation(list, transferSyntaxUID, aeTitle);
    list.write(dstFile, transferSyntaxUID, true/* useMeta */, true/*
                                                                   * useBufferedStream
                                                                   */);
  }

  /**
   * <p>
   * Blackout specified regions in an image, for example to remove burned in
   * identification.
   * </p>
   *
   * <p>
   * Overlays are not burned in.
   * </p>
   *
   * <p>
   * The replacement pixel value is the smallest possible pixel value based on
   * signedness and bit depth.
   * </p>
   *
   * <p>
   * The accompanying attribute list will be updated with new Pixel Data and
   * related Image Pixel Module attributes.
   * </p>
   *
   * <p>
   * Note that original PhotometricInterpretation will be retained; care should
   * be taken by the caller to change this as appropriate, e.g., from
   * YBR_FULL_422 if read as JPEG to RGB if written as uncompressed. See, for
   * example,
   * {@link com.pixelmed.dicom.AttributeList#correctDecompressedImagePixelModule()
   * AttributeList.correctDecompressedImagePixelModule()}.
   * </p>
   *
   * @param srcImg
   *        the image
   * @param list
   *        the attribute list corresponding image
   * @param shapes
   *        a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape
   *        java.awt.Shape}, specifed in image-relative coordinates
   * @throws DicomException
   *         if something bad happens handling the attribute list
   */
  static public void blackout(SourceImage srcImg, AttributeList list, Vector shapes) throws DicomException {
    blackout(srcImg, list, shapes, false, false, false, 0);
  }

  /**
   * <p>
   * Blackout specified regions in an image, for example to remove burned in
   * identification.
   * </p>
   *
   * <p>
   * Overlays may be burned in (and their corresponding attribues removed from
   * the AttributeList).
   * </p>
   *
   * <p>
   * The replacement pixel value is the smallest possible pixel value based on
   * signedness and bit depth.
   * </p>
   *
   * <p>
   * The accompanying attribute list will be updated with new Pixel Data and
   * related Image Pixel Module attributes.
   * </p>
   *
   * <p>
   * Note that original PhotometricInterpretation will be retained; care should
   * be taken by the caller to change this as appropriate, e.g., from
   * YBR_FULL_422 if read as JPEG to RGB if written as uncompressed. See, for
   * example,
   * {@link com.pixelmed.dicom.AttributeList#correctDecompressedImagePixelModule()
   * AttributeList.correctDecompressedImagePixelModule()}.
   * </p>
   *
   * @param srcImg
   *        the image
   * @param list
   *        the attribute list corresponding image
   * @param shapes
   *        a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape
   *        java.awt.Shape}, specifed in image-relative coordinates
   * @param burnInOverlays
   *        whether or not to burn in overlays
   * @throws DicomException
   *         if something bad happens handling the attribute list
   */
  static public void blackout(SourceImage srcImg, AttributeList list, Vector shapes, boolean burnInOverlays) throws DicomException {
    blackout(srcImg, list, shapes, burnInOverlays, false, false, 0);
  }

  /**
   * <p>
   * Blackout specified regions in an image, for example to remove burned in
   * identification.
   * </p>
   *
   * <p>
   * Overlays may be burned in (and their corresponding attribues removed from
   * the AttributeList).
   * </p>
   *
   * <p>
   * The replacement pixel value may be constrained to a specific value
   * (typically zero), rather than the using the pixel padding value, if
   * present, or the default, which is the smallest possible pixel value based
   * on signedness and bit depth.
   * </p>
   *
   * <p>
   * The accompanying attribute list will be updated with new Pixel Data and
   * related Image Pixel Module attributes.
   * </p>
   *
   * <p>
   * Note that original PhotometricInterpretation will be retained; care should
   * be taken by the caller to change this as appropriate, e.g., from
   * YBR_FULL_422 if read as JPEG to RGB if written as uncompressed. See, for
   * example,
   * {@link com.pixelmed.dicom.AttributeList#correctDecompressedImagePixelModule()
   * AttributeList.correctDecompressedImagePixelModule()}.
   * </p>
   *
   * @param srcImg
   *        the image
   * @param list
   *        the attribute list corresponding image
   * @param shapes
   *        a {@link java.util.Vector java.util.Vector} of {@link java.awt.Shape
   *        java.awt.Shape}, specifed in image-relative coordinates
   * @param burnInOverlays
   *        whether or not to burn in overlays
   * @param usePixelPaddingValue
   *        whether or not to use any pixel paddding value
   * @param useSpecifiedBlackoutValue
   *        whether or not to use the specifiedBlackoutValue or the default
   *        based on signedness and bit depth (overrides usePixelPaddingValue)
   * @param specifiedBlackoutValue
   *        the value used to replace blacked out pixel values, only used if
   *        useSpecifiedBlackoutValue is true
   * @throws DicomException
   *         if something bad happens handling the attribute list
   */
  static public void blackout(SourceImage srcImg, AttributeList list, Vector shapes, boolean burnInOverlays, boolean usePixelPaddingValue, boolean useSpecifiedBlackoutValue, int specifiedBlackoutValue) throws DicomException {
    slf4jlogger.debug("blackout(): burnInOverlays = {}", burnInOverlays);
    slf4jlogger.debug("blackout(): Integer.MAX_VALUE {}", Integer.MAX_VALUE);

    long startBlackoutTime = System.currentTimeMillis();
    long elapsedDrawingTime = 0;
    long elapsedCopyingTime = 0;
    long elapsedReconstructionTime = 0;

    int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.BitsAllocated, 0);
    ;
    int bitsStored = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.BitsStored, 0);
    ;
    int highBit = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.HighBit, bitsStored - 1);
    int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.SamplesPerPixel, 1);
    int pixelRepresentation = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.PixelRepresentation, 0);
    String photometricInterpretation = Attribute.getSingleStringValueOrNull(list, TagFromName.PhotometricInterpretation);
    int planarConfiguration = 0; // 0 is color-by-pixel, 1 is color-by-plane

    int rows = 0;
    int columns = 0;

    byte byteDstPixelsForFrames[][] = null;
    // short shortDstPixels[] = null;
    short shortDstPixelsForFrames[][] = null;
    Attribute pixelData = null;

    File[] temporaryPixelDataMultipleFiles = null;
    File temporaryPixelDataSingleFile = null;
    FileOutputStream temporaryPixelDataSingleFileOutputStream = null;
    BinaryOutputStream temporaryPixelDataBinaryOutputStream = null;

    boolean useOtherByteAttribute = false;
    boolean useOtherWordAttribute = false;

    Overlay overlay = srcImg.getOverlay();
    boolean inverted = srcImg.isInverted();
    boolean signed = srcImg.isSigned();
    int mask = srcImg.getMaskValue();
    boolean ybr = srcImg.isYBR();

    int dstOffsetToStartOfCurrentFrame = 0; // always 0 since now have separate
                                            // array for each frame
    int numberOfFrames = srcImg.getNumberOfBufferedImages();
    // boolean needToCopyEachFrame = true;
    for (int frame = 0; frame < numberOfFrames; ++frame) {
      BufferedImage src = srcImg.getBufferedImage(frame);
      slf4jlogger.debug("blackout(): Frame [{}]", frame);
      // BufferedImageUtilities.describeImage(src,System.err);
      columns = src.getWidth();
      slf4jlogger.debug("blackout(): columns = {}", columns);
      rows = src.getHeight();
      slf4jlogger.debug("blackout(): rows = {}", rows);
      SampleModel srcSampleModel = src.getSampleModel();
      slf4jlogger.debug("blackout(): srcSampleModel = {}", srcSampleModel);
      int srcDataType = srcSampleModel.getDataType();
      slf4jlogger.debug("blackout(): srcDataType = {}", srcDataType);
      Raster srcRaster = src.getRaster();
      DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
      int srcDataBufferType = srcDataBuffer.getDataType();
      slf4jlogger.debug("blackout(): srcDataBufferType = {}", srcDataBufferType);
      int srcNumBands = srcRaster.getNumBands();
      slf4jlogger.debug("blackout(): srcNumBands = {}", srcNumBands);
      int srcPixelStride = srcNumBands;
      int srcScanlineStride = columns * srcNumBands;
      if (srcNumBands > 1 && srcSampleModel instanceof ComponentSampleModel) {
        ComponentSampleModel srcComponentSampleModel = (ComponentSampleModel) srcSampleModel;
        srcPixelStride = srcComponentSampleModel.getPixelStride(); // should be
                                                                   // either
                                                                   // srcNumBands
                                                                   // if
                                                                   // color-by-pixel,
                                                                   // or 1 if
                                                                   // color-by-plane
        srcScanlineStride = srcComponentSampleModel.getScanlineStride(); // should
                                                                         // be
                                                                         // either
                                                                         // columns*srcNumBands
                                                                         // if
                                                                         // color-by-pixel,
                                                                         // or
                                                                         // columns
                                                                         // if
                                                                         // color-by-plane
        planarConfiguration = srcPixelStride == srcNumBands ? 0 : 1;
      }
      slf4jlogger.debug("blackout(): srcPixelStride = {}", srcPixelStride);
      slf4jlogger.debug("blackout(): srcScanlineStride = {}", srcScanlineStride);
      slf4jlogger.debug("blackout(): planarConfiguration = {}", planarConfiguration);
      int srcDataBufferOffset = srcDataBuffer.getOffset();
      slf4jlogger.debug("blackout(): Frame [{}] srcDataBufferOffset = {}", frame, srcDataBufferOffset);
      long srcFrameLength = ((long) rows) * columns * srcNumBands; // long since
                                                                   // when used
                                                                   // to
                                                                   // calculate
                                                                   // total
                                                                   // pixel data
                                                                   // size may
                                                                   // be > 2GB
                                                                   // (000728)
      slf4jlogger.debug("blackout(): Frame [{}] srcFrameLength = {}", frame, srcFrameLength);
      int srcDataBufferNumBanks = srcDataBuffer.getNumBanks();
      slf4jlogger.debug("blackout(): Frame [{}] srcDataBufferNumBanks = {}", frame, srcDataBufferNumBanks);

      if (srcDataBufferNumBanks > 1) {
        throw new DicomException("Unsupported type of image - DataBuffer number of banks is > 1, is " + srcDataBufferNumBanks);
      }

      int dstPixelStride = planarConfiguration == 0 ? srcNumBands : 1;
      int dstBandStride = planarConfiguration == 0 ? 1 : rows * columns;
      slf4jlogger.debug("blackout(): dstPixelStride = {}", dstPixelStride);
      slf4jlogger.debug("blackout(): dstBandStride = {}", dstBandStride);

      if (srcDataBufferType == DataBuffer.TYPE_BYTE) {
        slf4jlogger.debug("blackout(): srcDataBufferType = DataBuffer.TYPE_BYTE");
        useOtherByteAttribute = true;

        int backgroundValueBasis = (useSpecifiedBlackoutValue
            ? specifiedBlackoutValue
            : (usePixelPaddingValue && srcImg.isPadded()
                ? srcImg.getPadValue()
                : (inverted
                    ? (signed ? (mask >> 1) : mask) // largest value (will
                                                    // always be +ve)
                    : (signed ? (((mask >> 1) + 1)) : 0) // smallest value (will
                                                         // be -ve if signed,
                                                         // but do NOT need to
                                                         // extend into integer,
                                                         // since only used to
                                                         // set value in
                                                         // PixelData array)
                ))) & 0xff;
        slf4jlogger.debug("blackout(): backgroundValueBasis = {}", backgroundValueBasis);

        int foregroundValueBasis = (inverted
            ? (signed ? (((mask >> 1) + 1)) : 0) // smallest value (will be -ve
                                                 // if signed, but do NOT need
                                                 // to extend into integer,
                                                 // since only used to set value
                                                 // in PixelData array)
            : (signed ? (mask >> 1) : mask) // largest value (will always be
                                            // +ve)
        ) & 0xff;
        slf4jlogger.debug("blackout(): foregroundValueBasis = {}", foregroundValueBasis);

        byte[] backgroundValue = new byte[srcNumBands];
        byte[] foregroundValue = new byte[srcNumBands];
        if (ybr && srcNumBands >= 3) {
          backgroundValue[0] = (byte) (.2990 * backgroundValueBasis + .5870 * backgroundValueBasis + .1140 * backgroundValueBasis);
          backgroundValue[1] = (byte) (-.1687 * backgroundValueBasis - .3313 * backgroundValueBasis + .5000 * backgroundValueBasis + 128);
          backgroundValue[2] = (byte) (.5000 * backgroundValueBasis - .4187 * backgroundValueBasis - .0813 * backgroundValueBasis + 128);

          foregroundValue[0] = (byte) (.2990 * foregroundValueBasis + .5870 * foregroundValueBasis + .1140 * foregroundValueBasis);
          foregroundValue[1] = (byte) (-.1687 * foregroundValueBasis - .3313 * foregroundValueBasis + .5000 * foregroundValueBasis + 128);
          foregroundValue[2] = (byte) (.5000 * foregroundValueBasis - .4187 * foregroundValueBasis - .0813 * foregroundValueBasis + 128);

          for (int bandIndex = 3; bandIndex < srcNumBands; ++bandIndex) {
            backgroundValue[bandIndex] = -1;
            foregroundValue[bandIndex] = -1;
          }
        } else {
          for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
            backgroundValue[bandIndex] = (byte) backgroundValueBasis;
            foregroundValue[bandIndex] = (byte) foregroundValueBasis;
          }
        }
        // for (int bandIndex=0; bandIndex<srcNumBands; ++bandIndex) {
        // System.err.println("ImageEditUtilities.blackout():
        // backgroundValue["+bandIndex+"] = "+backgroundValue[bandIndex]+"
        // foregroundValue["+bandIndex+"] = "+foregroundValue[bandIndex]); }

        byte[][] srcPixelBanks = null;
        if (srcDataBuffer instanceof DataBufferByte) {
          slf4jlogger.debug("blackout(): Frame [{}] DataBufferByte", frame);
          srcPixelBanks = ((DataBufferByte) srcDataBuffer).getBankData();
        } else {
          throw new DicomException("Unsupported type of image - DataBuffer is TYPE_BYTE but not instance of DataBufferByte, is " + srcDataBuffer.getClass().getName());
        }
        int srcPixelBankLength = srcPixelBanks[0].length;
        slf4jlogger.debug("blackout(): Frame [{}] srcPixelBankLength = {}", frame, srcPixelBankLength);

        if (byteDstPixelsForFrames == null && temporaryPixelDataSingleFile == null && temporaryPixelDataMultipleFiles == null) {
          if (bitsAllocated > 8) {
            bitsAllocated = 8;
          }
          if (bitsStored > 8) {
            bitsStored = 8;
          }
          if (highBit > 7) {
            highBit = 7;
          }
          samplesPerPixel = srcNumBands;
          // leave photometricInterpretation alone
          // leave planarConfiguration alone ... already determined from
          // srcPixelStride if srcNumBands > 1

          long totalSizeInBytes = numberOfFrames * srcFrameLength;
          slf4jlogger.info("blackout(): totalSizeInBytes = {}", totalSizeInBytes);
          if (totalSizeInBytes > usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan && usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan != -1l) {
            if (numberOfFrames > 1 && totalSizeInBytes > useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan && useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan != -1l) {
              slf4jlogger.info("blackout(): using multiple temporary files, one for each frame, for blacked out pixel data rather than allocating array of frames");
              temporaryPixelDataMultipleFiles = new File[numberOfFrames];
              // actual files will be allocated as needed
            } else {
              slf4jlogger.info("blackout(): using a single temporary file for all frames of blacked out pixel data rather than allocating array of frames");
              try {
                temporaryPixelDataSingleFile = File.createTempFile("ImageEditUtilities_blackout", null);
                slf4jlogger.debug("blackout(): temporary file is \"{}\"", temporaryPixelDataSingleFile);
                temporaryPixelDataSingleFile.deleteOnExit(); // we will do
                                                             // better than this
                                                             // by explicitly
                                                             // deleting it when
                                                             // Attribute goes
                                                             // out of scope,
                                                             // but just in case
                temporaryPixelDataSingleFileOutputStream = new FileOutputStream(temporaryPixelDataSingleFile);
                // do use BufferedOutputStream, since we are doing a huge direct
                // write of a whole frame
              } catch (FileNotFoundException e) {
                slf4jlogger.error("", e);
                throw new DicomException("Could not find temporary file used during blackout");
              } catch (IOException e) {
                slf4jlogger.error("", e);
                throw new DicomException("Problem creating temporary file used during blackout");
              }
            }
          }
          if (temporaryPixelDataSingleFile == null && temporaryPixelDataMultipleFiles == null) {
            slf4jlogger.debug("blackout(): allocating array of frames rather than single contiguous array for all frames of blacked out pixel data");
            byteDstPixelsForFrames = new byte[numberOfFrames][];
            for (int f = 0; f < numberOfFrames; ++f) {
              slf4jlogger.debug("blackout(): allocating frame {} length {} bytes", f, srcFrameLength);
              if (srcFrameLength > Integer.MAX_VALUE) {
                throw new DicomException("Could not allocate frame size that exceeds Integer.MAX_VALUE bytes");
              } else {
                byteDstPixelsForFrames[f] = new byte[(int) srcFrameLength];
              }
            }
            pixelData = new OtherByteAttributeMultipleFrameArrays(TagFromName.PixelData);
            ((OtherByteAttributeMultipleFrameArrays) pixelData).setValuesPerFrame(byteDstPixelsForFrames);
          }
        }
        // long startCopyingTime = System.currentTimeMillis();
        byte[] byteDstPixels = null;
        if (byteDstPixelsForFrames != null) {
          byteDstPixels = byteDstPixelsForFrames[frame];
        }
        if (byteDstPixels == null) {
          if (srcFrameLength > Integer.MAX_VALUE) {
            throw new DicomException("Could not allocate frame size that exceeds Integer.MAX_VALUE bytes");
          } else {
            byteDstPixels = new byte[(int) srcFrameLength];
          }
        }
        System.arraycopy(srcPixelBanks[0], srcDataBufferOffset, byteDstPixels, dstOffsetToStartOfCurrentFrame, (int) srcFrameLength);
        // elapsedCopyingTime+=System.currentTimeMillis()-startCopyingTime;
        // long startDrawingTime = System.currentTimeMillis();
        if (burnInOverlays && overlay != null && overlay.getNumberOfOverlays(frame) > 0) {
          slf4jlogger.debug("blackout(): Drawing overlays for frame {}", frame);
          for (int o = 0; o < 16; ++o) {
            BufferedImage overlayImage = overlay.getOverlayAsBinaryBufferedImage(frame, o);
            if (overlayImage != null) {
              slf4jlogger.debug("blackout(): Drawing overlay number {}", o);
              int rowOrigin = overlay.getRowOrigin(frame, o);
              int columnOrigin = overlay.getColumnOrigin(frame, o);
              // first "draw" "shadow" offset one pixel down and right
              for (int overlayRow = 0; overlayRow < overlayImage.getHeight(); ++overlayRow) {
                for (int overlayColumn = 0; overlayColumn < overlayImage.getWidth(); ++overlayColumn) {
                  int value = overlayImage.getRGB(overlayColumn, overlayRow);
                  if (value != 0) {
                    int x = columnOrigin + overlayColumn + 1;
                    int y = rowOrigin + overlayRow + 1;
                    if (x < columns && y < rows) {
                      int pixelIndexWithinFrame = y * columns + x;
                      slf4jlogger.trace("blackout(): Drawing overlay -  setting shadow overlay ({},{}) at image ({},{})", overlayColumn, overlayRow, x, y);
                      for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                        int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                        slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                        byteDstPixels[sampleIndex] = backgroundValue[bandIndex];
                      }
                    }
                  }
                }
              }
              // now "draw" "image"
              for (int overlayRow = 0; overlayRow < overlayImage.getHeight(); ++overlayRow) {
                for (int overlayColumn = 0; overlayColumn < overlayImage.getWidth(); ++overlayColumn) {
                  int value = overlayImage.getRGB(overlayColumn, overlayRow);
                  if (value != 0) {
                    int x = columnOrigin + overlayColumn;
                    int y = rowOrigin + overlayRow;
                    if (x < columns && y < rows) {
                      int pixelIndexWithinFrame = y * columns + x;
                      slf4jlogger.trace("blackout(): Drawing overlay -  setting foreground overlay ({},{}) at image ({},{})", overlayColumn, overlayRow, x, y);
                      for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                        int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                        slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                        byteDstPixels[sampleIndex] = foregroundValue[bandIndex];
                      }
                    }
                  }
                }
              }
            }
          }
        }
        // do shapes AFTER overlays, because want to blackout overlays "under"
        // chapes
        if (shapes != null) {
          Iterator it = shapes.iterator();
          while (it.hasNext()) {
            Shape shape = (Shape) it.next();
            if (shape instanceof RectangularShape) { // this includes Rectangle
                                                     // and Rectangle2D (but
                                                     // also some other things
                                                     // that would need special
                                                     // handling :( )
              RectangularShape rect = (RectangularShape) shape;
              slf4jlogger.debug("blackout(): shape is RectangularShape {}", rect);
              int startX = (int) rect.getX();
              int startY = (int) rect.getY();
              int stopX = (int) (startX + rect.getWidth());
              int stopY = (int) (startY + rect.getHeight());
              for (int y = startY; y < stopY; ++y) {
                int pixelIndexWithinFrame = y * columns + startX;
                slf4jlogger.trace("blackout(): row {} startX {} pixelIndexWithinFrame {}", y, startX, pixelIndexWithinFrame);
                for (int x = startX; x < stopX; ++x) {
                  if (slf4jlogger.isTraceEnabled())
                    slf4jlogger.trace("blackout(): before set - getRGB({},{})=0x{}", x, y, Integer.toHexString(srcImg.getBufferedImage(frame).getRGB(x, y)));
                  for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                    int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                    slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                    byteDstPixels[sampleIndex] = backgroundValue[bandIndex];
                  }
                  ++pixelIndexWithinFrame;
                  if (slf4jlogger.isTraceEnabled())
                    slf4jlogger.trace("blackout(): after set - getRGB({},{})=0x{}", x, y, Integer.toHexString(srcImg.getBufferedImage(frame).getRGB(x, y)));
                }
              }
            }
          }
        }
        // elapsedDrawingTime+=System.currentTimeMillis()-startDrawingTime;
        // dstOffsetToStartOfCurrentFrame+=srcFrameLength; // do NOT increment
        // anymore ... always 0 since now have separate array for each frame
        if (temporaryPixelDataSingleFile != null) {
          try {
            slf4jlogger.debug("blackout(): writing to temporary frame {} length {} bytes", frame, byteDstPixels.length);
            temporaryPixelDataSingleFileOutputStream.write(byteDstPixels);
          } catch (IOException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Could not write to temporary file used during blackout");
          }
        } else if (temporaryPixelDataMultipleFiles != null) {
          slf4jlogger.debug("blackout(): using a temporary file for blacked out pixel data rather than allocating array of frames of blacked out pixel data");
          try {
            File temporaryPixelDataThisFrameFile = File.createTempFile("ImageEditUtilities_blackout", null);
            slf4jlogger.debug("blackout(): temporary file for frame {} is \"{}\"", frame, temporaryPixelDataThisFrameFile);
            temporaryPixelDataThisFrameFile.deleteOnExit(); // we will do better
                                                            // than this by
                                                            // explicitly
                                                            // deleting it when
                                                            // Attribute goes
                                                            // out of scope, but
                                                            // just in case
            FileOutputStream temporaryPixelDataThisFrameFileOutputStream = new FileOutputStream(temporaryPixelDataThisFrameFile);
            temporaryPixelDataThisFrameFileOutputStream.write(byteDstPixels);
            temporaryPixelDataThisFrameFileOutputStream.flush();
            temporaryPixelDataThisFrameFileOutputStream.close();
            temporaryPixelDataMultipleFiles[frame] = temporaryPixelDataThisFrameFile;
          } catch (FileNotFoundException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Could not find temporary file used during blackout");
          } catch (IOException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Problem creating or writing to or closing temporary file used during blackout");
          }
        }
      } else if (srcDataBufferType == DataBuffer.TYPE_USHORT || srcDataBufferType == DataBuffer.TYPE_SHORT) {
        slf4jlogger.debug("blackout(): srcDataBufferType = DataBuffer.TYPE_USHORT or DataBuffer.TYPE_SHORT");
        useOtherWordAttribute = true;

        short backgroundValue = (short) (useSpecifiedBlackoutValue
            ? specifiedBlackoutValue
            : (usePixelPaddingValue && srcImg.isPadded()
                ? srcImg.getPadValue()
                : (inverted
                    ? (signed ? (mask >> 1) : mask) // largest value (will
                                                    // always be +ve)
                    : (signed ? (((mask >> 1) + 1)) : 0) // smallest value (will
                                                         // be -ve if signed,
                                                         // but do NOT need to
                                                         // extend into integer,
                                                         // since only used tp
                                                         // set value in
                                                         // PixelData array)
                )));
        slf4jlogger.debug("blackout(): backgroundValue = {}", backgroundValue);

        short foregroundValue = (short) (inverted
            ? (signed ? (((mask >> 1) + 1)) : 0) // smallest value (will be -ve
                                                 // if signed, but do NOT need
                                                 // to extend into integer,
                                                 // since only used tp set value
                                                 // in PixelData array)
            : (signed ? (mask >> 1) : mask) // largest value (will always be
                                            // +ve)
        );
        slf4jlogger.debug("blackout(): foregroundValue = {}", foregroundValue);

        short[][] srcPixelBanks = null;
        if (srcDataBuffer instanceof DataBufferShort) {
          slf4jlogger.debug("blackout(): Frame [{}] DataBufferShort", frame);
          srcPixelBanks = ((DataBufferShort) srcDataBuffer).getBankData();
        } else if (srcDataBuffer instanceof DataBufferUShort) {
          slf4jlogger.debug("blackout(): Frame [{}] DataBufferUShort", frame);
          srcPixelBanks = ((DataBufferUShort) srcDataBuffer).getBankData();
        } else {
          throw new DicomException("Unsupported type of image - DataBuffer is TYPE_USHORT or TYPE_SHORT but not instance of DataBufferShort, is " + srcDataBuffer.getClass().getName());
        }
        int srcPixelBankLength = srcPixelBanks[0].length;
        slf4jlogger.debug("blackout(): Frame [{}] srcPixelBankLength = {}", frame, srcPixelBankLength);
        if (shortDstPixelsForFrames == null && temporaryPixelDataSingleFile == null && temporaryPixelDataMultipleFiles == null) {
          if (bitsAllocated > 16) {
            bitsAllocated = 16;
          }
          if (bitsStored > 16) {
            bitsStored = 16;
          }
          if (highBit > 15) {
            highBit = 15;
          }
          samplesPerPixel = srcNumBands;
          // leave photometricInterpretation alone
          // leave planarConfiguration alone ... already determined from
          // srcPixelStride if srcNumBands > 1

          long dstPixelsLength = srcFrameLength * numberOfFrames;
          slf4jlogger.debug("blackout(): Frame [{}] dstPixelsLength = {}", frame, dstPixelsLength);
          long totalSizeInBytes = dstPixelsLength * 2;
          slf4jlogger.info("blackout(): totalSizeInBytes = {}", totalSizeInBytes);
          if (totalSizeInBytes > usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan && usePixelDataTemporaryFilesIfPixelDataLengthGreaterThan != -1l) {
            if (numberOfFrames > 1 && totalSizeInBytes > useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan && useMultiplePixelDataTemporaryFilesIfPixelDataLengthGreaterThan != -1l) {
              slf4jlogger.info("blackout(): using multiple temporary files, one for each frame, for blacked out pixel data rather than allocating array of frames");
              temporaryPixelDataMultipleFiles = new File[numberOfFrames];
              // actual files will be allocated as needed
            } else {
              slf4jlogger.info("blackout(): using a single temporary file for all frames of blacked out pixel data rather than allocating array of frames");
              try {
                temporaryPixelDataSingleFile = File.createTempFile("ImageEditUtilities_blackout", null);
                slf4jlogger.debug("blackout(): temporary file is \"{}\"", temporaryPixelDataSingleFile);
                temporaryPixelDataSingleFile.deleteOnExit(); // we will do
                                                             // better than this
                                                             // by explicitly
                                                             // deleting it when
                                                             // Attribute goes
                                                             // out of scope,
                                                             // but just in case
                temporaryPixelDataSingleFileOutputStream = new FileOutputStream(temporaryPixelDataSingleFile);
                // do use BufferedOutputStream, since we are doing a huge direct
                // write of a whole frame
                temporaryPixelDataBinaryOutputStream = new BinaryOutputStream(temporaryPixelDataSingleFileOutputStream, false/*
                                                                                                                              * bigEndian
                                                                                                                              */);
              } catch (FileNotFoundException e) {
                slf4jlogger.error("", e);
                throw new DicomException("Could not find temporary file used during blackout");
              } catch (IOException e) {
                slf4jlogger.error("", e);
                throw new DicomException("Problem creating temporary file used during blackout");
              }
            }
          }
          if (temporaryPixelDataSingleFile == null && temporaryPixelDataMultipleFiles == null) {
            slf4jlogger.debug("blackout(): allocating array of frames rather than single contiguous array for all frames of blacked out pixel data");
            shortDstPixelsForFrames = new short[numberOfFrames][];
            for (int f = 0; f < numberOfFrames; ++f) {
              slf4jlogger.debug("blackout(): allocating frame {} length {} short", f, srcFrameLength);
              if (srcFrameLength > Integer.MAX_VALUE) {
                throw new DicomException("Could not allocate frame size that exceeds Integer.MAX_VALUE shorts");
              } else {
                shortDstPixelsForFrames[f] = new short[(int) srcFrameLength];
              }
            }
            pixelData = new OtherWordAttributeMultipleFrameArrays(TagFromName.PixelData);
            ((OtherWordAttributeMultipleFrameArrays) pixelData).setValuesPerFrame(shortDstPixelsForFrames);

            // 2016/08/26 this is the old code before we replicated the byte
            // approach of using temporary files when necessary ...
            // if (dstPixelsLength == srcPixelBankLength) {
            // slf4jlogger.debug("blackout(): Frame [{}] optimizing by using
            // entire multi-frame array rather than copying",frame);
            // // optimize for special case of entire multi-frame image data in
            // single array, shared by multiple BufferedImages using
            // srcDataBufferOffset for frames
            // // assumes that offsets are in same order as frames
            // shortDstPixels = srcPixelBanks[0];
            // needToCopyEachFrame = false; // rather than break, since still
            // need to draw regions
            // }
            // else {
            // shortDstPixels = new short[dstPixelsLength];
            // }
            // pixelData = new OtherWordAttribute(TagFromName.PixelData);
            // pixelData.setValues(shortDstPixels);
          }
        }
        // long startCopyingTime = System.currentTimeMillis();
        // if (needToCopyEachFrame) {
        // System.arraycopy(srcPixelBanks[0],srcDataBufferOffset,shortDstPixels,dstOffsetToStartOfCurrentFrame,srcFrameLength);
        // }
        short[] shortDstPixels = null;
        if (shortDstPixelsForFrames != null) {
          shortDstPixels = shortDstPixelsForFrames[frame];
        }
        if (shortDstPixels == null) {
          if (srcFrameLength > Integer.MAX_VALUE) {
            throw new DicomException("Could not allocate frame size that exceeds Integer.MAX_VALUE shorts");
          } else {
            shortDstPixels = new short[(int) srcFrameLength];
          }
        }
        System.arraycopy(srcPixelBanks[0], srcDataBufferOffset, shortDstPixels, dstOffsetToStartOfCurrentFrame, (int) srcFrameLength);
        // elapsedCopyingTime+=System.currentTimeMillis()-startCopyingTime;
        // long startDrawingTime = System.currentTimeMillis();
        if (burnInOverlays && overlay != null && overlay.getNumberOfOverlays(frame) > 0) {
          slf4jlogger.debug("blackout(): Drawing overlays for frame {}", frame);
          for (int o = 0; o < 16; ++o) {
            BufferedImage overlayImage = overlay.getOverlayAsBinaryBufferedImage(frame, o);
            if (overlayImage != null) {
              slf4jlogger.debug("blackout(): Drawing overlay number {}", o);
              int rowOrigin = overlay.getRowOrigin(frame, o);
              int columnOrigin = overlay.getColumnOrigin(frame, o);
              // first "draw" "shadow" offset one pixel down and right
              for (int overlayRow = 0; overlayRow < overlayImage.getHeight(); ++overlayRow) {
                for (int overlayColumn = 0; overlayColumn < overlayImage.getWidth(); ++overlayColumn) {
                  int value = overlayImage.getRGB(overlayColumn, overlayRow);
                  if (value != 0) {
                    int x = columnOrigin + overlayColumn + 1;
                    int y = rowOrigin + overlayRow + 1;
                    if (x < columns && y < rows) {
                      int pixelIndexWithinFrame = y * columns + x;
                      slf4jlogger.trace("blackout(): Drawing overlay -  setting shadow overlay ({},{}) at image ({},{})", overlayColumn, overlayRow, x, y);
                      for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                        int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                        slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                        shortDstPixels[sampleIndex] = backgroundValue;
                      }
                    }
                  }
                }
              }
              // now "draw" "image"
              for (int overlayRow = 0; overlayRow < overlayImage.getHeight(); ++overlayRow) {
                for (int overlayColumn = 0; overlayColumn < overlayImage.getWidth(); ++overlayColumn) {
                  int value = overlayImage.getRGB(overlayColumn, overlayRow);
                  if (value != 0) {
                    int x = columnOrigin + overlayColumn;
                    int y = rowOrigin + overlayRow;
                    if (x < columns && y < rows) {
                      int pixelIndexWithinFrame = y * columns + x;
                      slf4jlogger.trace("blackout(): Drawing overlay -  setting foreground overlay ({},{}) at image ({},{})", overlayColumn, overlayRow, x, y);
                      for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                        int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                        slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                        shortDstPixels[sampleIndex] = foregroundValue;
                      }
                    }
                  }
                }
              }
            }
          }
        }
        // do shapes AFTER overlays, because want to blackout overlays "under"
        // chapes
        if (shapes != null) {
          Iterator it = shapes.iterator();
          while (it.hasNext()) {
            Shape shape = (Shape) it.next();
            if (shape instanceof RectangularShape) { // this includes Rectangle
                                                     // and Rectangle2D (but
                                                     // also some other things
                                                     // that would need special
                                                     // handling :( )
              RectangularShape rect = (RectangularShape) shape;
              slf4jlogger.debug("blackout(): shape is RectangularShape {}", rect);
              int startX = (int) rect.getX();
              int startY = (int) rect.getY();
              int stopX = (int) (startX + rect.getWidth());
              int stopY = (int) (startY + rect.getHeight());
              for (int y = startY; y < stopY; ++y) {
                int pixelIndexWithinFrame = y * columns + startX;
                for (int x = startX; x < stopX; ++x) {
                  for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                    int sampleIndex = dstOffsetToStartOfCurrentFrame + pixelIndexWithinFrame * dstPixelStride + bandIndex * dstBandStride;
                    slf4jlogger.trace("blackout(): frame={} y={} x={} pixelIndexWithinFrame={} bandIndex={} sampleIndex={}", frame, y, x, pixelIndexWithinFrame, bandIndex, sampleIndex);
                    shortDstPixels[sampleIndex] = backgroundValue;
                  }
                  ++pixelIndexWithinFrame;
                }
              }
            }
          }
        }
        // elapsedDrawingTime+=System.currentTimeMillis()-startDrawingTime;
        // dstOffsetToStartOfCurrentFrame+=srcFrameLength; // do NOT increment
        // anymore ... always 0 since now have separate array for each frame
        if (temporaryPixelDataSingleFile != null) {
          try {
            slf4jlogger.debug("blackout(): writing to temporary frame {} length {} words", frame, shortDstPixels.length);
            temporaryPixelDataBinaryOutputStream.writeUnsigned16(shortDstPixels, shortDstPixels.length);
          } catch (IOException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Could not write to temporary file used during blackout");
          }
        } else if (temporaryPixelDataMultipleFiles != null) {
          slf4jlogger.debug("blackout(): using a temporary file for blacked out pixel data rather than allocating array of frames");
          try {
            File temporaryPixelDataThisFrameFile = File.createTempFile("ImageEditUtilities_blackout", null);
            slf4jlogger.debug("blackout(): temporary file for frame {} is \"{}\"", frame, temporaryPixelDataThisFrameFile);
            temporaryPixelDataThisFrameFile.deleteOnExit(); // we will do better
                                                            // than this by
                                                            // explicitly
                                                            // deleting it when
                                                            // Attribute goes
                                                            // out of scope, but
                                                            // just in case
            FileOutputStream temporaryPixelDataThisFrameFileOutputStream = new FileOutputStream(temporaryPixelDataThisFrameFile);
            // do use BufferedOutputStream, since we are doing a huge direct
            // write of a whole frame
            BinaryOutputStream temporaryPixelDataThisFrameBinaryOutputStream = new BinaryOutputStream(temporaryPixelDataThisFrameFileOutputStream, false/*
                                                                                                                                                         * bigEndian
                                                                                                                                                         */);
            temporaryPixelDataThisFrameBinaryOutputStream.writeUnsigned16(shortDstPixels, shortDstPixels.length);
            temporaryPixelDataThisFrameBinaryOutputStream.flush();
            temporaryPixelDataThisFrameBinaryOutputStream.close();
            temporaryPixelDataThisFrameFileOutputStream.close();
            temporaryPixelDataMultipleFiles[frame] = temporaryPixelDataThisFrameFile;
          } catch (FileNotFoundException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Could not find temporary file used during blackout");
          } catch (IOException e) {
            slf4jlogger.error("", e);
            throw new DicomException("Problem creating or writing to or closing temporary file used during blackout");
          }
        }
      } else {
        throw new DicomException("Unsupported pixel data form - DataBufferType = " + srcDataBufferType);
      }
    }

    if (temporaryPixelDataSingleFile != null) {
      if (useOtherByteAttribute) {
        try {
          temporaryPixelDataSingleFileOutputStream.flush();
          temporaryPixelDataSingleFileOutputStream.close();
          slf4jlogger.debug("blackout(): temporary file \"{}\" length {}", temporaryPixelDataSingleFile, temporaryPixelDataSingleFile.length());
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not close temporary file used during blackout before creating Pixel Data Attribute using it");
        }
        pixelData = new OtherByteAttributeOnDisk(TagFromName.PixelData);
        ((OtherByteAttributeOnDisk) pixelData).deleteFilesWhenNoLongerNeeded();
        try {
          ((OtherByteAttributeOnDisk) pixelData).setFile(temporaryPixelDataSingleFile, 0);
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not get length of temporary file used during blackout when creating Pixel Data Attribute using it");
        }
      } else if (useOtherWordAttribute) {
        try {
          temporaryPixelDataBinaryOutputStream.flush();
          temporaryPixelDataBinaryOutputStream.close();
          temporaryPixelDataSingleFileOutputStream.close();
          slf4jlogger.debug("blackout(): temporary file \"{}\" length {}", temporaryPixelDataSingleFile, temporaryPixelDataSingleFile.length());
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not close temporary file used during blackout before creating Pixel Data Attribute using it");
        }
        pixelData = new OtherWordAttributeOnDisk(TagFromName.PixelData, false/*
                                                                              * bigEndian
                                                                              */); // we
                                                                                   // wrote
                                                                                   // the
                                                                                   // files
                                                                                   // that
                                                                                   // way
        ((OtherWordAttributeOnDisk) pixelData).deleteFilesWhenNoLongerNeeded();
        try {
          ((OtherWordAttributeOnDisk) pixelData).setFile(temporaryPixelDataSingleFile, 0);
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not get length of temporary file used during blackout when creating Pixel Data Attribute using it");
        }
      }
      // cannot get here
    } else if (temporaryPixelDataMultipleFiles != null) {
      if (useOtherByteAttribute) {
        pixelData = new OtherByteAttributeMultipleFilesOnDisk(TagFromName.PixelData);
        ((OtherByteAttributeMultipleFilesOnDisk) pixelData).deleteFilesWhenNoLongerNeeded();
        try {
          long[] byteOffsets = new long[numberOfFrames];
          long[] lengths = new long[numberOfFrames];
          for (int f = 0; f < numberOfFrames; ++f) {
            byteOffsets[f] = 0;
            lengths[f] = temporaryPixelDataMultipleFiles[f].length();
          }
          ((OtherByteAttributeMultipleFilesOnDisk) pixelData).setFiles(temporaryPixelDataMultipleFiles, byteOffsets, lengths);
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not get lengths of temporary files used during blackout when creating Pixel Data Attribute using it");
        }
      } else if (useOtherWordAttribute) {
        pixelData = new OtherWordAttributeMultipleFilesOnDisk(TagFromName.PixelData, false/*
                                                                                           * bigEndian
                                                                                           */); // we
                                                                                                // wrote
                                                                                                // the
                                                                                                // files
                                                                                                // that
                                                                                                // way
        ((OtherWordAttributeMultipleFilesOnDisk) pixelData).deleteFilesWhenNoLongerNeeded();
        try {
          long[] byteOffsets = new long[numberOfFrames];
          long[] lengths = new long[numberOfFrames];
          for (int f = 0; f < numberOfFrames; ++f) {
            byteOffsets[f] = 0;
            lengths[f] = temporaryPixelDataMultipleFiles[f].length();
          }
          ((OtherWordAttributeMultipleFilesOnDisk) pixelData).setFiles(temporaryPixelDataMultipleFiles, byteOffsets, lengths);
        } catch (IOException e) {
          slf4jlogger.error("", e);
          throw new DicomException("Could not get lengths of temporary files used during blackout when creating Pixel Data Attribute using it");
        }
      }
      // cannot get here
    }

    // special case ... if input was uncompressed YBR_FULL_422 that was chroma
    // upsampled to YBR_FULL when getting BufferedImages from SourceImage, need
    // to change to YBR_FULL (000986)
    // use same check as in SourceImage so as not to confuse with decompressed
    // JPEG
    if (photometricInterpretation.equals("YBR_FULL_422") && !list.getDecompressedPhotometricInterpretation(photometricInterpretation).equals("RGB")) {
      slf4jlogger.debug("blackout(): replacing photometricInterpretation of YBR_FULL_422 with YBR_FULL");
      photometricInterpretation = "YBR_FULL";
    }

    list.remove(TagFromName.PixelData);
    list.remove(TagFromName.BitsAllocated);
    list.remove(TagFromName.BitsStored);
    list.remove(TagFromName.HighBit);
    list.remove(TagFromName.SamplesPerPixel);
    list.remove(TagFromName.PixelRepresentation);
    list.remove(TagFromName.PhotometricInterpretation);
    list.remove(TagFromName.PlanarConfiguration);
    boolean numberOfFramesWasPresentBefore = list.get(TagFromName.NumberOfFrames) != null;
    slf4jlogger.debug("blackout(): numberOfFramesWasPresentBefore = {}", numberOfFramesWasPresentBefore);
    list.remove(TagFromName.NumberOfFrames);

    if (burnInOverlays) {
      slf4jlogger.debug("blackout(): removeOverlayAttributes");
      list.removeOverlayAttributes();
    }

    list.put(pixelData);
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated);
      a.addValue(bitsAllocated);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored);
      a.addValue(bitsStored);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.HighBit);
      a.addValue(highBit);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.Rows);
      a.addValue(rows);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.Columns);
      a.addValue(columns);
      list.put(a);
    }
    if (numberOfFrames > 1 || numberOfFramesWasPresentBefore) {
      Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames);
      a.addValue(numberOfFrames);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel);
      a.addValue(samplesPerPixel);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation);
      a.addValue(pixelRepresentation);
      list.put(a);
    }
    {
      Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation);
      a.addValue(photometricInterpretation);
      list.put(a);
    }
    if (samplesPerPixel > 1) {
      Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration);
      a.addValue(planarConfiguration);
      list.put(a);
    }

    long startReconstructionTime = System.currentTimeMillis();
    slf4jlogger.debug("blackout(): Reconstruct source image");
    srcImg.constructSourceImage(list);
    elapsedReconstructionTime += System.currentTimeMillis() - startReconstructionTime;

    slf4jlogger.debug("blackout(): elapsedDrawingTime = {}", elapsedDrawingTime);
    slf4jlogger.debug("blackout(): elapsedCopyingTime = {}", elapsedCopyingTime);
    slf4jlogger.debug("blackout(): elapsedReconstructionTime = {}", elapsedReconstructionTime);
    slf4jlogger.debug("blackout(): total blackout time = {}", (System.currentTimeMillis() - startBlackoutTime));
    slf4jlogger.debug("blackout(): done");
  }

  public static final int getOffsetIntoMatrix(int fixedOffset, int row, int column, int height, int width, int rotation, boolean horizontal_flip) {
    int offset = 0;
    if (rotation == 0) {
      offset = fixedOffset + row * width + (horizontal_flip ? (width - column - 1) : column);
    } else if (rotation == 90) {
      offset = fixedOffset + column * height + (horizontal_flip ? row : (height - row - 1));
    } else if (rotation == 180) {
      offset = fixedOffset + (height - row - 1) * width + (horizontal_flip ? column : (width - column - 1));
    } else if (rotation == 270) {
      offset = fixedOffset + (width - column - 1) * height + (horizontal_flip ? (height - row - 1) : row);
    }
    return offset;
  }

  public static final int getOffsetIntoMatrix(int offset, int row, int column, int width) {
    return getOffsetIntoMatrix(offset, row, column, 0/* height not needed */, width, 0, false/*
                                                                                              * no
                                                                                              * flip
                                                                                              */);
  }

  /**
   * <p>
   * Rotate an image in 90 degree increments, optionally followed by a
   * horizontal flip.
   * </p>
   *
   * <p>
   * The accompanying attribute list will be updated with new Pixel Data and
   * related Image Pixel Module attributes.
   * </p>
   *
   * <p>
   * Note that original PhotometricInterpretation will be retained; care should
   * be taken by the caller to change this as appropriate, e.g., from
   * YBR_FULL_422 if read as JPEG to RGB if written as uncompressed. See, for
   * example,
   * {@link com.pixelmed.dicom.AttributeList#correctDecompressedImagePixelModule()
   * AttributeList.correctDecompressedImagePixelModule()}.
   * </p>
   *
   * @param srcImg
   *        the image
   * @param list
   *        the attribute list corresponding image
   * @param rotation
   *        multiple of 90 degrees
   * @param horizontal_flip
   *        whether or not to flip horizontally AFTER rotation
   * @throws DicomException
   *         if something bad happens handling the attribute list
   */
  static public void rotateAndFlip(SourceImage srcImg, AttributeList list, int rotation, boolean horizontal_flip) throws DicomException {
    slf4jlogger.debug("rotate(): requested rotation {}", rotation);
    if (rotation % 90 != 0) {
      throw new DicomException("Rotation of " + rotation + " not supported");
    }
    while (rotation >= 360) {
      rotation -= 360;
    }
    while (rotation < 0) {
      rotation += 360;
    }
    slf4jlogger.debug("rotate(): actual rotation {}", rotation);
    slf4jlogger.debug("rotate(): horizontal_flip {}", horizontal_flip);

    int bitsAllocated = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.BitsAllocated, 0);
    ;
    int bitsStored = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.BitsStored, 0);
    ;
    int highBit = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.HighBit, bitsStored - 1);
    int samplesPerPixel = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.SamplesPerPixel, 1);
    int pixelRepresentation = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.PixelRepresentation, 0);
    String photometricInterpretation = Attribute.getSingleStringValueOrNull(list, TagFromName.PhotometricInterpretation);
    int planarConfiguration = 0; // 0 is color-by-pixel, 1 is color-by-plane

    int srcRows = 0;
    int srcColumns = 0;

    int dstRows = 0;
    int dstColumns = 0;

    byte byteDstPixels[] = null;
    short shortDstPixels[] = null;
    Attribute pixelData = null;

    int dstOffsetToStartOfCurrentFrame = 0;
    int numberOfFrames = srcImg.getNumberOfBufferedImages();
    for (int frame = 0; frame < numberOfFrames; ++frame) {
      BufferedImage src = srcImg.getBufferedImage(frame);
      srcColumns = src.getWidth();
      srcRows = src.getHeight();

      dstColumns = rotation == 90 || rotation == 270 ? srcRows : srcColumns;
      dstRows = rotation == 90 || rotation == 270 ? srcColumns : srcRows;

      SampleModel srcSampleModel = src.getSampleModel();
      int srcDataType = srcSampleModel.getDataType();
      Raster srcRaster = src.getRaster();
      DataBuffer srcDataBuffer = srcRaster.getDataBuffer();
      int srcDataBufferType = srcDataBuffer.getDataType();
      int srcNumBands = srcRaster.getNumBands();
      int srcPixelStride = srcNumBands;
      int srcScanlineStride = srcColumns * srcNumBands;
      if (srcNumBands > 1 && srcSampleModel instanceof ComponentSampleModel) {
        ComponentSampleModel srcComponentSampleModel = (ComponentSampleModel) srcSampleModel;
        srcPixelStride = srcComponentSampleModel.getPixelStride(); // should be
                                                                   // either
                                                                   // srcNumBands
                                                                   // if
                                                                   // color-by-pixel,
                                                                   // or 1 if
                                                                   // color-by-plane
        srcScanlineStride = srcComponentSampleModel.getScanlineStride(); // should
                                                                         // be
                                                                         // either
                                                                         // srcColumns*srcNumBands
                                                                         // if
                                                                         // color-by-pixel,
                                                                         // or
                                                                         // srcColumns
                                                                         // if
                                                                         // color-by-plane
        planarConfiguration = srcPixelStride == srcNumBands ? 0 : 1;
      }
      int srcDataBufferOffset = srcDataBuffer.getOffset();
      int srcFrameLength = srcRows * srcColumns * srcNumBands;
      int srcDataBufferNumBanks = srcDataBuffer.getNumBanks();

      if (srcDataBufferNumBanks > 1) {
        throw new DicomException("Unsupported type of image - DataBuffer number of banks is > 1, is " + srcDataBufferNumBanks);
      }

      slf4jlogger.debug("rotateAndFlip(): srcPixelStride = {}", srcPixelStride);
      int srcBandStride = planarConfiguration == 0 ? 1 : srcRows * srcColumns;
      slf4jlogger.debug("rotateAndFlip(): srcBandStride = {}", srcBandStride);

      int dstPixelStride = planarConfiguration == 0 ? srcNumBands : 1;
      slf4jlogger.debug("rotateAndFlip(): dstPixelStride = {}", dstPixelStride);
      int dstBandStride = planarConfiguration == 0 ? 1 : srcRows * srcColumns;
      slf4jlogger.debug("rotateAndFlip(): dstBandStride = {}", dstBandStride);

      if (srcDataBufferType == DataBuffer.TYPE_BYTE) {
        byte[][] srcPixelBanks = null;
        if (srcDataBuffer instanceof DataBufferByte) {
          srcPixelBanks = ((DataBufferByte) srcDataBuffer).getBankData();
        } else {
          throw new DicomException("Unsupported type of image - DataBuffer is TYPE_BYTE but not instance of DataBufferByte, is " + srcDataBuffer.getClass().getName());
        }
        int srcPixelBankLength = srcPixelBanks[0].length;
        if (byteDstPixels == null) {
          if (bitsAllocated > 8) {
            bitsAllocated = 8;
          }
          if (bitsStored > 8) {
            bitsStored = 8;
          }
          if (highBit > 7) {
            highBit = 7;
          }
          samplesPerPixel = srcNumBands;
          // leave photometricInterpretation alone
          // leave planarConfiguration alone ... already determined from
          // srcPixelStride if srcNumBands > 1
          int dstPixelsLength = srcFrameLength * numberOfFrames;
          byteDstPixels = new byte[dstPixelsLength];
          pixelData = new OtherByteAttribute(TagFromName.PixelData);
          pixelData.setValues(byteDstPixels);
        }
        {
          for (int srcRow = 0; srcRow < srcRows; ++srcRow) {
            for (int srcColumn = 0; srcColumn < srcColumns; ++srcColumn) {
              int srcOffset = getOffsetIntoMatrix(0, srcRow, srcColumn, srcColumns);
              int dstOffset = getOffsetIntoMatrix(0, srcRow, srcColumn, srcRows, srcColumns, rotation, horizontal_flip);
              for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                byteDstPixels[dstOffsetToStartOfCurrentFrame + dstOffset * dstPixelStride + bandIndex * dstBandStride] = srcPixelBanks[0][srcDataBufferOffset + srcOffset * srcPixelStride + bandIndex * srcBandStride];
              }
            }
          }
          dstOffsetToStartOfCurrentFrame += srcFrameLength;
        }
      } else if (srcDataBufferType == DataBuffer.TYPE_USHORT || srcDataBufferType == DataBuffer.TYPE_SHORT) {
        short[][] srcPixelBanks = null;
        if (srcDataBuffer instanceof DataBufferShort) {
          srcPixelBanks = ((DataBufferShort) srcDataBuffer).getBankData();
        } else if (srcDataBuffer instanceof DataBufferUShort) {
          srcPixelBanks = ((DataBufferUShort) srcDataBuffer).getBankData();
        } else {
          throw new DicomException("Unsupported type of image - DataBuffer is TYPE_USHORT or TYPE_SHORT but not instance of DataBufferShort, is " + srcDataBuffer.getClass().getName());
        }
        int srcPixelBankLength = srcPixelBanks[0].length;
        if (shortDstPixels == null) {
          if (bitsAllocated > 16) {
            bitsAllocated = 16;
          }
          if (bitsStored > 16) {
            bitsStored = 16;
          }
          if (highBit > 15) {
            highBit = 15;
          }
          samplesPerPixel = srcNumBands;
          // leave photometricInterpretation alone
          // leave planarConfiguration alone ... already determined from
          // srcPixelStride if srcNumBands > 1
          int dstPixelsLength = srcFrameLength * numberOfFrames;
          shortDstPixels = new short[dstPixelsLength];
          pixelData = new OtherWordAttribute(TagFromName.PixelData);
          pixelData.setValues(shortDstPixels);
        }
        {
          for (int srcRow = 0; srcRow < srcRows; ++srcRow) {
            for (int srcColumn = 0; srcColumn < srcColumns; ++srcColumn) {
              int srcOffset = getOffsetIntoMatrix(0, srcRow, srcColumn, srcColumns);
              int dstOffset = getOffsetIntoMatrix(0, srcRow, srcColumn, srcRows, srcColumns, rotation, horizontal_flip);
              for (int bandIndex = 0; bandIndex < srcNumBands; ++bandIndex) {
                shortDstPixels[dstOffsetToStartOfCurrentFrame + dstOffset * dstPixelStride + bandIndex * dstBandStride] = srcPixelBanks[0][srcDataBufferOffset + srcOffset * srcPixelStride + bandIndex * srcBandStride];
              }
            }
          }
          dstOffsetToStartOfCurrentFrame += srcFrameLength;
        }
      } else {
        throw new DicomException("Unsupported pixel data form - DataBufferType = " + srcDataBufferType);
      }
    }

    list.remove(TagFromName.PixelData);
    list.remove(TagFromName.BitsAllocated);
    list.remove(TagFromName.BitsStored);
    list.remove(TagFromName.HighBit);
    list.remove(TagFromName.SamplesPerPixel);
    list.remove(TagFromName.PixelRepresentation);
    list.remove(TagFromName.PhotometricInterpretation);
    list.remove(TagFromName.PlanarConfiguration);
    boolean numberOfFramesWasPresentBefore = list.get(TagFromName.NumberOfFrames) != null;
    list.remove(TagFromName.NumberOfFrames);

    list.put(pixelData);
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated);
      a.addValue(bitsAllocated);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored);
      a.addValue(bitsStored);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.HighBit);
      a.addValue(highBit);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.Rows);
      a.addValue(dstRows);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.Columns);
      a.addValue(dstColumns);
      list.put(a);
    }
    if (numberOfFrames > 1 || numberOfFramesWasPresentBefore) {
      Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames);
      a.addValue(numberOfFrames);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel);
      a.addValue(samplesPerPixel);
      list.put(a);
    }
    {
      Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation);
      a.addValue(pixelRepresentation);
      list.put(a);
    }
    {
      Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation);
      a.addValue(photometricInterpretation);
      list.put(a);
    }
    if (samplesPerPixel > 1) {
      Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration);
      a.addValue(planarConfiguration);
      list.put(a);
    }

    srcImg.constructSourceImage(list);
  }

}
