package core.filter

/**
 * Created by pengyt on 2015/8/11.
 */
trait Filter {
  val doFilter: PartialFunction[AnyRef, AnyRef]
}
