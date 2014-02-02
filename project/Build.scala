import sbt._
import Keys._
import cloudbees.Plugin._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "AdvancedPublicTransportTimetables"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      javaCore,
      // Add your project dependencies here,
      //see latest version under: http://mvnrepository.com/artifact/com.google.code.gson/gson
      "com.google.code.gson" % "gson" % "2.2.4",
      "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2"
    )

    val main = play.Project(appName, appVersion, appDependencies)
        .settings(cloudBeesSettings :_*)
        .settings(CloudBees.applicationId := Some("aptt"))

}
