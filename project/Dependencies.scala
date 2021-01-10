import sbt._

object Dependencies {

  object Versions {
    val Decline  = "1.3.0"
    val Taxonomy = "0.3.0"
  }

  val Decline  = "com.monovore" %% "decline-effect" % Versions.Decline
  val Taxonomy = "fr.davit"     %% "taxonomy-fs2"   % Versions.Taxonomy

}
