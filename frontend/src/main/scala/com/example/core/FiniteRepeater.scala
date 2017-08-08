package com.example.core

import scala.collection.mutable.{Map => MMap}

trait FiniteRepeater {
  private val actionsToRepeat: MMap[() => Unit, Int] = MMap.empty

  def orderExecution(repeat: Int, action: () => Unit): Unit = actionsToRepeat += action -> repeat

  def executeEpoch(): Unit = {
    actionsToRepeat.retain((_, timeLeft) => timeLeft > 0)
    actionsToRepeat.transform((action, timeLeft) => {
      action(); timeLeft - 1
    })
  }

}

