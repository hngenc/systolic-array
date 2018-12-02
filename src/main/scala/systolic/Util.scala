package systolic

object Util {
  def comb[T](ls: Seq[T]*): Seq[Seq[T]] = {
    ls.foldLeft(Seq(Seq[T]()))((acc, x) => for (i <- acc; j <- x) yield i :+ j)
  }

  def matmul[A](a: Seq[Seq[A]], b: Seq[Seq[A]])(implicit n: Numeric[A]) = {
    import n._
    for (row <- a)
      yield for(col <- b.transpose)
        yield row zip col map Function.tupled(_*_) sum
  }

  def permutationsSgn[T]: List[T] => List[(Int,List[T])] = {
    case Nil => List((1,Nil))
    case xs => {
      for {
        (x, i) <- xs.zipWithIndex
        (sgn,ys) <- permutationsSgn(xs.take(i) ++ xs.drop(1 + i))
      } yield {
        val sgni = sgn * (2 * (i%2) - 1)
        (sgni, (x :: ys))
      }
    }
  }

  def det(m:Seq[Seq[Int]]) = {
    val summands =
      for {
        (sgn, sigma) <- permutationsSgn((0 to m.length - 1).toList).toList
      }
        yield {
          val factors =
            for (i <- 0 to (m.length - 1))
              yield m(i)(sigma(i))
          factors.toList.foldLeft(sgn)({ case (x, y) => x * y })
        }
    summands.toList.foldLeft(0)({ case (x, y) => x + y })
  }

  def dependencyvecs(e: Expr): Seq[Tuple2[Systolic#Local, Seq[Int]]] = e match {
    case Add(l, r) => dependencyvecs(l) ++ dependencyvecs(r)
    case Multiply(l, r) => dependencyvecs(l) ++ dependencyvecs(r)
    case Ref(loc, dir, _, _) => Seq((loc, dir.map(-_)))
    case _ => throw new Exception("unknown in dep vec")
  }
}
