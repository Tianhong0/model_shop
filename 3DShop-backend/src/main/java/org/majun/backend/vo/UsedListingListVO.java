package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UsedListingListVO {

    private Long id;
    private Long sellerId;
    private String sellerNickname;
    private String title;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String conditionLevel;
    private String categoryName;
    private String location;
    private Integer status;
    private Integer viewCount;
    private Integer wantCount;
    private LocalDateTime createTime;
}
