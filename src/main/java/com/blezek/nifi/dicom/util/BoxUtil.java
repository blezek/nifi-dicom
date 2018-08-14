package com.blezek.nifi.dicom.util;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxUser;
import com.box.sdk.BoxUser.Info;
import com.box.sdk.CreateUserParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxUtil {
  private static final Logger logger = LoggerFactory.getLogger(BoxUtil.class);

  public static BoxUser createBoxUser(BoxAPIConnection api, String userId) {
    CreateUserParams params = new CreateUserParams();
    params.setExternalAppUserId(userId);
    BoxUser.Info info = BoxUser.createAppUser(api, userId, params);

    logger.info("Created a user: " + info);
    BoxUser boxUser = new BoxUser(api, info.getID());
    return boxUser;

  }

  public static BoxUser getOrCreateBoxUser(BoxAPIConnection api, String userId) {
    Iterable<Info> users = BoxUser.getAppUsersByExternalAppUserID(api, userId);
    for (Info user : users) {
      logger.info("Found a user: " + user);
      return new BoxUser(api, user.getID());
    }
    return createBoxUser(api, userId);
  }

  public static BoxFolder makeDirectories(BoxFolder folder, String path) {
    // make sub-directories and return the final directory
    BoxFolder f = folder;
    for (String d : path.split("/")) {
      if (d.isEmpty()) {
        continue;
      }
      f = getOrCreateFolder(f, d);
    }
    return f;
  }

  public static BoxFolder getOrCreateFolder(BoxFolder folder, String name) {
    // If the folder exists, return it, otherwise create
    for (BoxItem.Info item : folder.getChildren("name")) {
      if (item.getName().equals(name)) {
        if (item instanceof BoxFile.Info) {
          throw new IllegalArgumentException(
              "item " + name + " in " + folder.getInfo("name").getName() + " is a file, not a folder");
        }
        BoxFolder.Info folderInfo = (BoxFolder.Info) item;
        return new BoxFolder(folder.getAPI(), folderInfo.getID());
      }
    }
    // Create a new folder
    com.box.sdk.BoxFolder.Info info = folder.createFolder(name);
    return new BoxFolder(folder.getAPI(), info.getID());
  }

}
