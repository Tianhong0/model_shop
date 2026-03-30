package org.majun.backend.service;

import org.majun.backend.dto.PrintJobAdjustRequest;
import org.majun.backend.dto.PrintJobDispatchRequest;
import org.majun.backend.dto.PrintJobQueryRequest;
import org.majun.backend.dto.PrintPrinterCreateRequest;
import org.majun.backend.dto.PrintPrinterUpdateRequest;
import org.majun.backend.vo.PageResult;
import org.majun.backend.vo.PrintJobEventVO;
import org.majun.backend.vo.PrintJobVO;
import org.majun.backend.vo.PrintPrinterVO;

import java.util.List;

public interface PrintJobService {

    void createAndDispatchFromPaidOrder(Long orderId);

    PageResult<PrintJobVO> adminList(PrintJobQueryRequest request);

    PageResult<PrintPrinterVO> listPrinters(Integer status, String keyword, Integer pageNum, Integer pageSize);

    void createPrinter(PrintPrinterCreateRequest request);

    void updatePrinter(PrintPrinterUpdateRequest request);

    void deletePrinter(Long id);

    void dispatchManual(PrintJobDispatchRequest request);

    void adjustJob(PrintJobAdjustRequest request);

    void stopJob(Long jobId);

    void retryJob(Long jobId);

    void syncAndBroadcastRunningJobs();

    List<PrintJobEventVO> listJobEvents(Long jobId, Integer limit);

    void runPipeline(Long jobId, Long preferredPrinterId);

    void deleteJob(Long jobId);
}
