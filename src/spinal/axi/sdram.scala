package axi

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axi.sim._
import spinal.lib.memory.sdram._ 
import spinal.lib.memory.sdram.sdr.sim.SdramModel 
import spinal.lib.memory.sdram.sdr.{Axi4SharedSdramCtrl, MT48LC16M16A2, SdramInterface, SdramTimings}
import java.nio.file.{Files, Paths, StandardCopyOption}
import spinal.lib.eda.altera.QuartusFlow
import learning._


object SharedSdramCtrlVerilog extends App {
    // 使用Axi4SharedSdramCtrl需要在Config里设置默认频率
    Config.spinal.generateVerilog(
        Axi4SharedSdramCtrl(
            axiDataWidth = 32,
            axiIdWidth   = 5,
            layout       = MT48LC16M16A2.layout,
            timing       = MT48LC16M16A2.timingGrade7,
            CAS          = 3
        )
    )
}


case class AxiSdramCtrl() extends Component{
    // 定义AXI接口
    val io = new Bundle {
        val axi = slave(Axi4(Axi4Config(32, 32, 5, useRegion = false, useLock = false, useCache = false, useQos = false, useProt = false)))
        val sdram = master(SdramInterface(MT48LC16M16A2.layout))
    }  // 使Quartus能测试引脚过多的模块：
    io.axi.addAttribute("altera_attribute", "-name VIRTUAL_PIN ON")

    // 创建一个片上RAM
    val sdramCtrl = Axi4SharedSdramCtrl(
        axiDataWidth = 32,
        axiIdWidth   = 5,
        layout       = MT48LC16M16A2.layout,
        timing       = MT48LC16M16A2.timingGrade7,
        CAS          = 3
    )

    // 将AXI接口连接到片上RAM
    io.axi.toShared() >> sdramCtrl.io.axi
    io.sdram <> sdramCtrl.io.sdram
}


object SdramCtrlVerilog extends App {
    Config.spinal.generateVerilog(AxiSdramCtrl())
}


object SdramCtrlSim {
    def main(args: Array[String]): Unit = {
        Config.sim.compile(new AxiSdramCtrl()).doSim { dut =>
            dut.clockDomain.forkStimulus(10)
            // 创建仿真用AXI主设备
            val axiMaster = Axi4Master(dut.io.axi, dut.clockDomain, "test")
            // 创建仿真用SDRAM模型
            val sdram = SdramModel(dut.io.sdram, MT48LC16M16A2.layout, dut.clockDomain)
            dut.clockDomain.waitSampling(10)

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

            // 等待几个时钟周期再结束仿真
            dut.clockDomain.waitSampling(10)
            SimUtil.copyWaveFile()  // 拷贝在/tmp生成的波形文件到目标目录
            simSuccess()
        }
    }
}