package com.blezek.nifi.dicom.util;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.PasswordRecipient;
import org.bouncycastle.cms.PasswordRecipientId;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JcePasswordEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JcePasswordRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Security;

public class Encryption {
  static {
    // Add Bouncy Castle Fips provider using the JAR from
    // https://www.bouncycastle.org/fips-java/
    // Security.addProvider(new BouncyCastleFipsProvider());
    Security.addProvider(new BouncyCastleProvider());

  }

  public static String hash(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return (new HexBinaryAdapter()).marshal(md.digest(s.getBytes()));
    } catch (Exception e) {
      return "1234";
    }
  }

  public static byte[] createPasswordEnvelopedObject(char[] passwd, byte[] salt, int iterationCount, byte[] data)
      throws GeneralSecurityException, CMSException, IOException {

    CMSEnvelopedDataGenerator envelopedGen = new CMSEnvelopedDataGenerator();
    envelopedGen.addRecipientInfoGenerator(new JcePasswordRecipientInfoGenerator(CMSAlgorithm.AES256_CBC, passwd)
        // .setProvider("BCFIPS")
        .setPasswordConversionScheme(PasswordRecipient.PKCS5_SCHEME2_UTF8)

        // .setPRF(PasswordRecipient.PRF.HMacSHA384)
        .setSaltAndIterationCount(salt, iterationCount));
    return envelopedGen.generate(new CMSProcessableByteArray(data),
        new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC)
            // .setProvider("BCFIPS")
            .build())
        .getEncoded();
  }

  /*
   * isPasswordRecipient determines if the envelopedData contains a password
   * recipient
   */
  public static boolean isPasswordRecipient(byte[] encEnvelopedData) throws CMSException {
    CMSEnvelopedData envelopedData = new CMSEnvelopedData(encEnvelopedData);
    RecipientInformationStore recipients = envelopedData.getRecipientInfos();
    RecipientId rid = new PasswordRecipientId();
    return (recipients.get(rid) != null);
  }

  public static byte[] extractPasswordEnvelopedData(char[] passwd, byte[] encEnvelopedData)
      throws GeneralSecurityException, CMSException {

    CMSEnvelopedData envelopedData = new CMSEnvelopedData(encEnvelopedData);
    RecipientInformationStore recipients = envelopedData.getRecipientInfos();
    RecipientId rid = new PasswordRecipientId();
    RecipientInformation recipient = recipients.get(rid);
    return recipient.getContent(new JcePasswordEnvelopedRecipient(passwd)
        // .setProvider("BCFIPS")
        .setPasswordConversionScheme(PasswordRecipient.PKCS5_SCHEME2_UTF8));
  }

}
