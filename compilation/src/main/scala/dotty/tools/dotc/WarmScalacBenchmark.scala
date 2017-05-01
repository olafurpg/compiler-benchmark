package dotty.tools.dotc

import java.util.concurrent.TimeUnit

import base.BaseBenchmark
import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

/**
  * Created by bibou on 28/04/2017.
  */
@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class WarmDotcBenchmark extends BaseBenchmark {
  @Benchmark
  override def compileDotc(): Unit = super.compileDotc()
}
