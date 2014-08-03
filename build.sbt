import play.PlayScala

name := """dorloch"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.scala-lang" %% "scala-pickling" % "0.8.0",
  "com.etaty.rediscala" %% "rediscala" % "1.3.1",
  "org.scalatestplus" %% "play" % "1.1.0" % "test"
)
