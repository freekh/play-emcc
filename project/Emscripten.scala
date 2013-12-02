import sbt._
import sbt.Keys._
import java.io.File
import play.Keys._
import play.PlayExceptions._
import play.PlaySourceGenerators

case class EmscriptenCompilerException(filename: String, lineNo: Option[Int], colNo: Option[Int], msg: String) extends Exception

class EmscriptenCompiler(eccCompiler: String)  {
  private val compilerString = eccCompiler + " "

  val ErrorLine = """(.*?):(\d+):(\d+): error: (.*?)""".r


  def compile(file: File, outputDir: File): Option[EmscriptenCompilerException] = {
    var result: Option[EmscriptenCompilerException] = None
    val logger = sys.process.ProcessLogger(
      (o: String) => None,
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

object EmscriptenKeys {
  val eccFiles = taskKey[PathFinder]("play-ecc-entry-points")
  val eccCompiler = settingKey[String]("play-ecc-compiler")
}

object EmscriptenSettings {
  import EmscriptenKeys._
  
  val settings = Seq(
    eccCompiler := "emcc",
    eccFiles <<= (sourceDirectory in Compile) map ( _   ** "*.cpp" ),
    resourceGenerators in Compile <+= (eccFiles, eccCompiler, resourceManaged, cacheDirectory, state) map { (files, eccCompilerString, resourceManaged, cacheDirectory, state) =>
      val emscriptenCompiler = new EmscriptenCompiler(eccCompilerString)
      val results = files.get.flatMap{ f =>
        val name = f.getName.replaceAll("""\..*?$""", "")
        val cacheFile = cacheDirectory / name

        val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)
        val currentInfos = Map(f -> FileInfo.lastModified(f))

        if (previousInfo != currentInfos) { //found new files
          val outputDir = resourceManaged / "main" / "public" / name

          IO.delete( (outputDir ** (name + ".*")).get) //delete current files
          emscriptenCompiler.compile(f, outputDir) match {
            case Some(EmscriptenCompilerException(filename, lineNo, colNo, msg)) => 
              throw PlaySourceGenerators.reportCompilationError(state,
                AssetCompilationException(Some(file(filename)), msg, lineNo, colNo))
            case _ => 
          }

          val outputFiles = (outputDir ** (name + ".*")).get
          val relationship = Relation.empty[File, File] ++ outputFiles.map(generatedFile => f -> generatedFile)
          Sync.writeInfo(cacheFile, relationship, currentInfos)(FileInfo.lastModified.format)
          outputFiles
        } else {
          previousRelation._2s.toSeq
        }
      }
      results
    }
  )
}


object EmscriptenPlugin extends Plugin {
  import EmscriptenKeys._

  override lazy val projectSettings: Seq[Setting[_]] = EmscriptenSettings.settings

}
