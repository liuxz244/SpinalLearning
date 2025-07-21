package axi

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.amba4.axilite.AxiLite4Utils.Axi4Rich
// 一个神秘的功能包，找不到源码，也不知道除了.toLite()还有哪些功能
import learning._


case class AxiToLite() extends Component {
    // 定义AXI接口设置
    val AxiConfig = Axi4Config(
        addressWidth = 32,
        dataWidth    = 32,
        idWidth      = 4
    )
    val AxiLiteConfig = AxiLite4Config(
        addressWidth = AxiConfig.addressWidth,
        dataWidth    = AxiConfig.dataWidth
    )

    // 定义AXI4接口(输入)和AxiLite接口(输出)
    val io = new Bundle {
        val Axi     = slave( Axi4(    AxiConfig    ))
        val AxiLite = master(AxiLite4(AxiLiteConfig))
    }

    // 接口转换
    io.Axi.toLite() >> io.AxiLite
}

object ToLiteVerilog extends App {
    Config.spinal.generateVerilog(AxiToLite())
}