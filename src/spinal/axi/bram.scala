package axi

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axi.sim._
import spinal.lib.eda.altera.QuartusFlow
import learning._


case class AxiOnChipRam() extends Component{
    // 定义AXI接口
    val io = new Bundle {
        val axi = slave(Axi4(Axi4Config(32, 32, 5, useRegion = false, useQos = false)))
    }  // 使Quartus能测试引脚过多的模块：
    io.axi.addAttribute("altera_attribute", "-name VIRTUAL_PIN ON")

    // 创建一个片上RAM
    val ram = Axi4SharedOnChipRam(
        dataWidth = 32,
        byteCount = 4096,
        idWidth   = 5
    )

    // 将AXI接口连接到片上RAM
    io.axi.toShared() >> ram.io.axi
}

object OnChipRamSim {
    def main(args: Array[String]): Unit = {
        SimUtil.withWaveCopy(new AxiOnChipRam()) { dut =>
            // 创建仿真用AXI主设备
            val axiMaster = Axi4Master(dut.io.axi, dut.clockDomain, "test")
            // 时钟复位
            dut.clockDomain.forkStimulus(5)
            dut.clockDomain.waitSampling(5)

            // 写测试数据到片上RAM的地址0x100（示例地址）
            val writeAddress = 0x100
            val writeData: List[Byte] = List(1, 2, 3, 4, 5, 6, 7, 8)   // 写入8字节数据
            println(" \n")
            println(s"开始向地址 0x${writeAddress.toHexString} 写入数据")
            axiMaster.write(writeAddress, writeData)
            println("写入完成，开始读取数据")

            // 读出刚才写入的数据
            val readData = axiMaster.read(writeAddress, writeData.length)
            println(s"读取到的数据: ${readData.map("%02x".format(_)).mkString(" ")}")
            // 验证读数据是否和写入的数据一致
            assert(readData == writeData, "读取的数据与写入的数据不一致！")
            println("读取的数据与写入的数据一致，测试通过。")
            println(" \n")
        }
    }
}


object OnChipRamQuartus extends App {
    val report = QuartusFlow(
        quartusPath="/opt/quartus/24.1Lite/quartus/bin",
        workspacePath="/tmp/spinalqr/",
        toplevelPath="src/verilog/AxiOnChipRam.v",
        family="Cyclone V",
        device="5CEBA2F23C8",
        frequencyTarget = 54 MHz
    );  println(" \n")
    println("Quartus编译报告:")
    println(report)
}


object OnChipRamVerilog extends App {
    Config.spinal.generateVerilog(AxiOnChipRam())
}