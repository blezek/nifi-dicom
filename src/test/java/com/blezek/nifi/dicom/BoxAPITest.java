package com.blezek.nifi.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.blezek.nifi.dicom.util.BoxUtil;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxUser;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

@DisplayName("basic Box API tests")
class BoxAPITest {
  static final Logger logger = LoggerFactory.getLogger(BoxAPITest.class);

  static BoxAPIConnection api;
  static String userId = "testuser";
  static BoxUser user;

  @BeforeAll
  public static void connect() throws IOException {
    URL url = Resources.getResource("boxToken.txt");
    String token = Resources.toString(url, Charsets.UTF_8).trim();
    api = new BoxAPIConnection(token);
    user = BoxUtil.getOrCreateBoxUser(api, userId);
    BoxUser.Info info = user.new Info();
    info.setSpaceAmount(10 * 1024 * 1024);
    user.updateInfo(info);
    api.asUser(user.getID());
    deleteFiles();
  }

  @AfterEach
  void afterEach() {
    deleteFiles();
  }

  public static void deleteFiles() {
    BoxFolder folder = BoxFolder.getRootFolder(api);
    for (com.box.sdk.BoxItem.Info item : folder.getChildren()) {
      if (item instanceof BoxFile.Info) {
        BoxFile.Info fileInfo = (BoxFile.Info) item;
        BoxFile file = new BoxFile(api, fileInfo.getID());
        file.delete();
      }
      if (item instanceof BoxFolder.Info) {
        BoxFolder f = new BoxFolder(api, item.getID());
        f.delete(true);
      }
    }
  }

  @SuppressWarnings("unused")
  int countItems(BoxFolder folder) {
    int count = 0;
    for (BoxItem.Info itemInfo : folder) {
      count++;
    }
    return count;
  }

  @Test
  @DisplayName("when a user is created, the account is empty")
  void emptyAccount() throws IOException {
    // Create a new user
    BoxFolder folder = BoxFolder.getRootFolder(api);
    int count = countItems(folder);
    assertEquals(0, count, "no folders or files");
  }

  @Test
  @DisplayName("a folder can be created and deleted")
  void createDirectory() {
    BoxFolder folder = BoxFolder.getRootFolder(api);
    BoxFolder f = BoxUtil.getOrCreateFolder(folder, "deleteme");
    assertEquals(1, countItems(folder), "one folder");
    // Ignore second attempt to create a folder?
    f = BoxUtil.getOrCreateFolder(folder, "deleteme");
    assertEquals(1, countItems(folder), "one folder");
    folder = new BoxFolder(api, f.getID());
    folder.delete(true);
    assertEquals(0, countItems(BoxFolder.getRootFolder(api)), "empty folder");
  }

  @Test
  @DisplayName("a folder hierarchy can be created")
  void createDirectoryHierarchy() {
    BoxFolder folder = BoxFolder.getRootFolder(api);
    String path = "/top/second/third/item/";
    BoxFolder sub = BoxUtil.makeDirectories(folder, path);
    assertEquals(1, countItems(folder), "one folder");
    assertEquals(0, countItems(sub), "no folder folder");
    assertEquals("item", sub.getInfo("name").getName(), "folder name");
    sub = BoxUtil.getOrCreateFolder(folder, "top");
    sub.delete(true);
    assertEquals(0, countItems(folder), "after delete");
  }

}
