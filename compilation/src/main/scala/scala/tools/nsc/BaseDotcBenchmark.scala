package scala.tools.nsc

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import dotty.tools.dotc.core.Contexts.ContextBase

import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters._

@State(Scope.Benchmark)
class BaseDotcBenchmark {
  @Param(value = Array[String]())
  var _classpath: String = _
  @Param(value = Array[String]())
  var _dottyVersion: String = _
  @Param(value = Array[String]())
  var source: String = _
  @Param(value = Array(""))
  var extraArgs: String = _
  var driver: Driver = _


  protected def compile(): Unit = {
    val cp = _classpath
    val compilerArgs =
      if (source.startsWith("@")) Array(source)
      else {
        import scala.collection.JavaConverters._
        val path = Collectors.toList[Path]()
        val allFiles = Files.walk(findSourceDir).collect(path).asScala.toList
        allFiles
          .filter(_.getFileName.toString.endsWith(".scala"))
          .map(_.toAbsolutePath.toString)
          .toArray
      }

    implicit val ctx = (new ContextBase).initialCtx.fresh
    ctx.setSetting(ctx.settings.classpath, cp)
    ctx.setSetting(ctx.settings.usejavacp, true)
    ctx.setSetting(ctx.settings.d, tempOutDir.getAbsolutePath)
    ctx.setSetting(ctx.settings.nowarn, true)
    if (source == "scalap")
      ctx.setSetting(ctx.settings.language, List("Scala2"))
    val reporter = Bench.doCompile(new Compiler, compilerArgs.toList)
    assert(!reporter.hasErrors)
  }


  private var tempDir: File = null

  @Setup(Level.Trial)
  def initTemp(): Unit = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempDir = tempFile
  }

  @TearDown(Level.Trial)
  def clearTemp(): Unit = {
    val directory = tempDir.toPath
    Files.walkFileTree(directory, new SimpleFileVisitor1[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  private def findSourceDir: Path = {
    val path = Paths.get("../corpus/" + source)
    if (Files.exists(path)) path
    else Paths.get(source)
  }
}






