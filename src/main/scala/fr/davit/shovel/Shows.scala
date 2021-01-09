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

import cats._
import cats.implicits._
import fr.davit.taxonomy.model._
import fr.davit.taxonomy.model.record._

import java.net.InetSocketAddress

object Shows {

  implicit val showHeader: Show[DnsHeader] = Show.show { header =>
    import header._
    val qr    = if (`type` == DnsType.Response) Some("qr") else None
    val tv    = if (isTruncated) Some("tc") else None
    val rd    = if (isRecursionDesired) Some("rd") else None
    val ra    = if (isRecursionAvailable) Some("ra") else None
    val aa    = if (isAuthoritativeAnswer) Some("aa") else None
    val flags = (qr ++ tv ++ rd ++ ra ++ aa).mkString(" ")

    s"->>HEADER<<- opcode: $opCode, status: $responseCode, id: $id\n" +
      s"flags: $flags"
  }

  implicit val showClass: Show[DnsRecordClass] = Show.show {
    case DnsRecordClass.Internet => "IN"
    case DnsRecordClass.Chaos    => "CH"
    case DnsRecordClass.Hesiod   => "HS"
    case DnsRecordClass.Any      => "ANY"
    case other: DnsRecordClass   => other.toString
  }

  implicit val showType: Show[DnsRecordType] = Show.show(_.toString)

  implicit val showQuestion: Show[DnsQuestion] = Show.show { question =>
    import question._
    s"$name\t\t${`class`.show}\t${`type`.show}"
  }

  implicit val showData: Show[DnsRecordData] = Show.show {
    case DnsARecordData(ip)   => ip.getHostAddress
    case other: DnsRecordData => other.toString
  }

  implicit val showResource: Show[DnsResourceRecord] = Show.show { record =>
    import record._
    s"$name\t${ttl.toSeconds}\t${`class`.show}\t${data.`type`.show}\t${data.show}"
  }

  def header(message: DnsMessage): String = {
    message.header.show ++
      s", QUERY: ${message.questions.size}" +
      s", ANSWER: ${message.answers.size}" +
      s", AUTHORITY: ${message.authorities.size}" +
      s", ADDITIONAL: ${message.additionals.size}\n\n"
  }

  def question(questions: Seq[DnsQuestion]): String = {
    if (questions.isEmpty) {
      ""
    } else {
      questions
        .map(r => s"\t${r.show}")
        .mkString("QUESTION SECTION:\n", "\n", "\n\n")
    }
  }

  def section(name: String, records: Seq[DnsResourceRecord]): String = {
    if (records.isEmpty) {
      ""
    } else {
      records
        .map(r => s"\t${r.show}")
        .mkString(s"$name SECTION:\n", "\n", "\n\n")
    }
  }

  implicit val showMessage: Show[DnsMessage] = Show.show { message =>
    header(message) +
      question(message.questions) +
      section("ANSWERS", message.answers) +
      section("AUTHORITIES", message.authorities) +
      section("ADDITIONAL", message.additionals)
  }

  implicit val showSocketAddress: Show[InetSocketAddress] = Show.show { address =>
    s"${address.getHostString}#${address.getPort}(${address.getAddress.getHostAddress})"
  }

  implicit val showPacket: Show[DnsPacket] = Show.show { packet =>
    packet.message.show + s"SERVER: ${packet.address.show}\n"
  }

}
