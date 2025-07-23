package learning

import spinal.core._
import spinal.core.sim._
import java.nio.file.{Files, Paths, StandardCopyOption}


// 仿真和生成Verilog的设置
object Config {
    def spinal = SpinalConfig(
        targetDirectory = "src/verilog",
        defaultClockDomainFrequency = FixedFrequency(50 MHz),
        defaultConfigForClockDomains = ClockDomainConfig(
            resetActiveLevel = HIGH
        ),
        onlyStdLogicVectorAtTopLevelIo = false
    )

    def sim = SimConfig.withConfig(spinal).withWave.workspacePath("/tmp/spinalvl")
}


object SimUtil {

    //  拷贝在/tmp生成的 wave.vcd 到目标目录，并按模块名重命名 
    def copyWaveFile(): Unit = {

        val wave = Paths.get(currentTestPath(), "wave.vcd")
        val waveStr = wave.toString
        val module = waveStr.split("/")(3)
        Files.copy(wave, Paths.get(s"sim/$module.vcd"), StandardCopyOption.REPLACE_EXISTING)

        println(s"波形已保存到: sim/$module.vcd")
    }

}