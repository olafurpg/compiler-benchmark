package base

import scala.tools.nsc.ColdScalacBenchmark

import dotty.tools.dotc.ColdDotcBenchmark

class BenchmarkTest extends org.scalatest.FunSuite {

  def check(bench: BaseBenchmark): Unit = {
    val name = bench.getClass.getSimpleName
    test(name) {
      println(s"Running $name")
      bench.source = "corpus/vector"
      bench.setup()
      bench.compile()
    }
  }

  check(new ColdDotcBenchmark)
  check(new ColdScalacBenchmark)
}
