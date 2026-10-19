package thaumcraft.fabric.clienttest;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFW;

/** TEST-ONLY OS input for the owned desktop runner. Minecraft sets AWT headless;
 * SendInput still passes through Win32/GLFW/native Screen.hasShiftDown. This does
 * not override a game boolean, tooltip callback, player flag, or server action. */
final class FocalNativeShift {
    private boolean down;
    private long lastFocusRequest;

    boolean focusOwnedWindow(Minecraft client) {
        requireWindows();
        long owned=GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow());
        WinDef.HWND target=new WinDef.HWND(new Pointer(owned));
        IntByReference pid=new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(target,pid);
        if(owned==0 || Integer.toUnsignedLong(pid.getValue())!=ProcessHandle.current().pid())
            throw new IllegalStateException("GLFW HWND does not belong to this test JVM: "+describe(client));
        if(isForeground(client,owned))return true;
        long now=System.nanoTime();
        if(now-lastFocusRequest>500_000_000L) {
            lastFocusRequest=now;
            // User explicitly requested automatic activation ("Давай сам").
            // Touch only our verified HWND; no keys go to a foreign window.
            GLFW.glfwFocusWindow(client.getWindow().getWindow());
            User32.INSTANCE.ShowWindow(target,WinUser.SW_RESTORE);
            User32.INSTANCE.SetForegroundWindow(target);
            WinDef.HWND foreground=User32.INSTANCE.GetForegroundWindow();
            if(foreground!=null && Pointer.nativeValue(foreground.getPointer())!=owned) {
                WinDef.DWORD currentThread=new WinDef.DWORD(Kernel32.INSTANCE.GetCurrentThreadId());
                WinDef.DWORD foregroundThread=new WinDef.DWORD(User32.INSTANCE.GetWindowThreadProcessId(foreground,null));
                // A bounded ordinary Win32 input-queue attachment is not a
                // process/privilege change. Always detach immediately; never
                // send modifiers while attached or alter the foreign window.
                boolean attached=currentThread.longValue()!=foregroundThread.longValue()
                        && foregroundThread.longValue()!=0
                        && User32.INSTANCE.AttachThreadInput(currentThread,foregroundThread,true);
                try {
                    if(attached) {
                        User32.INSTANCE.BringWindowToTop(target);
                        User32.INSTANCE.SetForegroundWindow(target);
                        User32.INSTANCE.SetFocus(target);
                    }
                } finally {
                    if(attached && !User32.INSTANCE.AttachThreadInput(currentThread,foregroundThread,false))
                        throw new IllegalStateException("Cannot detach owned native focus request: error="+Native.getLastError());
                }
            }
        }
        return false; // Wait for actual Win32/GLFW focus; never fake the flag.
    }

    String describe(Minecraft client) {
        long owned=GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow());
        WinDef.HWND foreground=User32.INSTANCE.GetForegroundWindow();
        return "ownedHWND="+Long.toUnsignedString(owned)+", foregroundHWND="
                +(foreground==null?"NULL":Long.toUnsignedString(Pointer.nativeValue(foreground.getPointer())))
                +", clientWindowActive="+client.isWindowActive()+", currentPID="+ProcessHandle.current().pid();
    }

    private static boolean isForeground(Minecraft client,long owned) {
        WinDef.HWND foreground=User32.INSTANCE.GetForegroundWindow();
        return client.isWindowActive() && foreground!=null && Pointer.nativeValue(foreground.getPointer())==owned;
    }
    private static void requireWindows() {
        if(Util.getPlatform()!=Util.OS.WINDOWS)throw new IllegalStateException("Focal native Shift requires the owned Windows runner");
    }

    void setHeld(Minecraft client, boolean held) {
        if (held == down) return;
        requireWindows();
        if (held) {
            long owned = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow());
            WinDef.HWND foreground = User32.INSTANCE.GetForegroundWindow();
            if (!client.isWindowActive() || owned == 0 || foreground == null
                    || Pointer.nativeValue(foreground.getPointer()) != owned) {
                throw new IllegalStateException("Refuse native Shift unless this exact Minecraft HWND is foreground: "+describe(client));
            }
        }
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(WinUser.KEYBDINPUT.class);
        input.input.ki = new WinUser.KEYBDINPUT();
        input.input.ki.wVk = new WinDef.WORD(0); // Ignored with KEYEVENTF_SCANCODE.
        input.input.ki.wScan = new WinDef.WORD(0x2A); // Physical LEFT_SHIFT make code.
        input.input.ki.dwFlags = new WinDef.DWORD(8 | (held ? 0 : 2)); // SCANCODE | paired KEYUP.
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        input.write();
        int sent = User32.INSTANCE.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[]{input}, input.size()).intValue();
        if (sent != 1) throw new IllegalStateException("Native Shift SendInput refused: sent=" + sent + ", error=" + Native.getLastError());
        // A paired key-up must also run during failure/focus-loss cleanup so the
        // modifier this test pressed cannot remain stuck on the user's desktop.
        down = held;
    }
}
