package org.majun.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SlicerService {

    private static final int LOG_TAIL_LINES = 60;

    @Value("${slicer.path}")
    private String path;

    @Value("${slicer.workDir}")
    private String workdir;

    @Value("${slicer.converterPath:}")
    private String converterPath;

    @Value("${slicer.timeoutSeconds:600}")
    private int timeoutSeconds;

    @Value("${slicer.threads:2}")
    private int threads;

    // .3mf 格式 Slic3r 支持较差，需要先转换为 STL
    private static final Set<String> NATIVE_SLICE_TYPES = Set.of("stl", "obj", "amf");

    private static final Set<String> COMMON_MODEL_TYPES = Set.of(
        "stl", "obj", "amf", "3mf", "glb", "gltf", "fbx", "dae", "ply", "off",
        "3ds", "x3d", "wrl", "step", "stp", "iges", "igs"
    );

    public String getWorkdir() {
        return workdir;
    }

    public String executeSlice(String modelFileName, Double layerHeight, Integer fillDensity, Double filamentDiameter) throws Exception {
        String ext = extensionOf(modelFileName);
        if (!COMMON_MODEL_TYPES.contains(ext)) {
            throw new RuntimeException("不支持的模型格式: " + ext);
        }

        // 记录需要清理的临时文件（如果有转换生成的 .stl 文件）
        String tempStlFile = null;
        String sliceInputFileName = modelFileName;

        // 格式转换：非原生支持的格式（如 .3mf）先转换为 STL
        if (!NATIVE_SLICE_TYPES.contains(ext)) {
            tempStlFile = convertToStl(modelFileName);
            sliceInputFileName = tempStlFile;
        }

        String outputGCode = toGcodeName(modelFileName);

        List<String> command = new ArrayList<>();
        command.add(path);
        command.add(workPath(sliceInputFileName));
        command.add("--layer-height");
        command.add(String.valueOf(layerHeight));
        command.add("--fill-density");
        command.add(fillDensity + "%");
        command.add("--filament-diameter");
        command.add(String.valueOf(filamentDiameter));
        command.add("--threads");
        command.add(String.valueOf(threads));
        command.add("--output");
        command.add(workPath(outputGCode));

        try {
            ProcessResult result = runProcess(command, "Slic3r", timeoutSeconds);
            if (result.exitCode() != 0) {
                throw new RuntimeException("切片失败，退出码: " + result.exitCode() + "\n" + result.combinedTail());
            }
            return outputGCode;
        } finally {
            // 切片完成（无论成功或失败），清理转换生成的临时 STL 文件
            if (tempStlFile != null) {
                cleanupTempFile(tempStlFile);
            }
        }
    }

    /**
     * 清理临时生成的 STL 文件
     */
    private void cleanupTempFile(String fileName) {
        try {
            File file = new File(workPath(fileName));
            if (file.exists() && file.delete()) {
                log.info("已清理临时文件: {}", fileName);
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败: {}", fileName, e);
        }
    }

    private String convertToStl(String sourceFileName) throws Exception {
        log.info("[SlicerService] 开始转换模型格式: {} → STL", sourceFileName);

        if (!StringUtils.hasText(converterPath)) {
            throw new RuntimeException("当前模型格式需要先转换为 STL，请在配置中设置 slicer.converterPath (如 /usr/bin/assimp)");
        }

        String sourcePath = workPath(sourceFileName);
        File sourceFile = new File(sourcePath);

        // 检查源文件是否存在
        if (!sourceFile.exists()) {
            throw new RuntimeException("源模型文件不存在: " + sourcePath);
        }
        if (sourceFile.length() == 0) {
            throw new RuntimeException("源模型文件为空: " + sourcePath);
        }
        log.info("[SlicerService] 源文件大小: {} bytes", sourceFile.length());

        String base = sourceFileName;
        int idx = sourceFileName.lastIndexOf('.');
        if (idx > 0) {
            base = sourceFileName.substring(0, idx);
        }
        String targetFileName = base + "__converted.stl";
        String targetPath = workPath(targetFileName);

        List<String> command = new ArrayList<>();
        command.add(converterPath);
        command.add("export");
        command.add(sourcePath);
        command.add(targetPath);

        log.info("[ModelConvert] 执行命令: {}", String.join(" ", command));

        ProcessResult result = runProcess(command, "ModelConvert", 60);
        if (result.exitCode() != 0) {
            throw new RuntimeException("模型转换 STL 失败，退出码: " + result.exitCode() + "\n" + result.combinedTail());
        }

        // 检查生成的文件
        File targetFile = new File(targetPath);
        if (!targetFile.exists() || targetFile.length() == 0) {
            throw new RuntimeException("assimp 转换失败，生成的 STL 文件为空: " + targetPath);
        }
        log.info("[SlicerService] 转换后的 STL 文件大小: {} bytes", targetFile.length());

        return targetFileName;
    }

    /**
     * 执行外部进程，异步读取 stdout/stderr，带超时保护。
     * <p>
     * 为什么必须异步处理输出流：
     * <p>
     * OS 为进程的 stdout 和 stderr 各分配了一个固定大小的管道缓冲区（Linux 默认 64KB）。
     * 如果父进程（JVM）不及时读取这两个缓冲区，任何一个写满后，子进程的 write() 系统调用
     * 就会阻塞——子进程被挂起，无法继续执行也无法退出。
     * <p>
     * 此时如果父进程正在 waitFor() 等待子进程退出：
     *   - 子进程：等父进程读走缓冲区数据才能继续写入并最终退出
     *   - 父进程：等子进程退出才会从 waitFor() 返回并去读数据
     *   → 双方互相等待 → 经典死锁
     * <p>
     * 因此必须在调用 waitFor() 之前，就启动独立线程持续消费 stdout 和 stderr，
     * 保证缓冲区始终有空间，子进程可以正常写入并最终退出。
     */
    private ProcessResult runProcess(List<String> command, String label, int timeoutSec) throws Exception {
        log.info("[{}] 执行命令: {}", label, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workdir));
        // 不合并 stderr→stdout，分别用独立线程异步读取，避免任一缓冲区满导致死锁

        Process process = pb.start();

        // 立即启动异步线程消费两个流——必须在 waitFor() 之前
        CompletableFuture<Deque<String>> stdoutFuture = CompletableFuture.supplyAsync(
            () -> drainStream(process.getInputStream(), label));
        CompletableFuture<Deque<String>> stderrFuture = CompletableFuture.supplyAsync(
            () -> drainStream(process.getErrorStream(), label + "-ERR"));

        // 带超时等待子进程退出
        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            // 超时：先 destroyForcibly 发送 SIGKILL，再短暂等待进程资源回收
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            String tail = safeCollectTail(stdoutFuture, stderrFuture);
            throw new RuntimeException(label + " 执行超时（" + timeoutSec + "s），进程已强制终止\n" + tail);
        }

        // 进程已退出，等待流读取线程收尾（进程退出后流会 EOF，线程很快结束）
        Deque<String> stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
        Deque<String> stderr = stderrFuture.get(5, TimeUnit.SECONDS);

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("[{}] 进程退出码: {}", label, exitCode);
        } else {
            log.info("[{}] 执行成功", label);
        }

        return new ProcessResult(exitCode, stdout, stderr);
    }

    /**
     * 持续读取 InputStream 直到 EOF，保留最后 LOG_TAIL_LINES 行用于错误诊断。
     * 该方法运行在独立线程中，保证子进程的管道缓冲区不会积压。
     */
    private Deque<String> drainStream(InputStream is, String label) {
        Deque<String> tail = new ArrayDeque<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[{}]: {}", label, line);
                appendTail(tail, line);
            }
        } catch (Exception e) {
            log.warn("[{}] 读取输出流异常", label, e);
        }
        return tail;
    }

    private String safeCollectTail(CompletableFuture<Deque<String>> stdoutF, CompletableFuture<Deque<String>> stderrF) {
        try {
            Deque<String> out = stdoutF.get(3, TimeUnit.SECONDS);
            Deque<String> err = stderrF.get(3, TimeUnit.SECONDS);
            return new ProcessResult(0, out, err).combinedTail();
        } catch (Exception e) {
            return "(无法获取进程输出)";
        }
    }

    private void appendTail(Deque<String> tail, String line) {
        if (tail.size() >= LOG_TAIL_LINES) {
            tail.pollFirst();
        }
        tail.offerLast(line);
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

    private String toGcodeName(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx > 0) {
            return fileName.substring(0, idx) + ".gcode";
        }
        return fileName + ".gcode";
    }

    private String workPath(String fileName) {
        if (!StringUtils.hasText(workdir)) {
            return fileName;
        }
        if (workdir.endsWith("/") || workdir.endsWith("\\")) {
            return workdir + fileName;
        }
        return workdir + "/" + fileName;
    }

    private record ProcessResult(int exitCode, Deque<String> stdout, Deque<String> stderr) {
        String combinedTail() {
            StringBuilder sb = new StringBuilder();
            if (stdout != null && !stdout.isEmpty()) {
                sb.append("--- stdout ---\n");
                stdout.forEach(line -> sb.append(line).append('\n'));
            }
            if (stderr != null && !stderr.isEmpty()) {
                sb.append("--- stderr ---\n");
                stderr.forEach(line -> sb.append(line).append('\n'));
            }
            return sb.isEmpty() ? "(无输出)" : sb.toString();
        }
    }
}
