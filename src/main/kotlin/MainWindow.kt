import WinUtils.bringUnminimizedWindowToFront
import java.awt.EventQueue
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

class MainWindow : JFrame("PS Automator") {

  private val automatorThread: Thread

  private val automatorEnabled = AtomicBoolean(false)

  private val robot = Robot()

  private val root = JPanel()

  init {
    setupFrame()

    automatorThread = Thread {
      automatorThreadRoutine()
    }
    automatorThread.isDaemon = true
    automatorThread.start()

    addButtons()
    add(root)
  }

  private fun setupFrame() {
    setSize(500, 100)
    setLocationRelativeTo(null)
    isResizable = false
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  }

  private fun addButtons() {
    val automatorButton = JButton("Automator enabled: false")
    automatorButton.addActionListener {
      automatorEnabled.set(!automatorEnabled.get())
      val enabledVal = automatorEnabled.get()
      automatorButton.text = "Automator enabled: $enabledVal"
    }
    val processButton = JButton("Select folder with XMP")
    processButton.addActionListener {
      EventQueue.invokeLater {
        openFolderDialog()
      }
    }
    root.add(automatorButton)
    root.add(processButton)
  }

  private fun openFolderDialog() {
    val fileChooser = JFileChooser()
    val home = System.getProperty("user.home") + "\\Desktop"
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.currentDirectory = File(home)
    fileChooser.isAcceptAllFileFilterUsed = false
    val option = fileChooser.showOpenDialog(null)
    if (option == JFileChooser.APPROVE_OPTION) {
      val folder = fileChooser.selectedFile
      val w = FileProcessingWindow(folder)
      w.show()
      w.startProcess()
    }
  }

  private fun automatorThreadRoutine() {
    while (true) {
      if (automatorEnabled.get()) {
        tryPressEnterOnWindow("Camera Raw")
        tryPressEnterOnWindow("Несоответствие")
      }
      Thread.sleep(500)
    }
  }

  private fun tryPressEnterOnWindow(windowQuery: String) {
    val window = WinUtils.detectWindow(windowQuery)
    if (window != null) {
      bringUnminimizedWindowToFront(window)
      Thread.sleep(250)
      robot.keyPress(KeyEvent.VK_ENTER)
      Thread.sleep(20)
      robot.keyRelease(KeyEvent.VK_ENTER)
      Thread.sleep(500)
    }
  }
}