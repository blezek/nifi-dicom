# DICOM for NiFi

[`nifi-dicom`](https://github.com/blezek/nifi-dicom) adds DICOM features to Apache's [NiFi](https://nifi.apache.org/) package.  The new [`Processors`](https://nifi.apache.org/docs/nifi-docs/html/developer-guide.html) reside in a `nar` file and extend NiFi with several new processors.  The `Processors` are self-documenting, with some details below.

The `nar` file releases can be [downloaded from GitHub](https://github.com/blezek/nifi-dicom/releases).

## Building

```bash
# Run tests
./gradlew test
# Build the nar file, in build/libs
./gradlew nar
```

## Install

To install `nifi-dicom` copy the `nar` file into the `lib` directory of your NiFi install and restart NiFi.

```bash
cp build/libs/nifi-dicom*.nar $NIFI_HOME/lib
```

## Notes on Encryption

[DICOM Supplement 55 Attribute Level Confidentiality](http://dicom.nema.org/Dicom/supps/sup55_03.pdf) covers the proper procedure for deidentification of DICOM data, and potential later recovery of the original PHI.  The secret sauce is to encrypt a set of tags and embed into the deidentified DICOM.  Later, with the proper key or password, the data can be recovered.

A [Nifi controller](https://nifi.apache.org/docs/nifi-docs/html/user-guide.html#Controller_Services) providing deidentification using the [PixelMedNet DICOM Cleaner](http://www.pixelmed.com/cleaner.html) application.  Removed or modified attributes are encrypted using the [Bouncy Castle FIPS](https://www.bouncycastle.org/fips_faq.html) code (distributed with the source code).

To keep `UID` remapping consistent, this processor needs to be associated with a `DeidentificationService`.



`DeidentifyEncryptDICOM` has these relevant properties:

* `Password`: password used to encrypt, needed for decryption
* `Iterations`: number of iterations to use in encryption, more is better for security but costs CPU cycles

`DecryptReidentifyDICOM` decrypts and reidentifies DICOM data.  Must use the same `password` as `DeidentifyEncryptDICOM` or the data will not be recoverable.  Has the option (`Accept new series`) to preserve the `SeriesInstanceUID` and `SOPInstanceUID` in the deidentified data.  This is mainly useful for analytics that create new series and instances.

Relevant properties:

* `Password`: password for decryption, must match the `DeidentifyEncryptDICOM` password
* `Accept new series`: if `true`, new series are allowed, otherwise they are rejected

## Processors

### DeidentifyDICOM

This processor implements a DICOM deidentifier.  The DeidentifyDICOM processor substitutes DICOM tags with deidentified values and stores the values.

#### Properties:

* `Deidentification controller`: Specified the deidentification controller for DICOM deidentification
* `Generate identification`: Create generated identifiers if the patient name did not match the Identifier CSV file
* `Keep descriptors`: Keep text description and comment attributes
* `Keep series descriptors`: Keep the series description even if all other descriptors are removed
* `Keep protocol name`: Keep protocol name even if all other descriptors are removed
* `Keep patient characteristics`: Keep patient characteristics (such as might be needed for PET SUV calculations)
* `Keep device identity`: Keep device identity
* `Keep institution identity`: Keep institution identity
* `Keep private tags`: Keep all private tags.  If set to 'false', all unsafe private tags are removed.
* `Add contributing equipment sequence`: Add tags indicating the software used for deidentification

#### Relationships:

* `success`: All deidentified DICOM images will be routed as FlowFiles to this relationship
* `not_matched`: DICOM files that do not match the patient remapping are routed to this relationship
* `failure`: FlowFiles that are not DICOM images

#### FlowFile attributes:

* **N/A**: does not set attributes

### ExtractDICOMTags

This processor extracts DICOM tags from the DICOM image and sets the values at attributes of the flowfile.  **Note:** this processor reads the entire file including all pixel data.

#### Properties:

* `Extract all DICOM tags`: Extract all DICOM tags if true, only listed tags if false
* `Construct suggested filename`: Construct a filename of the pattern 'PatientName/Modality_Date/SeriesNumber_SeriesDescription/SOPInstanceUID.dcm' with all unacceptable characters mapped to '_'
* `<TagName>`: any named Tag, for instance, `SeriesDescription`, `PatientId`.  Any Tag defined by `dcm4che` is accessable.  Missing Tags, or unknown Tags are ignored.  The `dcm4che` [Tags are generated dynamically from XML files](https://github.com/dcm4che/dcm4che/tree/master/dcm4che-dict/src/main/resources) 

#### Relationships:

* `success`: All DICOM images will be routed as FlowFiles to this relationship
* `failure`: FlowFiles that are not DICOM images

#### FlowFile attributes:

* **N/A**: does not set attributes

### ListenDICOM

This processor implements a DICOM receiver to listen for incoming DICOM images.

#### Properties:

* `Local Application Entity Title`: ListenDICOM requires that remote DICOM Application Entities use this AE Title when sending DICOM, default is to accept all called AE Titles
* `Listening port`: The TCP port the ListenDICOM processor will bind to.

#### Relationships:

* `success`: All new DICOM images will be routed as FlowFiles to this relationship

#### FlowFile attributes:

* `dicom.calling.aetitle`: The sending AE title
* `dicom.calling.hostname`: The sending hostname
* `dicom.called.aetitle`: The receiving AE title
* `dicom.called.hostname`: The receiving hostname
* `dicom.called.hostname`: The receiving hostname

### PutDICOM

This processor implements a DICOM sender, sending DICOM images to the specified destination.

#### Properties:

* `Remote Application Entity Title`: 
* `Remote hostname of remote DICOM destination`: 
* `Remote Port`: The TCP port to send to.
* `Local Application Entity`: 
* `batch size`: maxmium number of DICOM images to send at once, 0 is unlimited

#### Relationships:

* `success`: FlowFiles that are successfully sent will be routed to success
* `reject`: FlowFiles that are not DICOM images
* `failure`: FlowFiles that failed to send to the remote system; failure is usually looped back to this processor

#### FlowFile attributes:

* **N/A**: does not set attributes

### ModifyDICOMTags

This processor modifies DICOM tags. DICOM Tags listed as Properities are replaced by their value.  Tags are named according to the `ExtractDICOMTags` processor documented above. 

#### Properties:

* `<Tag>`: the value of the property is written as the value to `Tag`

#### Relationships:

* `success`: All modified DICOM images will be routed as FlowFiles to this relationship
* `failure`: FlowFiles that are not DICOM images

#### FlowFile attributes:

* **N/A**: does not set attributes

### DeidentifyEncryptDICOM

This processor implements a DICOM deidentifier.  Deidentified DICOM tags are encrypted using a password for later decription and re-identification.

#### Properties:

* `Encryption password`: Encryption password, leave empty or unset if deidintified or removed attributes are not to be encripted
* `Encryption iterations`: Number of encription rounds.  Higher number of iterations are typically more secure, but require more per-image computation
* `Keep descriptors`: Keep text description and comment attributes
* `Keep series descriptors`: Keep the series description even if all other descriptors are removed
* `Keep protocol name`: Keep protocol name even if all other descriptors are removed
* `Keep patient characteristics`: Keep patient characteristics (such as might be needed for PET SUV calculations)
* `Keep device identity`: Keep device identity
* `Keep institution identity`: Keep institution identity
* `Keep private tags`: Keep all private tags.  If set to 'false', all unsafe private tags are removed.
* `Add contributing equipment sequence`: Add tags indicating the software used for deidentification

#### Relationships:

* `success`: All deidentified DICOM images will be routed as FlowFiles to this relationship
* `failure`: FlowFiles that are not DICOM images

#### FlowFile attributes:

* **N/A**: does not set attributes

### DecryptReidentifyDICOM

This processor implements a DICOM reidentifier.  Previously deidintified DICOM files with Supplement 55 encrypted tags have the original tags decrypted and the reidentified image is written as a FlowFile.

#### Properties:

* `Encryption password`: Encryption password, leave empty or unset if deidintified or removed attributes are not to be encripted
* `Accept new series`: If the encrypted, generated Series and Instance UIDs do not match the DICOM object, assume this DICOM image is a new series generated from a deidentified, encrypted DICOM image.  Decrypt the original tags, but do not replace the Series and SOPInstance UIDs, effectively creating a new series
* `Batch size`: Number of DICOM files to process in batch

#### Relationships:

* `success`: All deidentified DICOM images will be routed as FlowFiles to this relationship
* `failure`: FlowFiles that are not DICOM images
* `not decrypted`: DICOM images that could not be sucessfully decrypted

#### FlowFile attributes:

* **N/A**: does not set attributes

### DeidentifyEncryptDICOM

This processor implements a DICOM deidentifier.  Deidentified DICOM tags are encrypted using a password for later decription and re-identification.

#### Properties:

* `Encryption password`: Encryption password, leave empty or unset if deidintified or removed attributes are not to be encripted
* `Encryption iterations`: Number of encription rounds.  Higher number of iterations are typically more secure, but require more per-image computation
* `Keep descriptors`: Keep text description and comment attributes
* `Keep series descriptors`: Keep the series description even if all other descriptors are removed
* `Keep protocol name`: Keep protocol name even if all other descriptors are removed
* `Keep patient characteristics`: Keep patient characteristics (such as might be needed for PET SUV calculations)
* `Keep device identity`: Keep device identity
* `Keep institution identity`: Keep institution identity
* `Keep private tags`: Keep all private tags.  If set to 'false', all unsafe private tags are removed.
* `Add contributing equipment sequence`: Add tags indicating the software used for deidentification

#### Relationships:

* `success`: All deidentified DICOM images will be routed as FlowFiles to this relationship
* `failure`: FlowFiles that are not DICOM images

#### FlowFile attributes:

* **N/A**: does not set attributes

