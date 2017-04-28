package scala.tools.nsc

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

/**
  * Created by bibou on 28/04/2017.
  */
@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 8, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3)
class HotScalacBenchmark extends BaseScalacBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}
