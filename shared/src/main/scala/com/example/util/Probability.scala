package com.example.util

import scala.language.implicitConversions
import scala.util.Random

trait Probability {
  self =>

  def and(other: Probability) = new Probability {
    override def check(implicit random: Random): Boolean = self.check && other.check
  }

  def or(other: Probability) = new Probability {
    override def check(implicit random: Random): Boolean = self.check || other.check
  }

  def unary_! = new Probability {
    override def check(implicit random: Random): Boolean = !self.check
  }

  def check(implicit random: Random): Boolean

}

object Probability {

  object Implicits {
     implicit val global = new Random()
  }

  private case class Finite(range: Int, part: Int) extends Probability {
    override def check(implicit random: Random): Boolean = random.nextInt(range) < part
  }

  private case class FloatingPoint(value: Double) extends Probability {
    override def check(implicit random: Random): Boolean = random.nextDouble() < value
  }

  class IntExtension(private val numb: Int) extends AnyVal {
    def percentProbability: Probability = Finite(100, numb)

    def probability(of: Int): Probability = Finite(of, numb)
  }

  class DoubleExtension(private val numb: Double) extends AnyVal {
    def normal: Probability = FloatingPoint(numb)
  }

  implicit def intToExtension(numb: Int): IntExtension = new IntExtension(numb)

  implicit def doubleToExtension(numb: Double): DoubleExtension = new DoubleExtension(numb)

}
