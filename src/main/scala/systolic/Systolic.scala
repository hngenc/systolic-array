package systolic

import scala.collection.mutable.ArrayBuffer
import chisel3._
import chisel3.util._
import Util._

class Systolic {
  // Classes
  class Iterator(low: Int, high: Int, val diff: Int = 0) {
    def +(i: Int): Iterator = new Iterator(low, high, i)
    def -(i: Int): Iterator = this.+(-i)
  }

  object Iterator {
    def apply(low: Int, high: Int): Iterator = {
      bounds += ((low, high))
      new Iterator(low, high)
    }
  }

  class Input(it1: Iterator, it2: Iterator) {
    def apply(its: Iterator*) = {
      new IRef(this, its)
    }
  }

  class Output(it1: Iterator, it2: Iterator) {
    def apply(its: Iterator*) = {
      new ORef(this, its)
    }
  }

  class Local(val width: Int) {
    var input: IRef = null
    var calculation: Expr = null
    var output: ORef = null

    def apply(its: Iterator*): Ref = {
      new Ref(this, its.map(_.diff), its.map(_ => false), its)
    }

    def apply(it1: Iterator, num: Int, it2: Iterator): Ref = {
      specific_domains ++= comb(Seq(bounds(0)._1 to bounds(0)._2, num to num, bounds(2)._1 to bounds(2)._2):_*)
      new Ref(this, Seq(it1.diff, num, it2.diff), Seq(false, true, false), Seq(it1, null, it2))
    }

    def apply(num: Int, it1: Iterator, it2: Iterator): Ref = {
      specific_domains ++= comb(Seq(num to num, bounds(1)._1 to bounds(1)._2, bounds(2)._1 to bounds(2)._2):_*)
      new Ref(this, Seq(num, it1.diff, it2.diff), Seq(true, false, false), Seq(null, it1, it2))
    }

    def apply(it1: Iterator, it2: Iterator, num: Int): Ref = {
      specific_domains ++= comb(Seq(bounds(0)._1 to bounds(0)._2, bounds(1)._1 to bounds(1)._2, num to num):_*)
      new Ref(this, Seq(it1.diff, it2.diff, num), Seq(false, false, true), Seq(it1, it2, null))
    }
  }

  object Local {
    def apply(width: Int): Local = {
      val result = new Local(width)
      locals += result
      result
    }
  }

  // Variables
  val bounds = ArrayBuffer.empty[Tuple2[Int, Int]]
  val locals = ArrayBuffer.empty[Local]
  val specific_domains = ArrayBuffer.empty[Seq[Int]]

  var mod: Module = null

  // Methods
  def spaceTimeTransform(matrix: Seq[Seq[Int]]): Unit = {
    val P = matrix.init
    val s = Seq(matrix.last)

    println(s"Det: ${det(matrix)}")

    val domain = comb(bounds.map(t => t._1 to t._2):_*)
    val domain_verts = comb(bounds.map(t => Seq(t._1, t._2)):_*)

    // println("Your total domain space is:")
    // println(domain)

    val domain_to_range = domain.map(d => d -> matmul(matrix, Seq(d).transpose).flatten).toMap
    val proj_verts = (for (v <- domain_verts) yield matmul(P, Seq(v).transpose).flatten).distinct

    println("The vertices of your systolic array are:")
    proj_verts.foreach(v => println(s"\t$v"))

    println("Your input pattern is:")
    val inputSeqs = ArrayBuffer.empty[Tuple4[Systolic#Input, Int, Seq[Int], Seq[Int]]]
    for (it_vec <- domain) {
      val xyt = matmul(matrix, Seq(it_vec).transpose).flatten
      println(s"\tAt xyt: $xyt\tAt ijk: $it_vec")

      for (l <- locals.filter(_.input != null)) {
        val l_it = (it_vec, l.input.localRef.direction, l.input.localRef.fixed).zipped.map((i,d,f) => if (f) d else i)
        val l_coord = matmul(matrix, Seq(l_it).transpose).flatten

        if (l_coord == xyt) {
          println(s"\t\t${l_coord.init}")
          val in_coord = l.input.its.map(it => (l.input.localRef.its zip it_vec).collect { case (jt, i) if it == jt => i }.head)
          println(s"\t\t$in_coord")

          inputSeqs += ((l.input.input, xyt.last, l_coord.init, in_coord))
        }
      }
    }

    for (l <- locals.filter(_.input != null)) {
      val input_seq = inputSeqs.filter(l.input.input == _._1).distinct.sortWith(_._2 < _._2)
      println(s"\t${input_seq.map(t => (t._2, t._3, t._4))}")
    }

    println("Your output pattern is:")
    val outputSeqs = ArrayBuffer.empty[Tuple4[Systolic#Output, Int, Seq[Int], Seq[Int]]]
    for (it_vec <- domain) {
      val xyt = matmul(matrix, Seq(it_vec).transpose).flatten
      println(s"\tAt xyt: $xyt\tAt ijk: $it_vec")

      for (l <- locals.filter(_.output != null)) {
        val l_it = (it_vec, l.output.localRef.direction, l.output.localRef.fixed).zipped.map((i,d,f) => if (f) d else i)
        val l_coord = matmul(matrix, Seq(l_it).transpose).flatten

        if (l_coord == xyt) {
          println(s"\t\t${l_coord.init}")
          val out_coord = l.output.its.map(it => (l.output.localRef.its zip it_vec).collect { case (jt, i) if it == jt => i }.head)
          println(s"\t\t$out_coord")

          outputSeqs += ((l.output.output, xyt.last, l_coord.init, out_coord))
        }
      }
    }

    for (l <- locals.filter(_.output != null)) {
      val output_seq = outputSeqs.filter(l.output.output == _._1).distinct.sortWith(_._2 < _._2)
      println(s"\t${output_seq.map(t => (t._2, t._3, t._4))}")
    }

    val dep_vecs_ex = locals.map(l => dependencyvecs(l.calculation))
    println("Dependency vectors (in space):")
    dep_vecs_ex.foreach(dps => println(dps.map(dp => matmul(P, Seq(dp._2).transpose).flatten)))
    println("Dependency vectors (in time):")
    dep_vecs_ex.foreach(dps => println(dps.map(dp => matmul(s, Seq(dp._2).transpose).flatten)))

    val ind = locals.zipWithIndex.toMap

    val dep_vecs = locals.map(l => l -> dependencyvecs(l.calculation)).toMap
    val stationary_locals = locals.filter(l => matmul(P, Seq(dep_vecs(l).find(_._1 == l).get._2).transpose).flatten.forall(_ == 0))

    class PE extends Module {
      val io = IO (new Bundle {
        val ins = Input(MixedVec(locals.map(l => Input(UInt(l.width.W)))))
        val outs = Output(MixedVec(locals.map(l => Input(UInt(l.width.W)))))
      })

      val regs = locals.map(l => Reg(UInt(l.width.W)))
      val wires = locals.map(l => Wire(UInt(l.width.W)))

      // Initialize registers for debugging purposes only
      regs.zipWithIndex.foreach { case (r,i) =>
          r := ("h" + (i+97).asInstanceOf[Char]).U
      }

      // Connect up all wires
      for ((loc, i) <- ind) {
        wires(i) := Converter.visit(loc.calculation, (ind.keys zip regs).toMap)
      }

      // Connect all stationary registers
      for ((_, i) <- ind.filter(t => stationary_locals.contains(t._1))) {
        regs(i) := wires(i)
      }

      // Connect all non-stationary registers
      for ((_, i) <- ind.filter(t => !stationary_locals.contains(t._1))) {
        regs(i) := io.ins(i)
      }

      (io.outs zip wires).foreach{case (out, w) => out := w}
    }

    class Mesh extends Module {
      val space = domain_to_range.map(_._2.init).toSeq.distinct
      val pes = space.map(s => s -> Module(new PE)).toMap

      // println("Space:")
      // println(space)

      val inputs, outputs = ArrayBuffer.empty[UInt]
      val was_connected = ArrayBuffer.empty[UInt]

      pes.values.foreach(_.io.ins.foreach(_ := DontCare))

      // Connect inputs to adjacent PEs
      for ((loc, i) <- ind) {
        val it_dir = dep_vecs(loc).collect{case (l, d) if l == loc => d}.head
        val space_dir = matmul(P, Seq(it_dir).transpose).flatten

        pes.foreach{case (s, pe) =>
          val adj_coord = (s zip space_dir).map(t => t._1 - t._2)
          if (pes.contains(adj_coord)) {
            val adj = pes(adj_coord)
            pe.io.ins(i) := adj.io.outs(i)
            if(s != adj_coord)
              was_connected += adj.io.outs(i)
          } else {
            // Special case when it has to be connected to global inputs
            val new_input = Wire(UInt(pe.io.ins(i).getWidth.W))
            pe.io.ins(i) := new_input
            inputs += new_input
          }
        }
      }

      // Connect outputs
      for ((loc, i) <- ind) {
        pes.filter(t => !was_connected.contains(t._2.io.outs(i))).foreach{case (_, pe) =>
          val new_output = Wire(UInt(pe.io.outs(i).getWidth.W))
          new_output := pe.io.outs(i)
          outputs += new_output
        }
      }

      // TODO remove this
      /*val height = space.map(_.head).max
      pes.collect{case (loc, pe) if loc(0) == height => pe}.foreach { pe =>
        val new_output = Wire(UInt(pe.io.outs.last.getWidth.W))
        new_output := pe.io.outs.last
        outputs += new_output
      }*/

      val io = IO(new Bundle {
        val ins = Input(MixedVec(inputs.map(_.cloneType)))
        val outs = Output(MixedVec(outputs.map(_.cloneType)))
      })

      (inputs zip io.ins).foreach(t => t._1 := t._2)
      (io.outs zip outputs).foreach(t => t._1 := t._2)
    }

    mod = new Mesh
  }

  // Ok, start now

  val N1, N2, N3 = 1

  val i = Iterator(0, N1)
  val j = Iterator(0, N2)
  val k = Iterator(0, N3)

  val A = new Input(i, k)
  val B = new Input(k, j)
  val C = new Output(i, j)

  val a, b = Local(16)
  val c = Local(32)

  // Inputs
  a(i, 0, k) := A(i, k)
  b(0, j, k) := B(k, j)
  /* TODO
  c(i, j, 0) := 0
  */

  // Calculations
  a(i, j, k) := a(i, j-1, k)
  b(i, j, k) := b(i-1, j, k)
  c(i, j, k) := c(i, j, k-1) + (a(i, j-1, k) * b(i-1, j, k))

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

  // Single PE\/
  /*
  spaceTimeTransform(Seq(
    Seq(0,0,0),
    Seq(0,0,0),
    Seq((N2+1)*(N3+1),(N3+1),1)))
  */
}

object OutputStationary {
  def apply(): Module = (new Systolic).mod
}
