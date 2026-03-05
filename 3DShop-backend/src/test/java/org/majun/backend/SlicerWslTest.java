package org.majun.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.majun.backend.service.SlicerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;

@SpringBootTest
public class SlicerWslTest {

    @Autowired
    private SlicerService slicerService;

    // 直接使用 WSL 路径
    private final String testDir = "/home/tianhong/slicer/";

    @Test
    public void testNativeWslSlice() {
        String fileName = "1.stl"; // 请确保该文件在此目录下

        // 1. 验证 WSL 环境下的文件访问
        File stlFile = new File(testDir + fileName);
        System.out.println("检查 WSL 路径: " + stlFile.getAbsolutePath());
        Assertions.assertTrue(stlFile.exists(), "WSL 无法读取到 STL 文件，请确认路径或权限");

        try {
            // 2. 执行切片
            String gcodeName = slicerService.executeSlice(fileName, 0.2, 15,1.75);

            // 3. 验证生成结果
            File gcodeFile = new File(testDir + gcodeName);
            Assertions.assertTrue(gcodeFile.exists(), "切片任务结束，但未发现生成的 GCode");
            System.out.println("切片成功！文件存放在: " + gcodeFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("执行异常: " + e.getMessage());
        }
    }
}
