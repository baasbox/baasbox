import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "BaasBox"
    val appVersion      = "0.5.3-SNAPSHOT"

    val appDependencies = Seq(
        
    			"commons-io" % "commons-io" % "2.4",
    			"com.wordnik" %% "swagger-play2" % "1.2.0",
    			"com.wordnik" %% "swagger-play2-utils" % "1.2.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      resolvers := Seq(
          "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases"
    ))

}
