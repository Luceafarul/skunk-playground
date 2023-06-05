val scala3Version = "3.3.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "skunk-playground",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "skunk-core" % "0.6.0",
      "org.scalameta" %% "munit"      % "0.7.29" % Test,
    )
  )
