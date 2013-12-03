## PLAY, SBT AND ITS COMPILERS



## Today we will create...



## Play Emscripten Compiler
(Compile C++/C to JavaScript!)



## Demo
https://gist.github.com/freekh/



## What we want
- On reload:
    - For new or *changed* input files:
       1. Compile
       2. Throw exception if failed
       3. Make sure Play can pick up the output files



## Steps 2 - 4
### Just some Scala code:
    object SomeCompiler {
      def compile(input: File, outputDir: File, state: State) = {
    //Step 2:    
        privateCompile(input) match {
          case exception: AssetsCompilerException => 
              throw PlaySourceGenerators.reportCompilationError(state, exception)
    //Step 3:                            ^
          case outputFiles => 
              move(outputFiles, outputDir)
    //Step 4: ^
        }
      }
    }



    
## Step 1...
## (sbt)



## Sbt overview
- Setting: Immutable values
- Tasks: Pure functions
- Settings/tasks can depend on other settings/tasks
- Commands: Mutating/state changing, executed by user
- Configurations: Apply a task/setting for a configuration



## Defining settings and tasks
    val testSettingKey = settingKey[File]("a test setting")
    val testTaskKey = taskKey[String]("a test task")



## Assignment
- Assignment ':=' 
      testSettingKey := file("foo")
- Dependent assignment  '<<=' 
      //testSettingKey depends on sourceDirectory in Compile         
      testSettingKey <<= 
          sourceDirectory in Compile map { dir => dir / "foo" }
- .value macro (0.13):
     //depends on sourceDirectory:                   
     testSettingKey := 
       (sourceDirectory in Compile).value: File



## Appending
- Append '+='
      //append foo to scalac options:                                 
      scalacOptions += "-foo"
- Dependent append '<+='
      sourceDirectories <+= 
          mySourceDirTask map { dir => dir + "/foo" }



## Architecture
- *Keys files contains definitions
- *Settings/Defaults files contains default settings
- *Plugin initializes the default settings



## Emscripten 001



## Back to Play
- input: 'app/'
  - sourceDirectory in Compile
- static output: 'public/'
  - playAssetsDirectories
- resources: 'target/scala-2.10/resource_managed/
  - resourceManaged
- And the generators...



## Generators
- Declaration (resource):
      // sbt/main/Keys.scala (slightly changed)
      val resourceGenerators = 
          settingKey[Seq[Task[Seq[File]]]]("resource-generators", "List of tasks that generate resources.")

- Using LessCompiler
      // playframework/framework/src/sbt-plugin/src/main/scala/PlaySettings.scala
      resourceGenerators in Compile <+= LessCompiler
- sourceGenerators



## Emscripten 002



## Useful tools
- Globbing / PathFinder
      //PathFinder                                       
      val cppFiles: PathFinder = 
          file(".") ** ".cpp"
      //Files
      val files: Seq[File] = cppFiles.get
- IO: sbt/util/sbt/IO.scala



## Emscripten 003




## Sbt plugins
- Classes/jars available to the sbt project
- Specifically an object that extends Plugin
  - Automatically loaded



## Caching
- Cache dir: 'cacheDirectory'

    Sync.{read,write}Info




# Emscripten 004



# Pro tip:
- Step 1:
  - Start with 'project/*.scala' files
- Step 2:
  - Create a new 'normal' sbt plugin project
      //simple difference
      sbtPlugin := true
- Step 3 (example next page):
  - Link project in sample project
- Step 4:
  - Add scripted tests
- Step 5:
  - Publish
  - Add documentation on 'plugins.sbt'



# Step 3
    //file: project/project/Build.scala
    import sbt._
    import sbt.Keys._

    object AdeptBuild extends Build {

      override lazy val settings = super.settings ++ Seq(
      )

      lazy val adeptSbt = ProjectRef(file("../../.."), "adept-sbt")

      lazy val root = Project(id = "adept-sbt-sample-project",
        base = file("."),
        settings = Project.defaultSettings).dependsOn(adeptSbt)  
    }



## Thank you!
Deck and code:
<br>
https://github.com/freekh/play-emcc
<br>
<br>
freekh 'at' gmail.com
<br>
@ekhfre
