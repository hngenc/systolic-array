package systolic

import chisel3._

object SystolicMain extends App {
  Driver.execute(args, () => OutputStationary())
  //Driver.execute(args, () => OutputStationary.pe)
}
