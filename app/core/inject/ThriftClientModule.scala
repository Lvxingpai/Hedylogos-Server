package core.inject

import play.api.{ Configuration, Environment }
import play.api.inject.{ Binding, Module }
import com.lvxingpai.yunkai.Userservice.{ FinagledClient => YunkaiClient }

/**
 * Created by zephyre on 1/11/16.
 */
class ThriftClientModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(bind[YunkaiClient] to new ThriftClientProvider[YunkaiClient])
}

