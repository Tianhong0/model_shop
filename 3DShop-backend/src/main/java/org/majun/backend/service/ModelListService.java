package org.majun.backend.service;

import org.majun.backend.dto.ModelListAdminQueryRequest;
import org.majun.backend.dto.ModelListBatchRequest;
import org.majun.backend.dto.ModelListCreateRequest;
import org.majun.backend.dto.ModelListInteractionMyQueryRequest;
import org.majun.backend.dto.ModelListInteractionToggleRequest;
import org.majun.backend.dto.ModelListItemAddRequest;
import org.majun.backend.dto.ModelListItemRemoveRequest;
import org.majun.backend.dto.ModelListItemSortRequest;
import org.majun.backend.dto.ModelListMyQueryRequest;
import org.majun.backend.dto.ModelListQueryRequest;
import org.majun.backend.dto.ModelListStatusUpdateRequest;
import org.majun.backend.dto.ModelListUpdateRequest;
import org.majun.backend.vo.ModelListAdminVO;
import org.majun.backend.vo.ModelListInteractionToggleVO;
import org.majun.backend.vo.ModelListShareDetailVO;
import org.majun.backend.vo.ModelListShareListVO;
import org.majun.backend.vo.ModelListStatisticsVO;
import org.majun.backend.vo.PageResult;

/**
 * 模型列表服务接口
 */
public interface ModelListService {

    Long createList(ModelListCreateRequest request, Long userId);

    void updateList(ModelListUpdateRequest request, Long userId);

    void deleteList(Long listId, Long userId);

    void publishList(Long listId, Long userId);

    void addItems(ModelListItemAddRequest request, Long userId);

    void removeItem(ModelListItemRemoveRequest request, Long userId);

    void sortItems(ModelListItemSortRequest request, Long userId);

    PageResult<ModelListShareListVO> getPublicPage(ModelListQueryRequest request, Long currentUserId);

    ModelListShareDetailVO getDetail(Long listId, Long currentUserId);

    ModelListInteractionToggleVO toggleInteraction(ModelListInteractionToggleRequest request, Long userId);

    PageResult<ModelListShareListVO> getMyLists(ModelListMyQueryRequest request, Long userId);

    PageResult<ModelListShareListVO> getMyInteractionLists(ModelListInteractionMyQueryRequest request, Long userId);

    // ===== 管理端方法 =====

    PageResult<ModelListAdminVO> getAdminPage(ModelListAdminQueryRequest request);

    ModelListAdminVO getAdminDetail(Long listId);

    void updateStatus(ModelListStatusUpdateRequest request);

    void adminDeleteList(Long listId);

    void batchUpdateStatus(ModelListBatchRequest request);

    void batchDelete(ModelListBatchRequest request);

    ModelListStatisticsVO getStatistics();
}
