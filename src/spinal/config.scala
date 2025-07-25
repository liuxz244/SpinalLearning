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
    /**
        * 编译并仿真一个模块，仿真结束后自动复制 wave.vcd 到指定目录。
        *
        * @param gen     要仿真的 Component 生成函数
        * @param outDir  波形存放目录（默认 "sim"）
        * @param body    在 doSim {} 中执行的测试逻辑
        */
    def withWaveCopy[T <: Component](
        gen: => T,
        outDir: String = "sim"
    ) (body: T => Unit): Unit = {
        var wavePath: java.nio.file.Path = null
        var moduleName: String = ""
        // 1) 编译并仿真
        Config.sim.compile(gen).doSim { dut =>
            // 用户测试逻辑
            body(dut)

            // 标记仿真成功，并记录波形文件位置
            wavePath = Paths.get(currentTestPath(), "wave.vcd")
            val waveStr = wavePath.toString
            moduleName  = waveStr.split("/")(3)
            simSuccess()
        }

        // 2) 确保目标目录存在
        Files.createDirectories(Paths.get(outDir))

        // 3) 复制波形到 outDir/{moduleName}.vcd
        val destPath = Paths.get(outDir, s"$moduleName.vcd")
        Files.copy(wavePath, destPath, StandardCopyOption.REPLACE_EXISTING)
        println(s"波形已保存到: $destPath")
    }
}