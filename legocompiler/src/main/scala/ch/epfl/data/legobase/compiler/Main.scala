package ch.epfl.data
package legobase
package compiler

import deep._
import prettyprinter._
import optimization._
import pardis.optimization._
import ch.epfl.data.pardis.ir._
import pardis.ir.pardisTypeImplicits._

object Main extends LegoRunner {

  def main(args: Array[String]) {
    if (args.length < 3) {
      System.out.println("ERROR: Invalid number (" + args.length + ") of command line arguments!")
      System.out.println("USAGE: run <data_folder> <scaling_factor_number> <list of queries to run> <copy>?")
      System.out.println("     : data_folder_name should contain folders named sf0.1 sf1 sf2 sf4 etc")
      System.exit(0)
    }
    Config.checkResults = false

    run(args)
    // if (args.length == 4 && args(3) == "copy") {
    //   import java.io.{ File, FileInputStream, FileOutputStream }
    //   val src = new File("generator-out/lala.scala")
    //   val dest = new File("legocompiler/src/test/scala/Generated.scala")
    //   new FileOutputStream(dest) getChannel () transferFrom (
    //     new FileInputStream(src) getChannel, 0, Long.MaxValue)
    // }
  }

  def executeQuery(query: String): Unit = query match {
    case "Q1"   => query1()
    case "Q1_U" => query1_unoptimized()
    case "Q2"   => query2()
    case "Q3"   => query3()
    case "Q4"   => query4()
    case "Q5"   => query5()
    case "Q6"   => query6()
  }

  def query1_unoptimized() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q1(context.unit(1)) }

    // Lowering (e.g. case classes to records)
    val lowering = new LBLowering(context, context)
    val loweredBlock = lowering.lower(block)
    val parameterPromotion = new LBParameterPromotion(context)
    val operatorlessBlock = parameterPromotion.optimize(loweredBlock)

    // DCE
    val dce = new DCE(context)
    val dceBlock = dce.optimize(operatorlessBlock)

    // Convert Scala constructs to C
    val scalaToC = new ScalaConstructsToCTranformer(context)
    val transformedBlock = scalaToC.transformBlock(dceBlock)
    val scalaToC2 = new ScalaCollectionsToGLibTransfomer(context)
    val transformedBlock2 = scalaToC2.optimize(transformedBlock)

    val ir2Program = new { val IR = context } with IRToProgram {}

    System.out.println(transformedBlock2)

    val finalProgram = ir2Program.createProgram(transformedBlock2)

    val LegoGenerator = new LegoCGenerator(2, true)
    LegoGenerator.apply(finalProgram)
  }

  def compileQuery(initContext: LoweringLegoBase, block: pardis.ir.PardisBlock[Unit], number: Int, shallow: Boolean) {

    // println(block)
    // LegoGenerator.apply(block)

    val loweringContext = new LoweringLegoBase {}

    // val loweredBlock = lowering.transformProgram(block)
    val lowering = new LBLowering(initContext, loweringContext)
    val loweredBlock = lowering.lower(block)
    // val loweredBlock = block

    val parameterPromotion = new LBParameterPromotion(loweringContext)

    val operatorlessBlock = parameterPromotion.optimize(loweredBlock)
    // val operatorlessBlock = loweredBlock

    val dce = new DCE(loweringContext)

    val dceBlock = dce.optimize(operatorlessBlock)
    // val dceBlock = operatorlessBlock

    val partialyEvaluator = new PartialyEvaluate(loweringContext)

    val partialyEvaluated = partialyEvaluator.optimize(dceBlock)
    // val partialyEvaluated = dceBlock

    val ir2Program = new { val IR = loweringContext } with IRToProgram {
    }

    val finalProgram = ir2Program.createProgram(partialyEvaluated)

    println(finalProgram)
    val LegoGenerator = new LegoScalaGenerator(number, shallow)
    LegoGenerator.apply(finalProgram)
  }

  def query1() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q1(context.unit(1)) }
    compileQuery(context, block, 1, false)
  }

  def query2() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q2(context.unit(1)) }
    compileQuery(context, block, 2, false)
  }

  def query3() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q3(context.unit(1)) }
    compileQuery(context, block, 3, false)
  }

  def query4() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q4(context.unit(1)) }
    compileQuery(context, block, 4, false)
  }

  def query5() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q5(context.unit(1)) }
    compileQuery(context, block, 5, false)
  }

  def query6() {
    val context = new LoweringLegoBase {}
    val block = context.reifyBlock { context.Queries.Q6(context.unit(1)) }
    compileQuery(context, block, 6, false)
  }
}
