package misc

import java.net.InetSocketAddress

import com.lvxingpai.smscenter.SmsCenter.{ FinagledClient => SmsClient }
import com.lvxingpai.yunkai.Userservice.{ FinagledClient => YunkaiClient }
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodec
import core.finagle.CoreConfig
import org.apache.thrift.protocol.TBinaryProtocol

/**
 * Created by zephyre on 6/30/15.
 */
object FinagleFactory {
  lazy val client = {
    val backends = CoreConfig.conf.getConfig("backends.yunkai").get
    val services = backends.subKeys.toSeq map (backends.getConfig(_).get)

    val server = services.head.getString("host").get -> services.head.getInt("port").get

    val service = ClientBuilder()
      //      .hosts(new InetSocketAddress("127.0.0.1", 9005))
      //      .hosts(new InetSocketAddress(server._1, server._2))
      .hosts(new InetSocketAddress("192.168.100.2", 9400))
      .hostConnectionLimit(1000)
      .codec(ThriftClientFramedCodec())
      .build()
    new YunkaiClient(service, new TBinaryProtocol.Factory())
  }

  lazy val smsClient = {
    val backends = CoreConfig.conf.getConfig("backends.smscenter").get
    val servers = for {
      subKey <- backends.subKeys.toSeq
      conf <- backends.getConfig(subKey)
      host <- conf.getString("host")
      port <- conf.getInt("port")
    } yield host -> port

    val service = ClientBuilder()
      .hosts(new InetSocketAddress(servers.head._1, servers.head._2))
      .hostConnectionLimit(1000)
      .codec(ThriftClientFramedCodec())
      .build()

    new SmsClient(service, new TBinaryProtocol.Factory())
  }
}
