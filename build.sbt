
/*
 build.sbt adapted from https://github.com/pbassiner/sbt-multi-project-example/blob/master/build.sbt
*/


name := "repository-utils"
ThisBuild / organization := "de.ekut.tbi"
ThisBuild / version := "1.0-SNAPSHOT"

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.8"
lazy val supportedScalaVersions =
  List(
//    scala212,
    scala213
  )

ThisBuild / scalaVersion := scala213


//-----------------------------------------------------------------------------
// PROJECT
//-----------------------------------------------------------------------------

lazy val global = project
  .in(file("."))
  .settings(
    settings,
    libraryDependencies ++= Seq(
      "org.typelevel"     %% "cats-core" % "2.1.1",
      "com.typesafe.play" %% "play-json" % "2.8.1",
      "org.slf4j"         %  "slf4j-api" % "1.7.32",
//      "org.slf4j"         %  "slf4j-api" % "1.7.26",
      "org.scalatest"     %% "scalatest" % "3.1.1" % Test
    ),
    crossScalaVersions := supportedScalaVersions
  )


//-----------------------------------------------------------------------------
// SETTINGS
//-----------------------------------------------------------------------------

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
//  "-language:existentials",
  "-language:higherKinds",
//  "-language:implicitConversions",
//  "-language:postfixOps",
  "-Xfatal-warnings",
  "-deprecation",
  "-encoding", "utf8"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq("Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository") ++
    Resolver.sonatypeOssRepos("releases") ++
    Resolver.sonatypeOssRepos("snapshots")
)

lazy val settings = commonSettings
