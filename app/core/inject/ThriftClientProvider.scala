package core.inject

import java.net.InetSocketAddress
import javax.inject.{ Inject, Provider }

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodec
import com.twitter.scrooge.ThriftService
import org.apache.thrift.protocol.TBinaryProtocol
import play.api.Configuration
import play.api.inject.{ BindingKey, Injector }

import scala.reflect._

/**
 * Created by zephyre on 1/13/16.
 */
class ThriftClientProvider[T <: ThriftService: ClassTag] extends Provider[T] {
  @Inject private var injector: Injector = _

  lazy val get: T = {
    val confKey = BindingKey(classOf[Configuration]) qualifiedWith "etcdService"
    val services = injector instanceOf confKey

    val yunkai = services getConfig "services.yunkai" getOrElse Configuration.empty
    val subKeys = yunkai.subKeys.toSeq

    // 服务器地址设置:  [ {host: "xxx", port: 2379} ]
    val addresses = subKeys map (serverKey => {
      for {
        host <- yunkai getString s"$serverKey.host"
        port <- yunkai getInt s"$serverKey.port"
      } yield {
        new InetSocketAddress(host, port)
      }
    }) filter (_.nonEmpty) map (_.get)

    val service = ClientBuilder()
      .hosts(addresses)
      .hostConnectionLimit(1000)
      .codec(ThriftClientFramedCodec())
      .build()

    val cls = classTag[T].runtimeClass
    val constructor = cls.getConstructors.head

    constructor.newInstance(service, new TBinaryProtocol.Factory(),
      cls.getTypeName, com.twitter.finagle.stats.NullStatsReceiver).asInstanceOf[T]
  }
}

