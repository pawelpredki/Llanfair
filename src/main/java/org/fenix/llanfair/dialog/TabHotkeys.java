package org.fenix.llanfair.dialog;

import org.fenix.llanfair.Language;
import org.fenix.llanfair.config.Settings;
import org.fenix.utils.gui.GBC;
import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author  Xavier "Xunkar" Sencert
 */
class TabHotkeys extends SettingsTab {

	// ------------------------------------------------------------- ATTRIBUTES

	/**
	 * List of all key fields customizable by the user.
	 */
	private List<KeyField> keyFields;

	/**
	 * List of labels displaying the name of each key field.
	 */
	private List<JLabel> keyLabels;

	// ----------------------------------------------------------- CONSTRUCTORS

	/**
	 * Creates the "Hotkeys" settings tab. Only called by {@link EditSettings}.
	 */
	TabHotkeys() {
		keyFields = new ArrayList<KeyField>();
		keyLabels = new ArrayList<JLabel>();

		int index = 0;
		for (Settings.Property<?> setting : Settings.getAll("hotkey")) {
			keyFields.add(new KeyField(
					index, (Settings.Property<Integer>) setting)
			);
			keyLabels.add(new JLabel("" + setting));
			index++;
		}

		place();
	}

	// -------------------------------------------------------------- INHERITED

	@Override void doDelayedSettingChange() {}

	/**
	 * Returns the localized name of this tab.
	 */
	@Override public String toString() {
		return "" + Language.INPUTS;
	}

	// -------------------------------------------------------------- UTILITIES

	/**
	 * Places all sub-components within this panel.
	 */
	private void place() {
		setLayout(new GridBagLayout());

		for (int row = 0; row < keyFields.size(); row++) {
			add(
					keyLabels.get(row),
					GBC.grid(0, row).insets(10, 0, 10, 10).anchor(GBC.LE)
			);
			add(keyFields.get(row), GBC.grid(1, row));
		}
	}

	// --------------------------------------------------------- INTERNAL TYPES

	/**
	 * A text field representing a hotkey setting. Clicking such field allows
	 * the user to define a key for this particular setting, using the tables
	 * from {@code JNativeHook}.
	 *
	 * @author  Xavier "Xunkar" Sencert
	 */
	static class KeyField extends JTextField
			implements MouseListener, NativeKeyListener {

		// ----------------------------------------------------- CONSTANTS

		/**
		 * Preferred dimension of a text field.
		 */
		private static final Dimension SIZE = new Dimension(85, 20);

		// ---------------------------------------------------- ATTRIBUTES

		private static boolean isEditing = false;

		/**
		 * Internal index of this field, used by the superclass.
		 */
		private int index;

		/**
		 * Setting represented by this key field.
		 */
		private Settings.Property<Integer> setting;

		// -------------------------------------------------- CONSTRUCTORS

		/**
		 * Creates a new key field for the given setting. Only called by
		 * {@link TabHotkeys}.
		 *
		 * @param   index   - internal index to identify this field.
		 * @param   setting - setting represented by this field.
		 */
		KeyField(int index, Settings.Property<Integer> setting) {
			this.index   = index;
			this.setting = setting;

			setEditable(false);
			setName(setting.getKey());
			setPreferredSize(SIZE);
			setHorizontalAlignment(CENTER);
			String text = NativeKeyEvent.getKeyText(setting.get());
			setText(setting.get() == -1 ? "" + Language.DISABLED : text);

			addMouseListener(this);
		}

		// ------------------------------------------------------- GETTERS

		/**
		 * Returns the setting represented by this key field.
		 */
		public Settings.Property<Integer> getSetting() {
			return setting;
		}

		// ----------------------------------------------------- CALLBACKS

		/**
		 * When a key field is clicked, we register ourselves as native key
		 * listener to capture the new key setting. The background adorns a
		 * new color to signify that this field is now listening.
		 */
		public void mouseClicked(MouseEvent event) {
			if (!isEditing) {
				setBackground(Color.YELLOW);
				GlobalScreen.getInstance().addNativeKeyListener(this);
				isEditing = true;
			}
		}

		// $UNUSED$
		public void mouseEntered(MouseEvent event) {}

		// $UNUSED$
		public void mouseExited(MouseEvent event) {}

		// $UNUSED$
		public void mousePressed(MouseEvent event) {}

		// $UNUSED$
		public void mouseReleased(MouseEvent event) {}

		/**
		 * When a key is pressed we set the captured key as the new value
		 * for the setting we represent. If escape is pressed, the hotkey
		 * becomes disabled. After registering the update, we no longer
		 * listen for native key events.
		 */
		public void nativeKeyPressed(NativeKeyEvent event) {
			int    code = event.getKeyCode();
			String text = null;

			if (code == NativeKeyEvent.VK_ESCAPE) {
				code = -1;
				text = "" + Language.DISABLED;
			} else {
				text = NativeKeyEvent.getKeyText(code);
			}
			setText(text);
			setting.set(code);

			setBackground(Color.GREEN);
			GlobalScreen.getInstance().removeNativeKeyListener(this);
			isEditing = false;
		}

		// $UNUSED$
		public void nativeKeyReleased(NativeKeyEvent event) {}

		// $UNUSED$
		public void nativeKeyTyped(NativeKeyEvent event) {}

	}
}
