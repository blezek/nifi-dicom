# Design of DICOM Supplement 55 Encription / Decryption

## Status

accepted

## Context

[DICOM Supplement 55 Attribute Level Confidentiality](http://dicom.nema.org/Dicom/supps/sup55_03.pdf) covers the proper procedure for deidentification of DICOM data, and potential later recovery of the original PHI.  The secret sauce is to encrypt a set of tags and embed into the deidentified DICOM.  Later, with the proper key or password, the data can be recovered.

## Decision

The [Bouncy Castle](https://www.bouncycastle.org/) FIPS toolkit shall be used to encrypt tags according to Supplement 55.  The only provided method shall be password.

Two Processors were created

### DeidentifyEncryptDICOM Processor

A [Nifi controller](https://nifi.apache.org/docs/nifi-docs/html/user-guide.html#Controller_Services) providing deidentification using the [PixelMedNet DICOM Cleaner](http://www.pixelmed.com/cleaner.html) application.  Removed or modified attributes are encrypted using the [Bouncy Castle FIPS](https://www.bouncycastle.org/fips_faq.html) code (distributed with the source code).

To keep `UID` remapping consistent, this processor needs to be associated with a `DeidentificationService`.

Relevant settings:

* `Password`: password used to encrypt, needed for decryption
* `Iterations`: number of iterations to use in encryption, more is better for security but costs CPU cycles

### DecryptReidentifyDICOM Processor

Decrypts and reidentifies DICOM data.  Must use the same `password` as `DeidentifyEncryptDICOM` or the data will not be recoverable.  Has the option (`Accept new series`) to preserve the `SeriesInstanceUID` and `SOPInstanceUID` in the deidentified data.  This is mainly useful for analytics that create new series and instances.

Relevant settings:

* `Password`: password for decryption, must match the `DeidentifyEncryptDICOM` password
* `Accept new series`: if `true`, new series are allowed, otherwise they are rejected

## Consequences

N/A
