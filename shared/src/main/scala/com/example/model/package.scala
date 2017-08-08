package com.example

import java.nio.ByteBuffer

package object model {

  def bbToArrayBytes(buffer: ByteBuffer): Array[Byte] = {
    val data = Array.ofDim[Byte](buffer.remaining())
    buffer.get(data)
    data
  }
}
