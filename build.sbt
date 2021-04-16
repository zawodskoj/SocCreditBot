name := "social-credit-bot"

version := "0.1"

scalaVersion := "2.13.5"
scalacOptions += "-Ymacro-annotations"

idePackagePrefix := Some("by.oyae.soccredit.bot")

resolvers += "Skija" at "https://packages.jetbrains.team/maven/p/skija/maven"

val http4sVersion = "0.21.21"
val circeVersion = "0.13.0"
val skijaVersion = "0.90.3"
val pureconfigVersion = "0.14.1"

val skijaPlatform = System.getProperty("os.name").toLowerCase match {
  case win if win.contains("win") => "windows"
  case linux if linux.contains("linux") => "linux"
  case _ => throw new Exception("gfy with your macosx")
}

libraryDependencies += "org.jetbrains.skija" % s"skija-$skijaPlatform" % skijaVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig" % pureconfigVersion
libraryDependencies ++= List("http4s-blaze-client", "http4s-blaze-server", "http4s-circe", "http4s-dsl").map("org.http4s" %% _ % http4sVersion)
libraryDependencies ++= List("circe-parser", "circe-generic").map("io.circe" %% _  % circeVersion)