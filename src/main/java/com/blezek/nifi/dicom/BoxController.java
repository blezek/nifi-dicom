package com.blezek.nifi.dicom;

import com.blezek.nifi.dicom.util.BoxUtil;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxConfig;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.BoxUser;
import com.box.sdk.BoxUser.Info;
import com.google.common.collect.ImmutableList;

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.FileReader;
import java.io.Reader;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Tags({ "box", "upload", "cloud" })
@CapabilityDescription("This controller provides access to Box's API services.  See https://developer.box.com/reference and http://opensource.box.com/box-java-sdk/javadoc/com/box/sdk/package-summary.html")
@SeeAlso()
public class BoxController extends AbstractControllerService implements BoxAPIService {

  // Properties
  static final PropertyDescriptor UserID = new PropertyDescriptor.Builder().name("USERID")
      .displayName("Box Managed User ID")
      .description("User ID of the managed user.  The BoxAPIConnection will be configured to operate as the user ID")
      .required(true).expressionLanguageSupported(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  static final PropertyDescriptor UserSpace = new PropertyDescriptor.Builder().name("USERSPACE")
      .displayName("User space in Mb").description("Space allocated to the user in megabytes (1024*1024 bytes)")
      .required(true).defaultValue("10").addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR).build();

  static final PropertyDescriptor DeveloperToken = new PropertyDescriptor.Builder().name("DEVELOPERTOKEN")
      .displayName("Developer Token")
      .description(
          "Timelimited Developer Token from Box API console.  Generally useful for testing only, as it's only valid for 1 hour.")
      .required(false).expressionLanguageSupported(true).addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

  static final PropertyDescriptor AppSettings = new PropertyDescriptor.Builder().name("APPSETTINGS")
      .displayName("App Settings File")
      .description(
          "A JSON file containing connection information for Box.  This is generated from the Box Developer API console.  Used for production settings.")
      .required(false).expressionLanguageSupported(true).addValidator(StandardValidators.FILE_EXISTS_VALIDATOR).build();

  Optional<BoxAPIConnection> api = Optional.empty();

  @Override
  public BoxAPIConnection getConnection() {
    return api.get();
  }

  @OnEnabled
  public void enabled(ConfigurationContext context) throws Exception {
    // Try to make the API connection
    // Dev token path
    if (context.getProperty(DeveloperToken).isSet()) {
      String token = context.getProperty(DeveloperToken).evaluateAttributeExpressions().getValue();
      token = token.trim();
      api = Optional.of(new BoxAPIConnection(token));
    }
    if (context.getProperty(AppSettings).isSet()) {
      String filename = context.getProperty(AppSettings).evaluateAttributeExpressions().getValue();
      try (Reader reader = new FileReader(filename)) {
        // Initialize the SDK with the Box configuration file and create a client
        // that uses the Service Account.
        BoxConfig boxConfig = BoxConfig.readFrom(reader);
        api = Optional.of(BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(boxConfig));
      }
    }
    if (!api.isPresent()) {
      throw new InvalidParameterException("Could not get Box API");
    }
    // Create the user
    String userId = context.getProperty(UserID).evaluateAttributeExpressions().getValue();
    BoxUser user = BoxUtil.getOrCreateBoxUser(api.get(), userId);
    getLogger().info("Operating as Box user: " + user);

    Integer space = context.getProperty(UserSpace).evaluateAttributeExpressions().asInteger();
    Info info = user.new Info();
    info.setSpaceAmount(space * 1024 * 1024);
    user.updateInfo(info);

    api.get().asUser(user.getID());
  }

  @Override
  protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
    // descriptors
    return ImmutableList.of(UserID, UserSpace, DeveloperToken, AppSettings);
  }

  @Override
  protected Collection<ValidationResult> customValidate(final ValidationContext validationContext) {
    HashSet<ValidationResult> results = new HashSet<>();
    // Make sure Token or AppSettings are set
    PropertyValue token = validationContext.getProperty(DeveloperToken);
    PropertyValue settings = validationContext.getProperty(AppSettings);
    if (!token.isSet() && !settings.isSet()) {
      ValidationResult r = new ValidationResult.Builder().valid(false)
          .explanation("One of Developer Token or APISettings must be configured").build();
      results.add(r);
    }
    if (token.isSet() && settings.isSet()) {
      ValidationResult r = new ValidationResult.Builder().valid(false)
          .explanation("Both Developer Token and APISettings are configured, please choose only one").build();
      results.add(r);
    }
    return Collections.emptySet();
  }

}
