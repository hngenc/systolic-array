package systolic

import chisel3._

class MatMul extends Systolic {
  val N1, N2, N3 = 1

  val i = Iterator(0, N1)
  val j = Iterator(0, N2)
  val k = Iterator(0, N3)

  val A = Input(i, k)
  val B = Input(k, j)
  val C = Output(i, j)

  // Reaching stationaries
  // val S = new Input(k, j)

  val a, b = Local(16)
  val c = Local(32)

  // val s = Local(1)

  // Inputs
  a(i, 0, k) := A(i, k)
  b(0, j, k) := B(k, j)
  c(i, j, 0) := 0
  // a(i, j, k) := A(i, k)
  // b(i, j, k) := B(k, j)
  // s(0, j, k) := S(k, j)

  // Calculations
  a(i, j, k) := a(i, j-1, k)
  b(i, j, k) := b(i-1, j, k)
  c(i, j, k) := c(i, j, k-1) + (a(i, j-1, k) * b(i-1, j, k))
  /*
  b(i, j, k) := SMux(s(i-1, j, k), c(i, j, k-1), b(i-1, j, k))
  c(i, j, k) := SMux(s(i-1, j, k), b(i-1, j, k), c(i, j, k-1) + (a(i, j-1, k) * b(i-1, j, k)))
  s(i, j, k) := s(i-1, j, k)
  */

  // Outputs
  C(i, j) := c(i, j, N3)

  // Space-time transformation
  // Output-stationary
  spaceTimeTransform(Seq(
    Seq(1,0,0),
    Seq(0,1,0),
    Seq(1,1,1)))

  /*
  // Hexagonal
  spaceTimeTransform(Seq(
    Seq(0,-1,1),
    Seq(-1,1,0),
    Seq(1,1,1)))
  */

  /*
  // Weight-stationary
  spaceTimeTransform(Seq(
    Seq(0,0,1),
    Seq(0,1,0),
    Seq(1,1,1)))
  */

  /*
  // Single PE
  spaceTimeTransform(Seq(
    Seq(0,0,0),
    Seq(0,0,0),
    Seq((N2+1)*(N3+1),(N3+1),1)))
  */

  // Two PEs
  /*
  spaceTimeTransform(Seq(
    Seq(2.0/(N1+1), 0, 0),
    Seq(0, 0, 0),
    Seq((N2+1)*(N3+1)/((N2+1)*(N3+1)+1), (N3+1), 1)))
  */

  /*
  // Output stationary
  fix(c)
  flowR(a)
  flowD(b)
  spaceTimeTransform()
  */

  // fix(b)
  // flowD(c)
  // flowR(a)
  /*
  val st = getSpaceTimeTransform
  println(st)
  println(st.map(det(_)))
  */
}

object SystolicMain extends App {
  Driver.execute(args, () => (new MatMul).mod)
}
