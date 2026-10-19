package thaumcraft.fabric.clienttest;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Read-only native ownership/focus facts; never overrides GLFW/game flags. */
final class BoreOwnedForeground {
    private BoreOwnedForeground() {}
    static boolean isOwned(Minecraft client) {
        if(Util.getPlatform()!=Util.OS.WINDOWS)throw new IllegalStateException("Owned native Bore runner requires Windows");
        long handle=GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow());
        var window=new WinDef.HWND(new Pointer(handle));var pid=new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window,pid);
        if(handle==0 || Integer.toUnsignedLong(pid.getValue())!=ProcessHandle.current().pid())
            throw new IllegalStateException("Refuse non-owned Minecraft HWND");
        WinDef.HWND foreground=User32.INSTANCE.GetForegroundWindow();
        return client.isWindowActive() && foreground!=null && Pointer.nativeValue(foreground.getPointer())==handle;
    }
}
