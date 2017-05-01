package dotty.tools.dotc

import java.util.concurrent.TimeUnit

import base.BaseBenchmark
import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 8, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class HotDotcBenchmark extends BaseBenchmark {
  @Benchmark
  override def compileDotc(): Unit = super.compileDotc()
}
