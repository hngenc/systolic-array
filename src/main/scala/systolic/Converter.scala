package systolic

import chisel3._

object Converter {
  def visit(ex: Expr, map: Map[Systolic#Local, UInt]): UInt = ex match {
    case Ref(loc, _, _, _, _) => map(loc)
    case Add(l, r) => visit(l, map) +& visit(r, map)
    case Multiply(l, r) => visit(l, map) * visit(r, map)
    case SMux(c, t, f) => Mux(visit(c, map).toBool(), visit(t, map), visit(f, map))
    case _ => throw new Exception("converter error")
  }
}