/*
 * Copyright 2020 Michel Davit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.davit.shovel

import cats.effect._
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._
import fr.davit.shovel.Shows._
import fr.davit.taxonomy.fs2.{Dns, DnsPacket}
import fr.davit.taxonomy.model.record.{DnsRecordClass, DnsRecordType, DnsResourceRecord}
import fr.davit.taxonomy.model.{DnsMessage, DnsQuestion}
import fr.davit.taxonomy.scodec.DnsCodec
import fs2.io.udp.SocketGroup
import scodec.Codec
import sun.net.dns.ResolverConfiguration

import java.net.InetSocketAddress
import scala.jdk.CollectionConverters._

object Shovel extends CommandIOApp(name = "shovel", header = "") {

  val nameOpts: Opts[String] = Opts.argument[String]("name")

  implicit val codec: Codec[DnsMessage] = DnsCodec.dnsMessage

  def systemResolver[F[_]: Sync](): Resource[F, ResolverConfiguration] =
    Resource.make(Sync[F].delay(ResolverConfiguration.open()))(_ => Sync[F].unit)

  def systemServers[F[_]: Sync](resolverConfiguration: ResolverConfiguration): F[List[InetSocketAddress]] =
    Sync[F].delay(resolverConfiguration.nameservers().asScala.toList.map(s => new InetSocketAddress(s, 53)))

  def query(server: InetSocketAddress, name: String): DnsPacket = {
    val question = DnsQuestion(name, DnsRecordType.A, unicastResponse = false, DnsRecordClass.Internet)
    val message  = DnsMessage.query(id = 1, questions = Seq(question))
    DnsPacket(server, message)
  }

  def printQuestions(questions: Seq[DnsQuestion]): Unit = {
    if (questions.nonEmpty) {
      println("\nQUESTION SECTION:")
      questions.foreach(q => println(s"\t${q.show}"))
    }
  }

  def printSection(section: String, records: Seq[DnsResourceRecord]): Unit = {
    if (records.nonEmpty) {
      println(s"\n$section SECTION:")
      records.foreach(r => println(s"\t${r.show}"))
    }
  }

  def printResult[F[_]: Sync](response: DnsMessage): F[Unit] =
    for {
      _ <- Sync[F].delay(println("Got answer:"))
      _ <- Sync[F].delay(println(response.header.show))
      _ <- Sync[F].delay(printQuestions(response.questions))
      _ <- Sync[F].delay(printSection("ANSWERS", response.answers))
      _ <- Sync[F].delay(printSection("AUTHORITIES", response.authorities))
      _ <- Sync[F].delay(printSection("ADDITIONAL", response.additionals))
    } yield ()

  override def main: Opts[IO[ExitCode]] = nameOpts.map { name =>
    val resources = for {
      resolver    <- systemResolver[IO]()
      blocker     <- Blocker[IO]
      socketGroup <- SocketGroup[IO](blocker)
      socket      <- socketGroup.open[IO]()
    } yield (resolver, socket)

    resources.use {
      case (resolver, socket) =>
        for {
          servers <- systemServers[IO](resolver)
          response <- servers.foldLeft(IO.raiseError[DnsMessage](new Exception("No DNS server provided"))) {
            case (result, server) => result.orElse(Dns.resolve(socket, query(server, name)))
          }
          _ <- printResult[IO](response)
        } yield ExitCode.Success
    }
  }
}
