package org.majun.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

@Service
public class SlicerService {

    private static final int LOG_TAIL_LINES = 60;

    @Value("${slicer.path}")
    private String path;

    @Value("${slicer.workDir}")
    private String workdir;

        @Value("${slicer.converterPath:}")
        private String converterPath;

        private static final Set<String> NATIVE_SLICE_TYPES = Set.of("stl", "obj", "amf", "3mf");

        private static final Set<String> COMMON_MODEL_TYPES = Set.of(
            "stl", "obj", "amf", "3mf", "glb", "gltf", "fbx", "dae", "ply", "off", "3ds", "x3d", "wrl", "step", "stp", "iges", "igs"
        );

    public String getWorkdir() {
        return workdir;
    }



    public String executeSlice(String modelFileName, Double layerHeight, Integer fillDensity, Double filamentDiameter) throws Exception {
        String ext = extensionOf(modelFileName);
        if (!COMMON_MODEL_TYPES.contains(ext)) {
            throw new RuntimeException("不支持的模型格式: " + ext);
        }

        String sliceInputFileName = modelFileName;
        if (!NATIVE_SLICE_TYPES.contains(ext)) {
            sliceInputFileName = convertToStl(modelFileName);
        }

        String outputGCode = toGcodeName(modelFileName);
        System.out.println("Debug: workDir is " + workdir);
        // 构建 Linux 原生命令
        List<String> command = new ArrayList<>();
        command.add(path);
        command.add(workPath(sliceInputFileName));
        command.add("--layer-height");
        command.add(String.valueOf(layerHeight));
        command.add("--fill-density");
        command.add(fillDensity + "%");
        command.add("--filament-diameter");
        command.add(String.valueOf(filamentDiameter));
        command.add("--output");
        command.add(workPath(outputGCode));

        ProcessBuilder pb = new ProcessBuilder(command);
        // 重要：设置工作目录路径
        pb.directory(new File(workdir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        Deque<String> tail = new ArrayDeque<>();

        // 打印实时日志，方便调试
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[WSL Slic3r]: " + line);
                appendTail(tail, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            return outputGCode;
        } else {
            throw new RuntimeException("WSL 原生切片失败，退出码: " + exitCode + "\n" + buildTailText(tail));
        }
    }

    private String convertToStl(String sourceFileName) throws Exception {
        if (!StringUtils.hasText(converterPath)) {
            throw new RuntimeException("当前模型格式需要先转换为 STL，请在配置中设置 slicer.converterPath (如 /usr/bin/assimp)");
        }

        String base = sourceFileName;
        int idx = sourceFileName.lastIndexOf('.');
        if (idx > 0) {
            base = sourceFileName.substring(0, idx);
        }
        String targetFileName = base + "__converted.stl";

        List<String> command = new ArrayList<>();
        command.add(converterPath);
        command.add("export");
        command.add(workPath(sourceFileName));
        command.add(workPath(targetFileName));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workdir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        Deque<String> tail = new ArrayDeque<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[MODEL-CONVERT]: " + line);
                appendTail(tail, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("模型转换 STL 失败，退出码: " + exitCode + "\n" + buildTailText(tail));
        }
        return targetFileName;
    }

    private void appendTail(Deque<String> tail, String line) {
        if (tail.size() >= LOG_TAIL_LINES) {
            tail.pollFirst();
        }
        tail.offerLast(line);
    }

    private String buildTailText(Deque<String> tail) {
        if (tail == null || tail.isEmpty()) {
            return "日志为空";
        }
        StringBuilder builder = new StringBuilder("最近日志:\n");
        for (String line : tail) {
            builder.append(line).append('\n');
        }
        return builder.toString();
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
}
