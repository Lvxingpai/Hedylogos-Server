package core.finagle

import java.net.InetSocketAddress

import com.aizou.yunkai.{UserInfo, Userservice}
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodec
import com.twitter.util.Future
import org.apache.thrift.protocol.TBinaryProtocol

/**
 * Created by topy on 2015/6/10.
 */
object FinagleFactory {

  private val service = {
    val (host, port) = ("192.168.100.2", 9400)
    val service = ClientBuilder()
      .hosts(new InetSocketAddress(host, port))
      .hostConnectionLimit(1000)
      .codec(ThriftClientFramedCodec())
      .build()
    service
  }

  private val protocol = new TBinaryProtocol.Factory()

  private val userClient = new Userservice.FinagledClient(service, protocol)

  def createUser(nickName: String, password: String, tel: String): Future[UserInfo] = {
    for {
      userInfo <- userClient.createUser(nickName, password, Some(tel))
    } yield userInfo
  }
}
