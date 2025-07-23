package axi

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import learning._


case class AxiCrossbar() extends Component {
    // 定义AXI接口设置
    val masterAxiConfig = Axi4Config(
        addressWidth = 32,
        dataWidth    = 32,
        idWidth      = 4,
        useRegion    = false,
        useQos       = false
    )
    val slaveAxiConfig = masterAxiConfig.copy(idWidth = 5)
    // 注意从机的ID宽度应为主机ID宽度最大值加上log2up(主机数)，在这里是 5 = 4 + log2up(2)
    // log2up(x)：表示x应最少使用几位二进制数

    // 定义两个AXI4主机和两个从机接口
    val io = new Bundle {
        val masters = Vec(slave (Axi4(     masterAxiConfig)), 2)
        val slaves  = Vec(master(Axi4Shared(slaveAxiConfig)), 2)
    }

    // 创建交叉开关
    val crossbar = Axi4CrossbarFactory()

    // 添加从机及其对应的地址空间大小
    crossbar.addSlaves(
        io.slaves(0) -> (0x00000000L, 64 KiB),
        io.slaves(1) -> (0x00010000L, 64 KiB)
    )
    // 声明互联逻辑（每个主机可以访问哪些从机）
    crossbar.addConnections(
        io.masters(0) -> List(io.slaves(0), io.slaves(1)),
        io.masters(1) -> List(io.slaves(0), io.slaves(1))
    )

    // 生成交叉互联逻辑
    crossbar.build()
}


object CrossbarVerilog extends App {
    Config.spinal.generateVerilog(AxiCrossbar())
}