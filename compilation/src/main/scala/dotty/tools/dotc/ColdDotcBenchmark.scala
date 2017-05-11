package dotty.tools.dotc

import java.util.concurrent.TimeUnit

import base.BaseBenchmark
import org.openjdk.jmh.annotations.Mode.SingleShotTime
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
@BenchmarkMode(Array(SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
// TODO -Xbatch reduces fork-to-fork variance, but incurs 5s -> 30s slowdown
@Fork(value = 1, jvmArgs = Array("-XX:CICompilerCount=2"))
class ColdDotcBenchmark extends BaseBenchmark {
  @Benchmark
  override def compileDotc(): Unit = super.compileDotc()
}
