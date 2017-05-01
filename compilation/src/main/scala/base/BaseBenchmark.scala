package base

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters._
import scala.tools.nsc._
import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import dotty.tools.dotc.core.Contexts.ContextBase
import scala.collection.JavaConverters._
import org.openjdk.jmh.annotations._

import SimpleFileVisitor1._

@State(Scope.Benchmark)
class BaseBenchmark {

  @Param(value = Array[String](""))
  var _classpath: String = _

  @Param(value = Array[String](""))
  var _dottyVersion: String = _

  @Param(value = Array[String](""))
  var source: String = _

  @Param(value = Array(""))
  var extraArgs: String = _

  var driver: Driver = _

  var compilerArgs: Array[String] = _

  // MainClass is copy-pasted from compiler for source compatibility with 2.10.x - 2.13.x
  class MainClass extends Driver with EvalLoop {
    def resident(compiler: Global): Unit = loop { line =>
      val command = new CompilerCommand(line split "\\s+" toList, new Settings(scalacError))
      compiler.reporter.reset()
      new compiler.Run() compile command.files
    }

    override def newCompiler(): Global = Global(settings, reporter)

    override protected def processSettingsHook(): Boolean = {
      settings.usejavacp.value = true
      settings.outdir.value = tempDir.getAbsolutePath
      settings.nowarn.value = true
      if (extraArgs != null && extraArgs != "")
        settings.processArgumentString(extraArgs)
      true
    }

    compilerArgs =
      if (source.startsWith("@")) {
        Array(source)
      }
      else {
        val allFiles = Files.walk(findSourceDir).collect(Collectors.toList[Path]).asScala.toList
        allFiles.filter(_.getFileName.toString.endsWith(".scala")).map(_.toAbsolutePath.toString).toArray
      }
  }

  protected def compileScalac(): Unit = {
    driver.process(compilerArgs)
    assert(!driver.reporter.hasErrors) // TODO: Remove
  }

  protected def compileDotc(): Unit = {
    val cp = _classpath
    val compilerArgs =
      if (source.startsWith("@")) Array(source)
      else {
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
    if (source == "scalap")
      ctx.setSetting(ctx.settings.language, List("Scala2"))
    val reporter = dotty.tools.dotc.Bench.doCompile(new dotty.tools.dotc.Compiler, compilerArgs.toList)
    assert(!reporter.hasErrors)
  }

  @Setup(Level.Trial)
  def initTemp(): Unit = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempDir = tempFile

    driver = new MainClass
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


  private var tempDir: File = null

  private def tempOutDir: File = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempFile
  }

  private def findSourceDir: Path = {
    val path = Paths.get("../corpus/" + source)
    if (Files.exists(path)) path
    else Paths.get(source)
  }
}






