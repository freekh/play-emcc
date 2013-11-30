import sbt._
import sbt.Keys._
import java.io.File

object EmscriptenCompiler  {
  case class EmscriptenCompilerException(filename: String, lineNo: Int, colNo: Int, msg: String) extends Exception

  val ErrorLine = """(.*?):(\d+):(\d+): error: (.*?)""".r


  val emcc = "/Users/freekh/Downloads/emsdk_portable/emscripten/1.7.8/emcc "

  def compile(file: File, outputDir: File) = {
    val logger = sys.process.ProcessLogger(
      (o: String) => None,
      (e: String) => e match {
        case ErrorLine(filename, lineNo, colNo, msg) => throw EmscriptenCompilerException(filename, lineNo.toInt, colNo.toInt, msg)
      })
      val outputHtmlFile = new File(outputDir, file.getName.replaceAll("""\..*?$""", ".html"))
      sys.process.Process(emcc + file.getAbsolutePath + " -o " + outputHtmlFile) ! logger
  }

}

object EmscriptenKeys {
  val eccFiles = taskKey[PathFinder]("play-ecc-entry-points")
}

object EmscriptenSettings {
  import EmscriptenKeys._

  val settings = Seq(
    eccFiles <<= (sourceDirectory in Compile) map ( _   ** "*.cpp" ),
    resourceGenerators in Compile <+= (eccFiles, playAssetsDirectories) map { (files, outputDirs) =>
       println(files.get)
       Seq.empty[File]
    }
  )
}


object EmscriptenPlugin extends Plugin {
  import EmscriptenKeys._

  override lazy val projectSettings: Seq[Setting[_]] = EmscriptenSettings.settings

}
