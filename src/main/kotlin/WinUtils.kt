import com.sun.jna.platform.DesktopWindow
import com.sun.jna.platform.WindowUtils
import com.sun.jna.platform.win32.User32
import java.awt.Rectangle

object WinUtils {

  private val user32 = User32.INSTANCE

  fun detectWindow(windowName: String): DesktopWindow? {
    val rect = Rectangle(0, 0, 0, 0)

    val windows = WindowUtils.getAllWindows(true)
    windows.forEach {
      if (it.title.contains(windowName)) {
        rect.setRect(it.locAndSize)
        return it
      }
    }

    return null
  }

  fun bringUnminimizedWindowToFront(window: DesktopWindow) {
    val hWnd = window.hwnd
    if (user32.IsWindowVisible(hWnd)) {
      user32.ShowWindow(hWnd, User32.SW_SHOWMINIMIZED)
    }
    user32.ShowWindow(hWnd, User32.SW_SHOWDEFAULT)
    user32.SetFocus(hWnd)
    user32.SetForegroundWindow(window.hwnd)
  }
}