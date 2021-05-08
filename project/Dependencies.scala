import sbt._

object Dependencies {

  object Versions {
    val Decline  = "2.0.0"
    val Taxonomy = "1.0.0"
  }

  val Decline  = "com.monovore" %% "decline-effect" % Versions.Decline
  val Taxonomy = "fr.davit"     %% "taxonomy-fs2"   % Versions.Taxonomy

}
