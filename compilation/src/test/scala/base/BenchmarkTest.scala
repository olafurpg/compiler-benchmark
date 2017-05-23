package base

import scala.tools.nsc.ColdScalacBenchmark

import dotty.tools.dotc.ColdDotcBenchmark

class BenchmarkTest extends org.scalatest.FunSuite {

  def check(bench: BaseBenchmark, corpus: String): Unit = {
    val name = s"${bench.getClass.getSimpleName}/$corpus"
    test(name) {
      println(s"Running $name")
      bench.source = s"corpus/$corpus"
      bench.setup()
      bench.compile()
    }
  }

  for {
    bench <- List(new ColdDotcBenchmark, new ColdScalacBenchmark)
    source <- List("vector", "squants", "paiges")
  } yield check(bench, source)
}
