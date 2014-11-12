/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
import sbt._
import Keys._
import play.Project._
import com.typesafe.config._
import com.typesafe.sbteclipse.core._
import com.typesafe.sbt.packager.Keys.stage
import com.typesafe.sbt.SbtNativePackager.Universal
import java.io.File

object ApplicationBuild extends Build {
	override def settings = super.settings ++ Seq(
      EclipsePlugin.EclipseKeys.preTasks := Seq()
    )
  
	val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
	val appName         = "BaasBox"
  val appVersion      = conf.getString("api.version")

  // fixme move version configuration here to make it dinamic
  //val altConf =ConfigFactory.parseFile(new File("conf/application.conf")).withValue("api.version",ConfigValueFactory.fromAnyRef("0.8.0-SNAPSHOT-2"))

  //todo script->distfiles
  //todo move package to -> dist

  val appDependencies = Seq(
    			javaCore,filters,cache,
    			"commons-io" % "commons-io" % "2.4",
    			"commons-lang" % "commons-lang" % "2.6",
    			"commons-collections" % "commons-collections" % "3.2",
    			"xalan"  % "xalan"  % "2.7.1",
    			"org.imgscalr" % "imgscalr-lib" % "4.2",
    			"org.apache.commons" % "commons-email" % "1.3.1",
          "com.github.tony19" % "named-regexp" % "0.2.3",
          "org.scribe" % "scribe" % "1.3.2",
				  "com.eaio.uuid" % "uuid" % "3.4",
				  "org.apache.tika" % "tika-core" % "1.4",
				  "org.apache.tika" % "tika-parsers" % "1.4",
          "com.codahale.metrics" % "metrics-json" % "3.0.1",
          "com.codahale.metrics" % "metrics-annotation" % "3.0.1",
          "com.orientechnologies" % "orientdb-graphdb" % "1.7.9",
          "com.notnoop.apns" % "apns" % "1.0.0.Beta4"

    		//	,"com.wordnik" %% "swagger-play2" % "1.2.1-SNAPSHOT",
    		//	"com.wordnik" %% "swagger-play2-utils" % "1.2.1-SNAPSHOT",
    )

    val baas = taskKey[File]("distribute standard baasbox format")


    val main = play.Project(appName, appVersion, appDependencies).settings(
       sources in doc in Compile := List(),

      resolvers := Seq(
          "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases",
          "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
		      "eaio" at "http://eaio.com/maven2"
	      )
       ,baas := {
          val distributionName = name.value.toLowerCase + "-" + version.value
		  val baseTarget = file(target.value.getAbsolutePath) / "universal"
          val tmpDir = baseTarget / distributionName
          val distributionDir = file(baseDirectory.value.getAbsolutePath) / "dist"
          val zipFile = distributionDir / (distributionName + ".zip")
          IO.delete(tmpDir)
          IO.delete(zipFile)
          val scriptsDir = file(baseDirectory.value.getAbsolutePath) / "distfiles"
          val staging = baseTarget / "stage"
          stage.value

          IO.copyDirectory(staging,tmpDir)
          IO.copyDirectory(scriptsDir,tmpDir)
          IO.delete(tmpDir/"bin")

          def entries(f : File):List[File] =
            f :: (if (f.isDirectory) IO.listFiles(f).toList.flatMap(entries(_)) else Nil)
          IO.zip(entries(tmpDir).map(
            d => (d,d.getAbsolutePath.substring(tmpDir.getParent.length+1))),
            zipFile)
          IO.delete(tmpDir)
          println("done packaging baas server distribution!!!")
          zipFile
        }
       , resourceDirectory in Compile <<= baseDirectory(_ => new File("conf"))
       , mappings in Universal ~= { _.filterNot { case (_,name) => name.startsWith("conf")}}
	     ,requireJs += "main.js"
       ,Keys.fork in (Test) := false
    )

}
