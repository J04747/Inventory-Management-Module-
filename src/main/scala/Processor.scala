import scala.annotation.unused

trait Config

trait Processor:
  def process(data: String, config: Config): Unit

class MyProcessor extends Processor:
  // Use the @unused annotation
  def process(data: String, @unused config: Config): Unit =
    println(data)

class MyProcessor2 extends Processor:
  // In Scala 3, you can omit the parameter name or use _ 
  // Wait, if _ gives an error, maybe omitting the name is not supported in the compiler being used.
  // Actually, we can just name it and use @unused
  def process(data: String, @unused config: Config): Unit =
    println(data)
