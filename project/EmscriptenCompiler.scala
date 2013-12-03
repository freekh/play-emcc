import java.io.File

case class EmscriptenCompilerException(filename: String, lineNo: Option[Int], colNo: Option[Int], msg: String) extends Exception

class EmscriptenCompiler(eccCompiler: String)  {
  private val compilerString = eccCompiler + " "

  val ErrorLine = """(.*?):(\d+):(\d+): error: (.*?)""".r


  def compile(file: File, outputDir: File): Option[EmscriptenCompilerException] = {
    var result: Option[EmscriptenCompilerException] = None
    val logger = sys.process.ProcessLogger(
      (o: String) => result = None,
      (e: String) => e match {
        case ErrorLine(filename, lineNo, colNo, msg) => 
          if (result.isEmpty)
            result = Some( EmscriptenCompilerException(filename, Some(lineNo.toInt), Some(colNo.toInt), msg))
        case unknownError =>
          if (result.isEmpty)
            result = Some(EmscriptenCompilerException(file.getAbsolutePath, None, None, unknownError.toString))
      })
    val outputHtmlFile = new File(outputDir, file.getName.replaceAll("""\..*?$""", ".html"))
    if (!outputDir.isDirectory && !outputDir.mkdirs()) throw new Exception("catastrophic error: could not create dir: " + outputDir)

    sys.process.Process(compilerString + file.getAbsolutePath + " -o " + outputHtmlFile) ! logger

    result
  }

}

