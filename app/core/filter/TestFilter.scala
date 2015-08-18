package core.filter

import core.exception.StopMessageFilterException

/**
 * Created by zephyre on 8/18/15.
 */
class TestFilter extends Filter {
  override val doFilter: PartialFunction[AnyRef, AnyRef] = {
    case _ => throw StopMessageFilterException("TestFilter")
  }
}
