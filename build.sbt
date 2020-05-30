name := "zio-rpc"
version := "0.1"
scalaVersion := "2.13.1"

val zioVersion = "1.0.0-RC20"
val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "com.lihaoyi" %% "upickle" % "1.0.0",
  "io.circe" %% "circe-generic"  % circeVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.lihaoyi" %% "utest" % "0.7.2" % "test"
)