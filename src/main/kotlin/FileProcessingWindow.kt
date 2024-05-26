import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Taskbar
import java.awt.Taskbar.State
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import kotlin.system.exitProcess

class FileProcessingWindow(
  private val folder: File
) : JFrame("Window") {

  private val root = JPanel()

  private val logLabel = JLabel()

  private val logProgressbar = JProgressBar()

  private val filesToProcess = ArrayList<File>()

  private val fileIndex = AtomicInteger(0)

  private val processingThread = Thread { processingThreadRoutine() }

  private val taskbar = Taskbar.getTaskbar()

  init {
    check(folder.exists())
    check(folder.isDirectory)
    setupFrame()
    addElements()
    add(root)
  }

  fun startProcess() {
    val files = Files.walk(folder.toPath())
        .filter { it.toFile().isFile }
        .filter { it.toFile().name.lowercase().endsWith(".xmp") }
        .map { it.toFile() }
        .toList()
    if (files.isEmpty()) {
      JOptionPane.showMessageDialog(
          null,
          "No files to process in ${folder.absolutePath}",
          "Error",
          JOptionPane.ERROR_MESSAGE)
      exitProcess(1)
    }
    filesToProcess.addAll(files)
    logProgressbar.maximum = filesToProcess.size
    logProgressbar.value = 0
    processingThread.start()
  }

  private fun processFile(i: Int, file: File) {
    log(i, file.absolutePath)
    val xmp = XmpFileWrapper(file)
    xmp.read()

    val iso = xmp.iso()
    if (iso != null) {
      log(i, "ISO=$iso")
      xmp.applySharpness(40)
      xmp.applyNoiseReduction(10)
      xmp.applyColorNoiseReduction(20)

      when {
        iso >= 5000 -> {
          xmp.applySharpness(65)
          xmp.applyNoiseReduction(60)
          xmp.applyColorNoiseReduction(45)
        }

        iso >= 3200 -> {
          xmp.applySharpness(55)
          xmp.applyNoiseReduction(45)
          xmp.applyColorNoiseReduction(35)
        }

        iso >= 1600 -> {
          xmp.applyNoiseReduction(35)
          xmp.applyColorNoiseReduction(25)
        }

        iso >= 800 -> {
          xmp.applyNoiseReduction(30)
          xmp.applyColorNoiseReduction(20)
        }

        iso >= 400 -> {
          xmp.applyNoiseReduction(20)
          xmp.applyColorNoiseReduction(15)
        }
      }
    }

    val lens = xmp.lens()
    log(i, "Lens=$lens")
    if (lens == "24.0-70.0 mm f/2.8") {
      xmp.applySigmaLensProfile()
      log(i, "Lens Profile Applied")
    }
    xmp.write()
    log(i, "XMP updated")
  }

  private fun processingThreadRoutine() {
    while (fileIndex.get() < filesToProcess.size) {
      val i = fileIndex.get()
      val file = filesToProcess[i]
      processFile(i, file)
      fileIndex.incrementAndGet()
      Thread.sleep(20)
    }
    SwingUtilities.invokeLater {
      JOptionPane.showMessageDialog(
          null,
          "Done",
          "Info",
          JOptionPane.INFORMATION_MESSAGE)
      // exitProcess(0)
    }
  }

  private fun log(i: Int, s: String) {
    val prefix = "${i + 1}/${filesToProcess.size}"
    this.title = prefix
    val line = "$prefix $s"
    logLabel.text = line
    logProgressbar.value = i
    println(line)
    SwingUtilities.invokeLater {
      this.contentPane.repaint()
    }

    taskbar.setWindowProgressState(this, State.NORMAL)
    val value = (i * 100.0 / filesToProcess.size).toInt()
    taskbar.setWindowProgressValue(this, value)
  }

  private fun setupFrame() {
    setSize(500, 400)
    setLocationRelativeTo(null)
    isResizable = false
    // defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
  }

  private fun addElements() {
    root.layout = GridBagLayout()
    val gbc = GridBagConstraints()
    gbc.gridwidth = GridBagConstraints.REMAINDER
    gbc.fill = GridBagConstraints.HORIZONTAL
    logLabel.text = "Example log"
    logProgressbar.maximum = 100
    logProgressbar.minimum = 0
    logProgressbar.value = 50
    root.add(logLabel, gbc)
    root.add(logProgressbar, gbc)
  }
}