// build.sbt
scalaVersion := "3.8.4" 

scalacOptions ++= Seq(
  "-Wunused:all",            // Enables all unused warnings (imports, privates, locals, params)
  "-Werror",                 // Fails the compilation if ANY warning is emitted
  "-Wconf:cat=deprecation:s" // Silence deprecation warnings (from third-party libs like ScalaFX)
)

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "2.0.0",
  "org.scalafx"          %% "scalafx"   % "26.0.0-R38"
)