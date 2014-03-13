import sbt._
import Keys._
import play.Project._
import com.typesafe.config._
import com.typesafe.sbteclipse.core._

object ApplicationBuild extends Build {
	override def settings = super.settings ++ Seq(
      EclipsePlugin.EclipseKeys.preTasks := Seq()
    )
  
	val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
    
	val appName         = "BaasBox"
    val appVersion      = conf.getString("api.version")

    val appDependencies = Seq(
    			javaCore,filters,cache,
    			"commons-io" % "commons-io" % "2.4",
    			"commons-lang" % "commons-lang" % "2.6",
    			"commons-collections" % "commons-collections" % "3.2",
    			"xalan"  % "xalan"  % "2.7.1",
    			"org.imgscalr" % "imgscalr-lib" % "4.2",
    			"org.apache.commons" % "commons-email" % "1.3.1",
    			"org.antlr" % "stringtemplate" % "4.0.2",
                "com.github.tony19" % "named-regexp" % "0.2.3",
                "org.scribe" % "scribe" % "1.3.2",
				"com.eaio.uuid" % "uuid" % "3.4",
				"org.apache.tika" % "tika-core" % "1.4",
				"org.apache.tika" % "tika-parsers" % "1.4",
                  "com.codahale.metrics" % "metrics-json" % "3.0.1",
    "com.codahale.metrics" % "metrics-annotation" % "3.0.1"
    		//	,"com.wordnik" %% "swagger-play2" % "1.2.1-SNAPSHOT",
    		//	"com.wordnik" %% "swagger-play2-utils" % "1.2.1-SNAPSHOT",

    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
       sources in doc in Compile := List(),
	resolvers := Seq(
          "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases",
          "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
		  "eaio" at "http://eaio.com/maven2"
	    )
	  ,requireJs += "main.js"
	  ,Keys.fork in (Test) := false
    )

}
