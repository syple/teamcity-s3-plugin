package com.gu.teamcity

import java.io.{InputStream, File}

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.transfer.TransferManager
import jetbrains.buildServer.serverSide.SBuild

import scala.util.{Success, Try}

class S3(config: S3ConfigManager) {
  val credentialsProvider = {
    val provider = new AWSCredentialsProviderChain(config, new DefaultAWSCredentialsProviderChain())
    provider.setReuseLastProvider(false)
    provider
  }

  val transferManager = new TransferManager(
    new AmazonS3Client(credentialsProvider, new ClientConfiguration().withMaxErrorRetry(2))
  )

  def upload(bucket: String, build: SBuild, fileName: String, contents: InputStream, fileSize: Long): Try[Unit] =
    Try {
      val uploadDirectory = s"${S3Plugin.cleanFullName(build)}"
      val metadata = {
        val md = new ObjectMetadata()
        md.setContentLength(fileSize)
        md
      }
      val req = new PutObjectRequest(bucket, s"$uploadDirectory/$fileName", contents, metadata)
      val upload = transferManager.upload(req)
      upload.waitForUploadResult()
    }

  def upload(bucket: String, build: SBuild, fileName: String, file: File): Try[Unit] =
    Try {
      val uploadDirectory = s"${S3Plugin.cleanFullName(build)}"
      val req = new PutObjectRequest(bucket, s"$uploadDirectory/$fileName", file)
      val upload = transferManager.upload(req)
      upload.waitForUploadResult()
    }

}
