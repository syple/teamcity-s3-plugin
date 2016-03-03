package com.gu.teamcity

import java.io.{File, PrintWriter}

import com.amazonaws.auth.{BasicAWSCredentials, AWSCredentialsProvider, AWSCredentials}
import jetbrains.buildServer.serverSide.ServerPaths
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._

case class S3Config(
  artifactBucket: Option[String],
  awsAccessKey: Option[String], awsSecretKey: Option[String]
)

class S3ConfigManager(paths: ServerPaths) extends AWSCredentialsProvider {
  implicit val formats = Serialization.formats(NoTypeHints)

  val configFile = new File(s"${paths.getConfigDir}/s3.json")

  private[teamcity] var config: Option[S3Config] = {
    if (configFile.exists()) {
      parse(configFile).extractOpt[S3Config]
    } else None
  }

  def artifactBucket: Option[String] = config.flatMap(_.artifactBucket)
  
  private[teamcity] def update(config: S3Config): Unit = {
    this.config = Some(if (config.awsSecretKey.isEmpty && config.awsAccessKey == this.config.flatMap(_.awsAccessKey)) {
      config.copy(awsSecretKey = this.config.flatMap(_.awsSecretKey))
    } else config)
  }

  def updateAndPersist(newConfig: S3Config): Unit = {
    synchronized {
      update(newConfig)
      val out = new PrintWriter(configFile, "UTF-8")
      try { writePretty(config, out) }
      finally { out.close }
    }
  }

  def details: Map[String, Option[String]] = Map(
    "artifactBucket" -> artifactBucket,
    "accessKey" -> config.flatMap(_.awsAccessKey)
  )

  override def getCredentials: AWSCredentials = (for {
    c <- config
    accessKey <- c.awsAccessKey
    secretKey <- c.awsSecretKey
  } yield new BasicAWSCredentials(accessKey, secretKey)).getOrElse(null) // Yes, this is sad

  override def refresh(): Unit = ()
}

object S3ConfigManager {
  val bucketElement = "bucket"
  val s3Element = "S3"
}