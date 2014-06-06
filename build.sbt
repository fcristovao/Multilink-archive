name := "Multilink"

version := "0.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.3",
  "com.typesafe.akka" %% "akka-remote" % "2.3.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
  "org.scalatest" % "scalatest_2.10" % "2.1.4" % "test"
)

libraryDependencies += "org.scala-lang" % "scala-swing" % "2.10.4"
