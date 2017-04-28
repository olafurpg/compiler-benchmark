package scala.tools.nsc

import java.util.concurrent.TimeUnit

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
class WarmScalacBenchmark extends BaseScalacBenchmark {
  @Benchmark
  override def compile(): Unit = super.compile()
}
