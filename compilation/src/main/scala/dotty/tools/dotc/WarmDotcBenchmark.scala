package dotty.tools.dotc

import java.util.concurrent.TimeUnit

import base.BaseBenchmark
import base.BaseDotcBenchmark
import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class WarmDotcBenchmark extends BaseDotcBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}
