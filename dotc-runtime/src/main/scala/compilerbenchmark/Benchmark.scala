package compilerbenchmark

import java.io.File
import java.net.URLClassLoader

import dotty.tools.dotc.core.Contexts.ContextBase

class Benchmark {
  def compile(compilerClasspath: Array[String],
              classpath: String,
              outDir: String,
              extraArgs: String,
              files: Array[String]): Unit = {
    implicit val ctx = (new ContextBase).initialCtx.fresh
    val stdlib =
      compilerClasspath
        .filter(
          x =>
            (x.endsWith("NIGHTLY.jar") && x.contains("dotty-library")) ||
              (!x.endsWith("NIGHTLY.jar") && x.contains("scala-library"))
        )
    ctx.setSetting(ctx.settings.classpath, stdlib.mkString(File.pathSeparator))
    ctx.setSetting(ctx.settings.d, outDir)
    ctx.setSetting(ctx.settings.language, List("Scala2"))
    val compiler = new dotty.tools.dotc.Compiler
    val reporter = dotty.tools.dotc.Bench.doCompile(compiler, files.toList)
    assert(!reporter.hasErrors)
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    val b = new Benchmark
    val thisCp: Array[String] = this.getClass.getClassLoader match {
      case u: URLClassLoader => u.getURLs.map(_.getPath)
      case _ => Array.empty
    }
    b.compile(thisCp, "", "target", "", Array("corpus/vector/Vector.scala"))
  }
}
