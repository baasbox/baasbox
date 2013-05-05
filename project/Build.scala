import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "BaasBox"
    val appVersion      = "0.5.6-SNAPSHOT"

    val appDependencies = Seq(
    			javaCore,filters,
    			"commons-io" % "commons-io" % "2.4",
    			"commons-lang" % "commons-lang" % "2.6",
    			"xalan"  % "xalan"  % "2.7.1",
    			"org.imgscalr" % "imgscalr-lib" % "4.2"
    		//	,"com.wordnik" %% "swagger-play2" % "1.2.1-SNAPSHOT",
    		//	"com.wordnik" %% "swagger-play2-utils" % "1.2.1-SNAPSHOT"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers := Seq(
          "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases",
          "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    ),requireJs += "main.js")

}
