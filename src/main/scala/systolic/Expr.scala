package systolic

trait Expr {
  def +(that: Expr) = Add(this, that)
  def *(that: Expr) = Multiply(this, that)
}

case class Ref(local: Systolic#Local, direction: Seq[Int], fixed: Seq[Boolean], its: Seq[Systolic#Iterator], maxes: Seq[Int])
  extends Expr {

  def :=(that: Expr): Unit = {
    // assert(direction.forall(_ == 0))
    that match {
      case in: IRef => in.localRef = this; local.input = in
      case out: ORef => out.localRef = this; local.output = out
      case calc => local.calculation = calc
    }
  }

  def :=(that: Int): Unit = {
    // TODO
  }
}

case class IRef(input: Systolic#Input, its: Seq[Systolic#Iterator]) extends Expr {
  var localRef: Ref = null
}

case class ORef(output: Systolic#Output, its: Seq[Systolic#Iterator]) extends Expr {
  var localRef: Ref = null

  def :=(that: Ref): Unit = {
    localRef = that
    that.local.output = this
  }
}

case class Add(left: Expr, right: Expr) extends Expr
case class Multiply(left: Expr, right: Expr) extends Expr
case class SMux(cond: Expr, ifTrue: Expr, ifFalse: Expr) extends Expr
