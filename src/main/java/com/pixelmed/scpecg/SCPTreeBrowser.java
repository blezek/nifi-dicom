/* Copyright (c) 2001-2017, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

//import java.awt.*;
//import java.awt.event.*;
import javax.swing.JScrollPane;
import javax.swing.JTree;
//import javax.swing.tree.*;
//import javax.swing.event.*;
//import java.util.*;
//import java.io.*;

import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

/**
 * <p>The {@link com.pixelmed.scpecg.SCPTreeBrowser SCPTreeBrowser} class implements a Swing graphical user interface
 * to browse the contents of an {@link com.pixelmed.scpecg.SCPTree SCPTree}.</p>
 *
 * @author	dclunie
 */
public class SCPTreeBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/scpecg/SCPTreeBrowser.java,v 1.5 2017/01/24 10:50:47 dclunie Exp $";

	private JTree tree;
	private SCPTree treeModel;

	/**
	 * <p>Build and display a graphical user interface view of an SCP-ECG instance.</p>
	 *
	 * <p>Implicitly builds a tree from the SCP-ECG instance.</p>
	 *
	 * @param	scpecg				tan SCP-ECG instance
	 * @param	treeBrowserScrollPane		the scrolling pane in which the tree view of the attributes will be rendered
	 * @throws	Exception
	 */
	public SCPTreeBrowser(SCPECG scpecg,JScrollPane treeBrowserScrollPane) throws Exception {
		treeModel=new SCPTree(scpecg);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
	}
}






