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

import scala.collection.JavaConverters._
import org.openjdk.jmh.annotations._

import scala.bench.SimpleFileVisitor1
import scala.bench.SimpleFileVisitor1._

@State(Scope.Benchmark)
class BaseBenchmark {

  @Param(value = Array[String](""))
  var classPath: String = _

  @Param(value = Array[String](""))
  var dottyVersion: String = _

  @Param(value = Array[String](""))
  var source: String = _

  @Param(value = Array(""))
  var extraArgs: String = _

  var compilerArgs: Array[String] = _

  var driver: Driver = _

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
  }

  protected def compileScalac(): Unit = {
    driver = new MainClass

    driver.process(compilerArgs)
    assert(!driver.reporter.hasErrors) // TODO: Remove
  }

  @Setup(Level.Trial)
  def setup(): Unit = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempDir = tempFile

    compilerArgs =
      if (source.startsWith("@")) {
        Array(source)
      }
      else {
        val allFiles = Files
          .walk(findSourceDir)
          .collect(Collectors.toList[Path])
          .asScala
          .toList

        allFiles
          .filter(_.getFileName.toString.endsWith(".scala"))
          .map(_.toAbsolutePath.toString)
          .toArray
      }
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






