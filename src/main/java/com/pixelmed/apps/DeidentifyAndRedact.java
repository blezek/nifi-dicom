/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CompressedFrameDecoder;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.VersionAndConstants;
import com.pixelmed.display.ImageEditUtilities;
import com.pixelmed.display.SourceImage;
import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;
import com.pixelmed.utils.CapabilitiesAvailable;
import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

/**
 * <p>
 * A class to implement bulk de-identification and redaction of DICOM files.
 * </p>
 *
 * <p>
 * Development of this class was supported by funding from MDDX Research and
 * Informatics.
 * </p>
 *
 * @author dclunie
 */
public class DeidentifyAndRedact {
  private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/apps/DeidentifyAndRedact.java,v 1.20 2017/03/29 21:39:16 dclunie Exp $";

  private static final Logger slf4jlogger = LoggerFactory.getLogger(DeidentifyAndRedact.class);

  protected static String ourCalledAETitle = "OURAETITLE";

  /**
   * <p>
   * Make a suitable file name to use for a deidentified and redacted input
   * file.
   * </p>
   *
   * <p>
   * The default is the UID plus "_Anon.dcm" in the outputFolderName (ignoring
   * the inputFileName).
   * </p>
   *
   * <p>
   * Override this method in a subclass if a different file name is required.
   * </p>
   *
   * @param outputFolderName
   *        where to store all the processed output files
   * @param inputFileName
   *        the path to search for DICOM files
   * @param sopInstanceUID
   *        the SOP Instance UID of the output file
   * @exception IOException
   *            if a filename cannot be constructed
   */
  protected String makeOutputFileName(String outputFolderName, String inputFileName, String sopInstanceUID) throws IOException {
    // ignore inputFileName
    return new File(outputFolderName, (sopInstanceUID == null || sopInstanceUID.length() == 0 ? "NOSOPINSTANCEUID" : sopInstanceUID) + "_Anon.dcm").getCanonicalPath();
  }

  /**
   * <p>
   * A protected class that actually does all the work of finding and processing
   * the files.
   * </p>
   *
   */
  protected class OurMediaImporter extends MediaImporter {

    String outputFolderName;
    RedactionRegions redactionRegions;
    boolean decompress;
    boolean keepAllPrivate;
    boolean addContributingEquipmentSequence;
    AttributeList replacementAttributes;
    Set<String> failedSet;

    /**
     * <p>
     * Get file names that failed to import.
     * </p>
     *
     * @return file names that failed to import (empty if did not fail)
     */
    public Set<String> getFilePathNamesThatFailedToProcess() {
      return failedSet;
    }

    /**
     * <p>
     * Deidentify both the DICOM Attributes and the Pixel Data using the
     * RedactionRegions specified in the constructor.
     * </p>
     *
     * @param logger
     * @param outputFolderName
     *        where to store all the processed output files
     * @param redactionRegions
     *        which regions to redact in all the processed files
     * @param decompress
     *        decompress JPEG rather than try to avoid loss in unredacted blocks
     * @param keepAllPrivate
     *        retain all private attributes, not just known safe ones
     * @param addContributingEquipmentSequence
     *        whether or not to add ContributingEquipmentSequence
     * @param replacementAttributes
     *        additional attributes with values to add or replace during
     *        de-identification
     * @param failedSet
     *        set to add paths of files that failed to import (does not have to
     *        be empty; will be added to)
     */
    public OurMediaImporter(MessageLogger logger, String outputFolderName, RedactionRegions redactionRegions, boolean decompress, boolean keepAllPrivate, boolean addContributingEquipmentSequence, AttributeList replacementAttributes,
        Set<String> failedSet) {
      super(logger);
      this.outputFolderName = outputFolderName;
      this.redactionRegions = redactionRegions;
      this.decompress = decompress;
      this.keepAllPrivate = keepAllPrivate;
      this.addContributingEquipmentSequence = addContributingEquipmentSequence;
      this.replacementAttributes = replacementAttributes;
      this.failedSet = failedSet;
    }

    /**
     * <p>
     * Deidentify both the DICOM Attributes and the Pixel Data using the
     * RedactionRegions specified in the constructor.
     * </p>
     *
     * <p>
     * Implements the following options of
     * {@link com.pixelmed.dicom.ClinicalTrialsAttributes#removeOrNullIdentifyingAttributes(AttributeList,int,boolean,boolean,boolean,boolean,boolean,boolean,int,Date,Date)
     * ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes()}:
     * </p>
     * <p>
     * keepDescriptors, keepSeriesDescriptors, keepProtocolName,
     * keepPatientCharacteristics, keepDeviceIdentity, keepInstitutionIdentity,
     * ClinicalTrialsAttributes.HandleDates.keep
     * </p>
     *
     * <p>
     * Also performs
     * {@link com.pixelmed.dicom.AttributeList#removeUnsafePrivateAttributes()
     * AttributeList.removeUnsafePrivateAttributes()}
     * </p>
     * <p>
     * Also performs
     * {@link com.pixelmed.dicom.ClinicalTrialsAttributes#remapUIDAttributes(AttributeList)
     * ClinicalTrialsAttributes.remapUIDAttributes(AttributeList)}
     * </p>
     *
     * <p>
     * If the pixel data can be redacted without decompressing it (i.e., for
     * Baseline JPEG), then that will be done, otherwise the pixel data will be
     * decompressed and store in Explicit VR Little Endian Transfer Syntax.
     * </p>
     *
     * <p>
     * The output file is stored in the outputFolderName specified in the
     * constructor and is named by its SOP Instance UID, a suffix of _Anon" and
     * an extension of ".dcm"
     * </p>
     *
     * <p>
     * Any exceptions encountered during processing are logged to stderr, and
     * processing of the next file will continue.
     * </p>
     *
     * @param dicomFileName
     *        the fully qualified path name to a DICOM file
     * @param inputTransferSyntaxUID
     *        the Transfer Syntax of the Data Set if a DICOM file, from the
     *        DICOMDIR or Meta Information Header
     * @param sopClassUID
     *        the SOP Class of the Data Set if a DICOM file, from the DICOMDIR
     *        or Meta Information Header
     */
    @Override
    protected void doSomethingWithDicomFileOnMedia(String dicomFileName, String inputTransferSyntaxUID, String sopClassUID) {
      // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
      // "+dicomFileName);
      slf4jlogger.info("Processing {} Transfer Syntax {}", dicomFileName, inputTransferSyntaxUID);
      try {
        // copied from DicomCleaner.copyFromOriginalToCleanedPerformingAction()
        // and GUI stuff removed ... should refactor :(
        // long startTime = System.currentTimeMillis();
        File file = new File(dicomFileName);
        DicomInputStream i = new DicomInputStream(file);
        AttributeList list = new AttributeList();

        // Do not decompress the pixel data if we can redact the JPEG (unless
        // overriddden), but let AttributeList.read() decompress the pixel data
        // for all other formats (lossy or not)
        boolean doitwithjpeg = !decompress && inputTransferSyntaxUID.equals(TransferSyntax.JPEGBaseline) && CapabilitiesAvailable.haveJPEGBaselineSelectiveBlockRedaction();
        slf4jlogger.info("OurMediaImporter.doSomethingWithDicomFile(): doitwithjpeg = {}", doitwithjpeg);
        boolean deferredDecompression = !doitwithjpeg && CompressedFrameDecoder.canDecompress(file);
        slf4jlogger.info("OurMediaImporter.doSomethingWithDicomFile(): deferredDecompression = {}", deferredDecompression);
        list.setDecompressPixelData(!doitwithjpeg && !deferredDecompression); // we
                                                                              // don't
                                                                              // want
                                                                              // to
                                                                              // decompress
                                                                              // it
                                                                              // during
                                                                              // read
                                                                              // if
                                                                              // we
                                                                              // can
                                                                              // redact
                                                                              // the
                                                                              // JPEG,
                                                                              // or
                                                                              // decompress
                                                                              // it
                                                                              // on
                                                                              // the
                                                                              // fly
                                                                              // during
                                                                              // redaction
                                                                              // (000848)
        String outputTransferSyntaxUID = doitwithjpeg ? TransferSyntax.JPEGBaseline : TransferSyntax.ExplicitVRLittleEndian;

        // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
        // list.read()");
        list.read(i);
        // long currentTime = System.currentTimeMillis();
        // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
        // reading AttributeList took = "+(currentTime-startTime)+" ms");
        // startTime=currentTime;
        i.close();

        Attribute aPixelData = list.getPixelData();
        // String inputTransferSyntaxUID =
        // Attribute.getSingleStringValueOrEmptyString(list,TagFromName.TransferSyntaxUID);
        if (aPixelData != null) {
          if (redactionRegions != null) {
            String classNameForThisImage = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.Columns, 0) + "x" + Attribute.getSingleIntegerValueOrDefault(list, TagFromName.Rows, 0);
            // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
            // redactionRegion selector classNameForThisImage =
            // "+classNameForThisImage);
            Vector<Shape> redactionShapes = redactionRegions.getRedactionRegionShapes(classNameForThisImage);
            if (redactionShapes != null && redactionShapes.size() > 0) {
              if (doitwithjpeg && aPixelData instanceof OtherByteAttributeMultipleCompressedFrames) {
                // Pixel Data was not decompressed so can redact it without loss
                // outside the redacted regions
                // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
                // lossless redaction of JPEG pixels");
                byte[][] frames = ((OtherByteAttributeMultipleCompressedFrames) aPixelData).getFrames();
                for (int f = 0; f < frames.length; ++f) {
                  ByteArrayInputStream fbis = new ByteArrayInputStream(frames[f]);
                  ByteArrayOutputStream fbos = new ByteArrayOutputStream();
                  // com.pixelmed.codec.jpeg.Parse.parse(fbis, fbos,
                  // redactionShapes);
                  frames[f] = fbos.toByteArray(); // hmmm :(
                }
              } else {
                // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
                // redaction of fully decompressed pixels");
                SourceImage sImg = new SourceImage(list);
                // may not have been decompressed during AttributeList reading
                // when CompressedFrameDecoder.canDecompress(currentFile) is
                // true,
                // but ImageEditUtilities.blackout() can decompress on the fly
                // because it calls SourceImage.getBufferedImage(frame);
                // we like that because it uses less (esp. contiguous) memory
                // but we need to make sure when writing it during Save that
                // PhotometricInterpretation is corrected, etc. vide infra
                ImageEditUtilities.blackout(sImg, list, redactionShapes, true/*
                                                                              * burnInOverlays
                                                                              */, false/*
                                                                                        * usePixelPaddingBlackoutValue
                                                                                        */, true/*
                                                                                                 * useSpecifiedBlackoutValue
                                                                                                 */, 0/*
                                                                                                       * specifiedBlackoutValue
                                                                                                       */);
              }
            } else {
              // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
              // no redaction shapes specified");
            }
          } else {
            // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
            // no redaction regions specified");
          }
        } else {
          // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
          // not an image ... only de-identifying non-PixelData attributes");
        }

        list.removeGroupLengthAttributes();
        list.correctDecompressedImagePixelModule(deferredDecompression); // make
                                                                         // sure
                                                                         // to
                                                                         // correct
                                                                         // even
                                                                         // if
                                                                         // decompression
                                                                         // was
                                                                         // deferred
        list.insertLossyImageCompressionHistoryIfDecompressed(deferredDecompression);
        list.removeMetaInformationHeaderAttributes();

        ClinicalTrialsAttributes.removeClinicalTrialsAttributes(list);
        ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes(list,
            ClinicalTrialsAttributes.HandleUIDs.keep,
            true/* keepDescriptors */,
            true/* keepSeriesDescriptors */,
            true/* keepProtocolName */,
            true/* keepPatientCharacteristics */,
            true/* keepDeviceIdentity */,
            true/* keepInstitutionIdentity */,
            ClinicalTrialsAttributes.HandleDates.keep,
            null/* epochForDateModification */,
            null/* earliestDateInSet */);

        // NB. ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes() will
        // have already
        // added both DeidentificationMethod and
        // DeidentificationMethodCodeSequence
        // so now we can assume their presence without checking

        Attribute aDeidentificationMethod = list.get(TagFromName.DeidentificationMethod);
        SequenceAttribute aDeidentificationMethodCodeSequence = (SequenceAttribute) (list.get(TagFromName.DeidentificationMethodCodeSequence));

        aDeidentificationMethod.addValue("Burned in text redacted");
        aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("113101", "DCM", "Clean Pixel Data Option").getAttributeList());
        {
          Attribute a = new CodeStringAttribute(TagFromName.BurnedInAnnotation);
          a.addValue("NO");
          list.put(a);
        }

        if (keepAllPrivate) {
          aDeidentificationMethod.addValue("All private retained");
          aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("210002", "99PMP", "Retain all private elements").getAttributeList());
        } else {
          list.removeUnsafePrivateAttributes();
          aDeidentificationMethod.addValue("Unsafe private removed");
          aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("113111", "DCM", "Retain Safe Private Option").getAttributeList());
        }

        ClinicalTrialsAttributes.remapUIDAttributes(list);
        aDeidentificationMethod.addValue("UIDs remapped");

        {
          // remove the default Retain UIDs added by
          // ClinicalTrialsAttributes.removeOrNullIdentifyingAttributes() with
          // the ClinicalTrialsAttributes.HandleUIDs.keep option
          Iterator<SequenceItem> it = aDeidentificationMethodCodeSequence.iterator();
          while (it.hasNext()) {
            SequenceItem item = it.next();
            if (item != null) {
              CodedSequenceItem testcsi = new CodedSequenceItem(item.getAttributeList());
              if (testcsi != null) {
                String cv = testcsi.getCodeValue();
                String csd = testcsi.getCodingSchemeDesignator();
                if (cv != null && cv.equals("113110") && csd != null && csd.equals("DCM")) { // "Retain
                                                                                             // UIDs
                                                                                             // Option"
                  it.remove();
                }
              }
            }
          }
        }
        aDeidentificationMethodCodeSequence.addItem(new CodedSequenceItem("210001", "99PMP", "Remap UIDs").getAttributeList());

        if (addContributingEquipmentSequence) {
          ClinicalTrialsAttributes.addContributingEquipmentSequence(list,
              true,
              new CodedSequenceItem("109104", "DCM", "De-identifying Equipment"), // per
                                                                                  // CP
                                                                                  // 892
              "PixelMed", // Manufacturer
              null, // Institution Name
              null, // Institutional Department Name
              null, // Institution Address
              ourCalledAETitle, // Station Name
              "DeidentifyAndRedact.main()", // Manufacturer's Model Name
              null, // Device Serial Number
              VersionAndConstants.getBuildDate(), // Software Version(s)
              "Deidentified and Redacted");
        }

        if (replacementAttributes != null) {
          list.putAll(replacementAttributes);
        }

        FileMetaInformation.addFileMetaInformation(list, outputTransferSyntaxUID, ourCalledAETitle);
        list.insertSuitableSpecificCharacterSetForAllStringValues(); // E.g.,
                                                                     // may have
                                                                     // de-identified
                                                                     // Kanji
                                                                     // name and
                                                                     // need new
                                                                     // character
                                                                     // set
        // currentTime = System.currentTimeMillis();
        // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
        // cleaning AttributeList took = "+(currentTime-startTime)+" ms");
        // startTime=currentTime;
        // File cleanedFile = File.createTempFile("clean",".dcm");
        // cleanedFile.deleteOnExit();
        // File cleanedFile = new
        // File(outputFolderName,Attribute.getSingleStringValueOrDefault(list,TagFromName.SOPInstanceUID,"NOSOPINSTANCEUID")+"_Anon.dcm");
        File cleanedFile = new File(makeOutputFileName(outputFolderName, dicomFileName, Attribute.getSingleStringValueOrEmptyString(list, TagFromName.SOPInstanceUID)));
        list.write(cleanedFile, outputTransferSyntaxUID, true/* useMeta */, true/*
                                                                                 * useBufferedStream
                                                                                 */);
        // currentTime = System.currentTimeMillis();
        // System.err.println("DeidentifyAndRedact.OurMediaImporter.doSomethingWithDicomFile():
        // writing AttributeList took = "+(currentTime-startTime)+" ms");
        // startTime=currentTime;
        // logger.sendLn("Deidentified and Redacted "+dicomFileName+" into
        // "+cleanedFile.getCanonicalPath());
        slf4jlogger.info("Deidentified and Redacted {} into {}", dicomFileName, cleanedFile.getCanonicalPath());
      } catch (Exception e) {
        slf4jlogger.error("Failed to process " + dicomFileName + " ", e);
        failedSet.add(dicomFileName);
      }
    }

    protected boolean canUseBzip = CapabilitiesAvailable.haveBzip2Support();

    // override base class isOKToImport(), which rejects unsupported compressed
    // transfer syntaxes

    /**
     * <p>
     * Allows all types of DICOM files, images or not, uncompressed or
     * compressed, with supported Transfer Syntaxes to be processed
     * </p>
     *
     * @param sopClassUID
     * @param transferSyntaxUID
     */
    @Override
    protected boolean isOKToImport(String sopClassUID, String transferSyntaxUID) {
      return sopClassUID != null
          && (SOPClass.isImageStorage(sopClassUID) || (SOPClass.isNonImageStorage(sopClassUID) && !SOPClass.isDirectory(sopClassUID)))
          && transferSyntaxUID != null
          && (transferSyntaxUID.equals(TransferSyntax.ImplicitVRLittleEndian)
              || transferSyntaxUID.equals(TransferSyntax.ExplicitVRLittleEndian)
              || transferSyntaxUID.equals(TransferSyntax.ExplicitVRBigEndian)
              || transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian)
              || (transferSyntaxUID.equals(TransferSyntax.DeflatedExplicitVRLittleEndian) && canUseBzip)
              || transferSyntaxUID.equals(TransferSyntax.RLE)
              || transferSyntaxUID.equals(TransferSyntax.JPEGBaseline)
              || CapabilitiesAvailable.haveJPEGLosslessCodec() && (transferSyntaxUID.equals(TransferSyntax.JPEGLossless) || transferSyntaxUID.equals(TransferSyntax.JPEGLosslessSV1))
              || CapabilitiesAvailable.haveJPEG2000Part1Codec() && (transferSyntaxUID.equals(TransferSyntax.JPEG2000) || transferSyntaxUID.equals(TransferSyntax.JPEG2000Lossless))
              || CapabilitiesAvailable.haveJPEGLSCodec() && (transferSyntaxUID.equals(TransferSyntax.JPEGLS) || transferSyntaxUID.equals(TransferSyntax.JPEGNLS)));
    }
  }

  /**
   * <p>
   * A protected class to store sets of rectangular redaction regions indexed by
   * a String classname.
   * </p>
   *
   */
  public class RedactionRegions {
    private final Map<String, Vector<Shape>> regionsByClassName = new HashMap<String, Vector<Shape>>();

    /**
     * <p>
     * Construct the redaction regions from a text file.
     * </p>
     *
     * <p>
     * The format for each line is "class=(x,y,w,h)[;(x,y,w,h)]*", e.g., where
     * class = "columnsxrows". E.g., "800x600 = (0,0,639,150)" (without the
     * quotes)
     * </p>
     *
     * @param fileName
     * @exception Exception
     */
    public RedactionRegions(String fileName) throws Exception {
      // System.err.println("RedactionRegions");
      // one or more lines consisting of "class=(x,y,w,h)[;(x,y,w,h)]*",
      // e.g.,where class = "columnsxrows"
      BufferedReader r = new BufferedReader(new FileReader(fileName));
      String line = null;
      while ((line = r.readLine()) != null) {
        // System.err.println("line = \""+line+"\"");
        line = line.trim();
        if (!line.startsWith("#") && line.length() > 0) {
          line = line.replaceAll(" ", "");
          String[] classNameAndRegions = line.split("=", 2);
          if (classNameAndRegions.length != 2) {
            throw new Exception("Missing delimiter between class name and regions");
          }
          String[] regions = classNameAndRegions[1].split(";");
          Vector<Shape> shapes = new Vector<Shape>();
          for (String region : regions) {
            Pattern pRegion = Pattern.compile("[(]([0-9]+),([0-9]+),([0-9]+),([0-9]+)[)]");
            Matcher mRegion = pRegion.matcher(region);
            if (mRegion.matches() && mRegion.groupCount() == 4) {
              int x = Integer.parseInt(mRegion.group(1));
              int y = Integer.parseInt(mRegion.group(2));
              int w = Integer.parseInt(mRegion.group(3));
              int h = Integer.parseInt(mRegion.group(4));
              Rectangle rectangle = new Rectangle(x, y, w, h);
              // System.err.println("rectangle = \""+rectangle+"\"");
              shapes.add(rectangle);
            } else {
              throw new Exception("Malformed region \"" + region + "\" does not match expected (x,y,w,h)");
            }
          }
          Vector<Shape> regionsForThisClass = regionsByClassName.get(classNameAndRegions[0]);
          if (regionsForThisClass != null) {
            System.err.println("Warning: shapes already specified in previous line(s) for class = \"" + classNameAndRegions[0] + "\" -  appending the new regions to the previous list");
            regionsForThisClass.addAll(shapes);
          } else {
            regionsByClassName.put(classNameAndRegions[0], shapes);
          }
        }
      }
    }

    /**
     * <p>
     * Find the redaction regions for the specified class name.
     * </p>
     *
     * @param className
     *        a String of the form "colsxrows" to match the Rows and Columns
     *        values of the image
     * @return the Vector of Shape for the requested class name, or null if not
     *         found
     */
    public Vector<Shape> getRedactionRegionShapes(String className) {
      return regionsByClassName.get(className);
    }
  }

  protected Set<String> failedSet;

  /**
   * <p>
   * Get file names that failed to import.
   * </p>
   *
   * @return file names that failed to import (empty if did not fail)
   */
  public Set<String> getFilePathNamesThatFailedToProcess() {
    return failedSet;
  }

  /**
   * <p>
   * Read DICOM format image files, de-identify them and apply any specified
   * redactions to the Pixel Data.
   * </p>
   *
   * <p>
   * Searches the specified input path recursively for suitable files.
   * </p>
   *
   * If Baseline (sequential 8 bit) JPEG, can either redact without affecting
   * other JPEG blocks or decompress JPEG entirely, redact and recompress
   * (lossy)
   *
   * <p>
   * For details of the processing, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.OurMediaImporter#doSomethingWithDicomFileOnMedia(String,String,String)}.
   * </p>
   *
   * <p>
   * For specification of the contents of the redaction control file, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.RedactionRegions}.
   * </p>
   *
   * @param inputPathName
   *        the path to search for DICOM files
   * @param outputFolderName
   *        where to store all the processed output files
   * @param redactionControlFileName
   *        which regions to redact in all the processed files
   * @param decompress
   *        decompress JPEG rather than try to avoid loss in unredacted blocks
   * @param keepAllPrivate
   *        retain all private attributes, not just known safe ones
   * @param addContributingEquipmentSequence
   *        whether or not to add ContributingEquipmentSequence
   * @param replacementAttributes
   *        additional attributes with values to add or replace during
   *        de-identification
   * @exception DicomException
   * @exception IOException
   * @exception Exception
   */
  public DeidentifyAndRedact(String inputPathName, String outputFolderName, String redactionControlFileName, boolean decompress, boolean keepAllPrivate, boolean addContributingEquipmentSequence, AttributeList replacementAttributes)
      throws DicomException, Exception, IOException {
    {
      String osname = System.getProperty("os.name");
      if (osname != null && osname.toLowerCase().startsWith("windows")) {
        slf4jlogger.info("disabling memory mapping for SourceImage on Windows platform");
        SourceImage.setAllowMemoryMapping(false); // otherwise problems with
                                                  // redacting large
      }
    }
    RedactionRegions redactionRegions = redactionControlFileName.trim().length() == 0 ? null : new RedactionRegions(redactionControlFileName);
    MessageLogger logger = new PrintStreamMessageLogger(System.err);
    failedSet = new HashSet<String>();
    OurMediaImporter importer = new OurMediaImporter(logger, outputFolderName, redactionRegions, decompress, keepAllPrivate, addContributingEquipmentSequence, replacementAttributes, failedSet);
    importer.importDicomFiles(inputPathName);
  }

  /**
   * <p>
   * Read DICOM format image files, de-identify them and apply any specified
   * redactions to the Pixel Data.
   * </p>
   *
   * <p>
   * Searches the specified input path recursively for suitable files.
   * </p>
   *
   * If Baseline (sequential 8 bit) JPEG, can either redact without affecting
   * other JPEG blocks or decompress JPEG entirely, redact and recompress
   * (lossy)
   *
   * <p>
   * For details of the processing, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.OurMediaImporter#doSomethingWithDicomFileOnMedia(String,String,String)}.
   * </p>
   *
   * <p>
   * For specification of the contents of the redaction control file, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.RedactionRegions}.
   * </p>
   *
   * <p>
   * Adds ContributingEquipmentSequence by default.
   * </p>
   *
   * @param inputPathName
   *        the path to search for DICOM files
   * @param outputFolderName
   *        where to store all the processed output files
   * @param redactionControlFileName
   *        which regions to redact in all the processed files
   * @param decompress
   *        decompress JPEG rather than try to avoid loss in unredacted blocks
   * @param keepAllPrivate
   *        retain all private attributes, not just known safe ones
   * @param replacementAttributes
   *        additional attributes with values to add or replace during
   *        de-identification
   * @exception DicomException
   * @exception IOException
   * @exception Exception
   */
  public DeidentifyAndRedact(String inputPathName, String outputFolderName, String redactionControlFileName, boolean decompress, boolean keepAllPrivate, AttributeList replacementAttributes) throws DicomException, Exception, IOException {
    this(inputPathName, outputFolderName, redactionControlFileName, decompress, keepAllPrivate, true/*
                                                                                                     * addContributingEquipmentSequence
                                                                                                     */, replacementAttributes);
  }

  /**
   * <p>
   * Read DICOM format image files, de-identify them and apply any specified
   * redactions to the Pixel Data.
   * </p>
   *
   * <p>
   * Searches the specified input path recursively for suitable files.
   * </p>
   *
   * If Baseline (sequential 8 bit) JPEG, can either redact without affecting
   * other JPEG blocks or decompress JPEG entirely, redact and recompress
   * (lossy)
   *
   * <p>
   * For details of the processing, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.OurMediaImporter#doSomethingWithDicomFileOnMedia(String,String,String)}.
   * </p>
   *
   * <p>
   * For specification of the contents of the redaction control file, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.RedactionRegions}.
   * </p>
   *
   * @param inputPathName
   *        the path to search for DICOM files
   * @param outputFolderName
   *        where to store all the processed output files
   * @param redactionControlFileName
   *        which regions to redact in all the processed files
   * @param decompress
   *        decompress JPEG rather than try to avoid loss in unredacted blocks
   * @param keepAllPrivate
   *        retain all private attributes, not just known safe ones
   * @param addContributingEquipmentSequence
   *        whether or not to add ContributingEquipmentSequence
   * @exception DicomException
   * @exception IOException
   * @exception Exception
   */
  public DeidentifyAndRedact(String inputPathName, String outputFolderName, String redactionControlFileName, boolean decompress, boolean keepAllPrivate, boolean addContributingEquipmentSequence) throws DicomException, Exception, IOException {
    this(inputPathName, outputFolderName, redactionControlFileName, decompress, keepAllPrivate, addContributingEquipmentSequence, null/*
                                                                                                                                       * replacementAttributes
                                                                                                                                       */);
  }

  /**
   * <p>
   * Read DICOM format image files, de-identify them and apply any specified
   * redactions to the Pixel Data.
   * </p>
   *
   * <p>
   * Searches the specified input path recursively for suitable files.
   * </p>
   *
   * If Baseline (sequential 8 bit) JPEG, can either redact without affecting
   * other JPEG blocks or decompress JPEG entirely, redact and recompress
   * (lossy)
   *
   * <p>
   * For details of the processing, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.OurMediaImporter#doSomethingWithDicomFileOnMedia(String,String,String)}.
   * </p>
   *
   * <p>
   * For specification of the contents of the redaction control file, see
   * {@link com.pixelmed.apps.DeidentifyAndRedact.RedactionRegions}.
   * </p>
   *
   * <p>
   * Adds ContributingEquipmentSequence by default.
   * </p>
   *
   * @param inputPathName
   *        the path to search for DICOM files
   * @param outputFolderName
   *        where to store all the processed output files
   * @param redactionControlFileName
   *        which regions to redact in all the processed files
   * @param decompress
   *        decompress JPEG rather than try to avoid loss in unredacted blocks
   * @param keepAllPrivate
   *        retain all private attributes, not just known safe ones
   * @exception DicomException
   * @exception IOException
   * @exception Exception
   */
  public DeidentifyAndRedact(String inputPathName, String outputFolderName, String redactionControlFileName, boolean decompress, boolean keepAllPrivate) throws DicomException, Exception, IOException {
    this(inputPathName, outputFolderName, redactionControlFileName, decompress, keepAllPrivate, true/*
                                                                                                     * addContributingEquipmentSequence
                                                                                                     */, null/*
                                                                                                              * replacementAttributes
                                                                                                              */);
  }

  /**
   * <p>
   * Read DICOM format image files, de-identify them and apply any specified
   * redactions to the Pixel Data.
   * </p>
   *
   * Searches the specified input path recursively for suitable files
   *
   * If Baseline (sequential 8 bit) JPEG, can either redact without affecting
   * other JPEG blocks or decompress JPEG entirely, redact and recompress
   * (lossy)
   *
   * @param arg
   *        three, four or more parameters, the inputPath (file or folder),
   *        outputFolder, redactionControlFile, optionally the redaction method
   *        BLOCK|DECOMPRESS (default is BLOCK), optionally whether or not to
   *        keep all or just known safe private data elements
   *        KEEPALLPRIVATE|KEEPSAFEPRIVATE (default is KEEPSAFEPRIVATE),
   *        optionally whether or not to add ContributingEquipmentSequence
   *        ADDCONTRIBUTINGEQUIPMENT|DONOTADDCONTRIBUTINGEQUIPMENT (default is
   *        ADDCONTRIBUTINGEQUIPMENT), followed by optional pairs of keyword and
   *        value attribute replacements (e.g., PatientName "Doe^Jane")
   */
  public static void main(String arg[]) {
    try {
      boolean bad = false;
      if (arg.length >= 3) {
        AttributeList replacementAttributes = null;
        int startReplacements = 3;
        boolean decompress = false;
        boolean keepAllPrivate = false;
        boolean addContributingEquipmentSequence = true;
        if ((arg.length - startReplacements) > 0) {
          String option = arg[startReplacements].trim().toUpperCase();
          if (option.equals("DECOMPRESS")) {
            decompress = true;
            ++startReplacements;
          } else if (option.equals("BLOCK")) {
            ++startReplacements;
          }
        }
        // System.err.println("DeidentifyAndRedact.main(): decompress =
        // "+decompress);
        if ((arg.length - startReplacements) > 0) {
          String option = arg[startReplacements].trim().toUpperCase();
          if (option.equals("KEEPALLPRIVATE")) {
            keepAllPrivate = true;
            ++startReplacements;
          } else if (option.equals("KEEPSAFEPRIVATE")) {
            ++startReplacements;
          }
        }
        // System.err.println("DeidentifyAndRedact.main(): keepAllPrivate =
        // "+keepAllPrivate);
        if ((arg.length - startReplacements) > 0) {
          String option = arg[startReplacements].trim().toUpperCase();
          if (option.equals("ADDCONTRIBUTINGEQUIPMENT")) {
            addContributingEquipmentSequence = true;
            ++startReplacements;
          } else if (option.equals("DONOTADDCONTRIBUTINGEQUIPMENT")) {
            addContributingEquipmentSequence = false;
            ++startReplacements;
          }
        }
        // System.err.println("DeidentifyAndRedact.main():
        // addContributingEquipmentSequence =
        // "+addContributingEquipmentSequence);
        if (arg.length > startReplacements) {
          if ((arg.length - startReplacements) % 2 == 0) { // replacement
                                                           // keyword/value
                                                           // pairs must be
                                                           // pairs
            // System.err.println("DeidentifyAndRedact.main(): have replacement
            // attributes");
            replacementAttributes = AttributeList.makeAttributeListFromKeywordAndValuePairs(arg, startReplacements, arg.length - startReplacements);
            // System.err.print("DeidentifyAndRedact.main(): the replacement
            // attributes are:\n"+replacementAttributes);
          } else {
            slf4jlogger.error("Replacement keyword/value pairs must be pairs");
            bad = true;
          }
        }
        if (!bad) {
          long startTime = System.currentTimeMillis();
          DeidentifyAndRedact d = new DeidentifyAndRedact(arg[0], arg[1], arg[2], decompress, keepAllPrivate, addContributingEquipmentSequence, replacementAttributes);
          long currentTime = System.currentTimeMillis();
          slf4jlogger.info("DeidentifyAndRedact entire set took = {} ms", (currentTime - startTime));

          Set<String> failedSet = d.getFilePathNamesThatFailedToProcess();
          if (failedSet.isEmpty()) {
            slf4jlogger.info("successfully deidentified and redacted all files");
          } else {
            slf4jlogger.info("failed to deidentify and redact {} files", failedSet.size());
            for (String failedFileName : failedSet) {
              slf4jlogger.error("failed to deidentify and redact {}", failedFileName);
            }
          }
        }
      } else {
        slf4jlogger.error("Incorrect number of arguments");
        bad = true;
      }
      if (bad) {
        slf4jlogger.error("Usage: DeidentifyAndRedact inputPath outputFile redactionControlFile [BLOCK|DECOMPRESS] [KEEPALLPRIVATE|KEEPSAFEPRIVATE] [keyword value]*");
        System.exit(1);
      }
    } catch (Exception e) {
      slf4jlogger.error("", e); // use SLF4J since may be invoked from script
    }
  }
}
