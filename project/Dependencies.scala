import sbt._

object Dependencies {

  object Versions {
    val Decline  = "2.2.0"
    val Taxonomy = "1.1.0-SNAPSHOT"
  }

  val Decline  = "com.monovore" %% "decline-effect" % Versions.Decline
  val Taxonomy = "fr.davit"     %% "taxonomy-fs2"   % Versions.Taxonomy

}
