package org.majun.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.majun.backend.common.exception.BusinessException;
import org.majun.backend.config.OctoPrintProperties;
import org.majun.backend.service.OctoPrintService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OctoPrintServiceImpl implements OctoPrintService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OctoPrintProperties octoPrintProperties;
    private final ObjectMapper objectMapper;

    private OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(octoPrintProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(octoPrintProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void verifyConnection(String baseUrl, String authHeaderKey, String authHeaderValue) {
        String normalized = trimSlash(baseUrl);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("打印机地址为空");
        }

        OkHttpClient client = buildClient();
        Request request = withAuth(new Request.Builder(), authHeaderKey, authHeaderValue)
                .url(normalized + "/api/version")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() == null ? "" : response.body().string();
                throw new BusinessException("打印机连接失败: " + response.code() + " " + err);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("无法连接 OctoPrint: " + normalized, ex);
        }
    }

    @Override
    public void uploadAndStartPrint(String baseUrl, String gcodeFileName, String gcodeAbsolutePath, String authHeaderKey, String authHeaderValue) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("打印机地址为空");
        }
        if (!StringUtils.hasText(gcodeAbsolutePath)) {
            throw new BusinessException("GCode 路径为空");
        }

        File file = new File(gcodeAbsolutePath);
        if (!file.exists()) {
            throw new BusinessException("GCode 文件不存在: " + gcodeAbsolutePath);
        }

        OkHttpClient client = buildClient();
        String normalized = trimSlash(baseUrl);

        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("select", "true")
                .addFormDataPart("print", "true")
                .addFormDataPart("file", gcodeFileName,
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();

        Request uploadReq = withAuth(new Request.Builder(), authHeaderKey, authHeaderValue)
                .url(normalized + "/api/files/local")
                .post(body)
                .build();

        try (Response response = client.newCall(uploadReq).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() == null ? "" : response.body().string();
                throw new BusinessException("上传并启动打印失败: " + response.code() + " " + err);
            }
        } catch (Exception ex) {
            throw new BusinessException("调用 OctoPrint 失败", ex);
        }
    }

    @Override
    public OctoPrintStatus fetchStatus(String baseUrl, String authHeaderKey, String authHeaderValue) {
        String normalized = trimSlash(baseUrl);
        OkHttpClient client = buildClient();

        Request jobReq = withAuth(new Request.Builder(), authHeaderKey, authHeaderValue)
                .url(normalized + "/api/job")
                .get()
                .build();

        Request printerReq = withAuth(new Request.Builder(), authHeaderKey, authHeaderValue)
                .url(normalized + "/api/printer")
                .get()
                .build();

        try (Response jobRes = client.newCall(jobReq).execute();
             Response printerRes = client.newCall(printerReq).execute()) {
            if (!jobRes.isSuccessful()) {
                throw new BusinessException("获取打印任务状态失败: " + jobRes.code());
            }
            if (!printerRes.isSuccessful()) {
                throw new BusinessException("获取打印机温度失败: " + printerRes.code());
            }

            JsonNode jobNode = objectMapper.readTree(Objects.requireNonNull(jobRes.body()).string());
            JsonNode printerNode = objectMapper.readTree(Objects.requireNonNull(printerRes.body()).string());

            String state = text(jobNode.at("/state"));
            BigDecimal progress = decimal(jobNode.at("/progress/completion"));
            Integer left = integer(jobNode.at("/progress/printTimeLeft"));
            BigDecimal printedSeconds = decimal(jobNode.at("/progress/printTime"));
            if ((left == null || left <= 0)
                    && progress != null
                    && progress.compareTo(BigDecimal.ZERO) > 0
                    && progress.compareTo(BigDecimal.valueOf(100)) < 0
                    && printedSeconds != null
                    && printedSeconds.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal estimatedTotalSeconds = printedSeconds
                        .multiply(BigDecimal.valueOf(100))
                        .divide(progress, 2, RoundingMode.HALF_UP);
                BigDecimal estimatedLeft = estimatedTotalSeconds.subtract(printedSeconds);
                if (estimatedLeft.compareTo(BigDecimal.ZERO) < 0) {
                    estimatedLeft = BigDecimal.ZERO;
                }
                left = estimatedLeft.setScale(0, RoundingMode.HALF_UP).intValue();
            }

            JsonNode tool0 = printerNode.at("/temperature/tool0");
            JsonNode bed = printerNode.at("/temperature/bed");

            return new OctoPrintStatus(
                    state,
                    progress,
                    left,
                    decimal(tool0.get("actual")),
                    decimal(tool0.get("target")),
                    decimal(bed.get("actual")),
                    decimal(bed.get("target"))
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("获取 OctoPrint 状态失败", ex);
        }
    }

    @Override
    public void cancelCurrent(String baseUrl, String authHeaderKey, String authHeaderValue) {
        String normalized = trimSlash(baseUrl);
        OkHttpClient client = buildClient();
        Request req = withAuth(new Request.Builder(), authHeaderKey, authHeaderValue)
                .url(normalized + "/api/job")
                .post(RequestBody.create("{\"command\":\"cancel\"}", JSON))
                .build();
        try (Response response = client.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() == null ? "" : response.body().string();
                throw new BusinessException("终止打印失败: " + response.code() + " " + err);
            }
        } catch (Exception ex) {
            throw new BusinessException("终止 OctoPrint 任务失败", ex);
        }
    }

    private Request.Builder withAuth(Request.Builder builder, String authHeaderKey, String authHeaderValue) {
        String key = StringUtils.hasText(authHeaderKey) ? authHeaderKey : octoPrintProperties.getAuthHeaderKey();
        String value = StringUtils.hasText(authHeaderValue) ? authHeaderValue : octoPrintProperties.getAuthHeaderValue();
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            builder.addHeader(key, value);
        }
        return builder;
    }

    private String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asText();
    }

    private Integer integer(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (!node.isNumber() && !node.isTextual()) {
            return null;
        }
        return node.asInt();
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (!node.isNumber() && !node.isTextual()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble()).setScale(2, RoundingMode.HALF_UP);
    }
}
