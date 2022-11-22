ThisBuild / tlBaseVersion := "0.2"

val http4sVersion   = "0.23.16"
val natchezVersion  = "0.2.2"
val scala212Version = "2.12.17"
val scala213Version = "2.13.10"
val scala3Version   = "3.2.1"
val slf4jVersion    = "1.7.30"
val munitVersion    = "1.0.0-M7"
val munitCEVersion  = "2.0.0-M3"

// Publishing
ThisBuild / organization := "org.tpolecat"
ThisBuild / tlSonatypeUseLegacyHost := false
ThisBuild / licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))
ThisBuild / developers := List(
  Developer("tpolecat", "Rob Norris", "rob_norris@mac.com", url("http://www.tpolecat.org"))
)

// Compilation
ThisBuild / scalaVersion       := scala213Version
ThisBuild / crossScalaVersions := Seq(scala212Version, scala213Version, scala3Version)

// Global Settings
lazy val commonSettings = Seq(
  // Headers
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  headerLicense  := Some(HeaderLicense.Custom(
    """|Copyright (c) 2021 by Rob Norris
       |This software is licensed under the MIT License (MIT).
       |For more information see LICENSE or https://opensource.org/licenses/MIT
       |""".stripMargin
    )
  ),

  // Testing
  libraryDependencies ++= Seq(
    "org.scalameta" %%% "munit"               % munitVersion   % Test,
    "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test,
    "org.http4s"    %%% "http4s-dsl"          % http4sVersion  % Test,
  ),

)

// root project
lazy val root = tlCrossRootProject.aggregate(http4s)

lazy val http4s = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/http4s"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip := false,
    name        := "natchez-http4s",
    description := "Natchez middleware for http4s.",
    libraryDependencies ++= Seq(
      "org.tpolecat" %%% "natchez-core"  % natchezVersion,
      "org.http4s"   %%% "http4s-core"   % http4sVersion,
      "org.http4s"   %%% "http4s-client" % http4sVersion,
      "org.http4s"   %%% "http4s-server" % http4sVersion,
    )
  )

lazy val examples = project
  .in(file("modules/examples"))
  .dependsOn(http4s.jvm)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip       := true,
    name                 := "natchez-http4s-examples",
    description          := "Example programs for Natchez-Http4s.",
    scalacOptions        -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "natchez-jaeger"      % natchezVersion,
      "org.http4s"   %% "http4s-dsl"          % http4sVersion,
      "org.http4s"   %% "http4s-ember-server" % http4sVersion,
      "org.slf4j"     % "slf4j-simple"        % slf4jVersion,
    )
  )

lazy val docs = project
  .in(file("modules/docs"))
  .dependsOn(http4s.jvm)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(ParadoxPlugin)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    scalacOptions      := Nil,
    git.remoteRepo     := "git@github.com:tpolecat/natchez-http4s.git",
    ghpagesNoJekyll    := true,
    publish / skip     := true,
    paradoxTheme       := Some(builtinParadoxTheme("generic")),
    version            := version.value.takeWhile(_ != '+'), // strip off the +3-f22dca22+20191110-1520-SNAPSHOT business
    paradoxProperties ++= Map(
      "scala-versions"            -> (http4s.jvm / crossScalaVersions).value.map(CrossVersion.partialVersion).flatten.distinct.map { case (a, b) => s"$a.$b"} .mkString("/"),
      "org"                       -> organization.value,
      "scala.binary.version"      -> s"2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
      "core-dep"                  -> s"${(http4s.jvm / name).value}_2.${CrossVersion.partialVersion(scalaVersion.value).get._2}",
      "version"                   -> version.value,
      "scaladoc.natchez.base_url" -> s"https://static.javadoc.io/org.tpolecat/natchez-core_2.13/${version.value}",
    ),
    mdocIn := (baseDirectory.value) / "src" / "main" / "paradox",
    Compile / paradox / sourceDirectory := mdocOut.value,
    makeSite := makeSite.dependsOn(mdoc.toTask("")).value,
    mdocExtraArguments := Seq("--no-link-hygiene"), // paradox handles this
  )

