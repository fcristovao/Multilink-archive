name := "Multilink"

version := "0.1.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
  "org.scaldi" %% "scaldi" % "0.3.2",
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-remote" % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "2.1.4" % "test"
)