package ca.sqlpower.architect.swingui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.SwingUserSettings;
import ca.sqlpower.architect.undo.UndoManager;

public class RedoAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(UndoAction.class);

	private class ManagerListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			updateSettingsFromManager();
		}
	}

	private UndoManager manager;
	private ChangeListener managerListener = new ManagerListener();

	public RedoAction() {
		putValue(Action.SMALL_ICON, ASUtils.createIcon("redo_arrow",
				"Redo",
				ArchitectFrame.getMainInstance().getSwingUserSettings().getInt(SwingUserSettings.ICON_SIZE, ArchitectFrame.DEFAULT_ICON_SIZE)));
		putValue(Action.NAME,"Redo");
		putValue(AbstractAction.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		updateSettingsFromManager();
	}
	
	public void actionPerformed(ActionEvent evt ) {
        if (logger.isDebugEnabled()) {
            logger.debug(manager);
            int choice = JOptionPane.showConfirmDialog(null,
                    "Undo manager state dumped to logger." +
                    "\n\n" +
                    "Proceed with redo?");
            if (choice == JOptionPane.YES_OPTION) {
                manager.redo();
            }
        } else {
            manager.redo();
        }
	}
	
	/**
	 * Attaches this action to the given undo manager.
	 * 
	 * @param manager The manager to attach to, or <code>null</code> for no manager.
	 */
	public void setManager(UndoManager manager) {
		if (this.manager != null) {
			this.manager.removeChangeListener(managerListener);
		}
		
		this.manager = manager;
		
		if (this.manager != null) {
			this.manager.addChangeListener(managerListener);
		}
		updateSettingsFromManager();
	}

	private void updateSettingsFromManager() {
		if (manager == null) {
			putValue(SHORT_DESCRIPTION, "Can't Redo");
			setEnabled(false);
		} else {
			putValue(SHORT_DESCRIPTION, manager.getRedoPresentationName());
			setEnabled(manager.canRedo());
		}
	}
}
