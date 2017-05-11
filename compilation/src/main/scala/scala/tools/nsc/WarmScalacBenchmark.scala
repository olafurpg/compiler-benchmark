package scala.tools.nsc

import java.util.concurrent.TimeUnit

import base.BaseBenchmark
import base.BaseScalacBenchmark
import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class WarmScalacBenchmark extends BaseScalacBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}
