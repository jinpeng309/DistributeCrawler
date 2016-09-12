name := "crawler"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
    "RoundEights" at "http://maven.spikemark.net/roundeights"
)

libraryDependencies ++= Seq(
    "org.jsoup" %  "jsoup" % "1.7.2",
    "com.typesafe.akka" %% "akka-actor" % "2.4.10",
    "com.typesafe.akka" %% "akka-cluster" % "2.4.10",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.10",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.10",
    "com.typesafe.akka" %% "akka-stream" % "2.4.10",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.10"
)