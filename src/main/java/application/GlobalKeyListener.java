package application;

import java.awt.MouseInfo;
import java.awt.Point;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class GlobalKeyListener implements NativeKeyListener {
	
	public String type;
	
	public void nativeKeyPressed(NativeKeyEvent e) {
		System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

		if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            try {
            	GlobalScreen.removeNativeKeyListener(this);
				GlobalScreen.unregisterNativeHook();
				Main.keyListener = null;
			} catch (NativeHookException e1) {
				e1.printStackTrace();
			}
            Main.isListening = false;
            Point currentPosition = MouseInfo.getPointerInfo().getLocation();
            if (type.equals("Import")) {
            	Main.updateImportPosition(currentPosition.x, currentPosition.y);
            }
            else if (type.equals("Error")) {
            	Main.updateErrorPosition(currentPosition.x, currentPosition.y);
            }
            Main.dataUpdate = true;
        }
	}

	public void nativeKeyReleased(NativeKeyEvent e) {
		//System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}

	public void nativeKeyTyped(NativeKeyEvent e) {
		//System.out.println("Key Typed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}
}