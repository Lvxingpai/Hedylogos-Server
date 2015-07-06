package core.finagle

import java.net.InetSocketAddress

//import com.aizou.yunkai.Userservice
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.thrift.ThriftClientFramedCodec
import com.twitter.util.Await
import org.apache.thrift.protocol.TBinaryProtocol

/**
 * Created by topy on 2015/6/16.
 */
object Client extends App {

  override def main(args: Array[String]) = {
    val (host, port) = ("192.168.100.2", 9400)
    //val (host, port) = ("localhost", 9000)

    val service = ClientBuilder()
      .hosts(new InetSocketAddress(host, port))
      .hostConnectionLimit(1000)
      .codec(ThriftClientFramedCodec())
      .build()

//    val client = new Userservice.FinagledClient(service, new TBinaryProtocol.Factory())
//
//    val loginName = "13699851562"
//    val passwd = "000999"
//
//    //val user = Await.result(client.login(loginName, passwd))
//    val user = Await.result(client.getUserById(100000))
//    println(user)
//
//    client.service.close()
  }


  main(args)

}
