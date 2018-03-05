package com.pixelmed.utils;

import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;

/**
 * <p>JTree with additional key stroke actions.</p>
 *
 * <p>The additional actions are to expand all (alt-right arrow or asterisk) or collapse all (alt-left arrow or minus) nodes.</p>
 *
 * @author	dclunie
 */
public class JTreeWithAdditionalKeyStrokeActions extends JTree {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/utils/JTreeWithAdditionalKeyStrokeActions.java,v 1.3 2003/10/09 21:38:37 dclunie Exp $";
	
	private static final String   expandAllActionName = "expandAll";
	private static final String collapseAllActionName = "collapseAll";
	
	protected void addAdditionalKeyStrokeActions() {
//System.err.println("JTreeWithAdditionalKeyStrokeActions.addAdditionalKeyStrokeActions():");
		// For explanation, see "http://java.sun.com/products/jfc/tsc/special_report/kestrel/keybindings.html".

		Action expandAllAction = new AbstractAction(expandAllActionName) {
			public void actionPerformed(java.awt.event.ActionEvent e) {
//System.err.println("JTreeWithAdditionalKeyStrokeActions.expandAllAction.actionPerformed():");
				Object source = e.getSource();
//System.err.println("JTreeWithAdditionalKeyStrokeActions.expandAllAction.actionPerformed(): source = "+source);
//System.err.println("JTreeWithAdditionalKeyStrokeActions.expandAllAction.actionPerformed(): source class name = "+source.getClass().getName());
				JTreeWithAdditionalKeyStrokeActions tree = (JTreeWithAdditionalKeyStrokeActions)source;
				expandPathCompletely(tree,tree.getSelectionPath(),true);
			}
		};
		getActionMap().put(expandAllAction.getValue(Action.NAME),expandAllAction);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT,InputEvent.ALT_MASK),expandAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,InputEvent.ALT_MASK),expandAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ASTERISK,0),expandAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ASTERISK,InputEvent.SHIFT_MASK),expandAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY,0),expandAllActionName);

		Action collapseAllAction = new AbstractAction(collapseAllActionName) {
			public void actionPerformed(java.awt.event.ActionEvent e) {
//System.err.println("JTreeWithAdditionalKeyStrokeActions.collapseAllAction.actionPerformed():");
				Object source = e.getSource();
//System.err.println("JTreeWithAdditionalKeyStrokeActions.collapseAllAction.actionPerformed(): source = "+source);
//System.err.println("JTreeWithAdditionalKeyStrokeActions.collapseAllAction.actionPerformed(): source class name = "+source.getClass().getName());
				JTreeWithAdditionalKeyStrokeActions tree = (JTreeWithAdditionalKeyStrokeActions)source;
				expandPathCompletely(tree,tree.getSelectionPath(),false);
			}
		};
		getActionMap().put(collapseAllAction.getValue(Action.NAME),collapseAllAction);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT,InputEvent.ALT_MASK),collapseAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,InputEvent.ALT_MASK),collapseAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,0),collapseAllActionName);
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT,0),collapseAllActionName);
	}
	
	// Derived from "http://javaalmanac.com/egs/javax.swing.tree/ExpandAll.html".
	private void expandPathCompletely(JTree tree, TreePath parent, boolean expand) {
		if (tree != null && parent != null) {
			// Depth-first traversal of children before expanding
			TreeNode node = (TreeNode)parent.getLastPathComponent();
			if (!node.isLeaf()) {
				for (Enumeration e=node.children(); e.hasMoreElements(); ) {
					TreeNode n = (TreeNode)e.nextElement();
					TreePath path = parent.pathByAddingChild(n);
					expandPathCompletely(tree, path, expand);
				}
			}
			// Expansion or collapse must be done bottom-up, so do it after children processed
			if (expand) {
				tree.expandPath(parent);
			}
			else {
				tree.collapsePath(parent);
			}
		}
	}

	
	public JTreeWithAdditionalKeyStrokeActions() {
		super();
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(Object[] value) {
		super(value);
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(Vector value) {
		super(value);
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(Hashtable value) {
		super(value);
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(TreeNode root) {
		super(root);
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(TreeNode root,boolean asksAllowsChildren) {
		super(root,asksAllowsChildren);
		addAdditionalKeyStrokeActions();
	}

	public JTreeWithAdditionalKeyStrokeActions(TreeModel newModel) {
		super(newModel);
		addAdditionalKeyStrokeActions();
	}
}