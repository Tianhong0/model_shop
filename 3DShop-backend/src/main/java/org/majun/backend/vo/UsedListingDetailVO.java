package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UsedListingDetailVO {

    private Long id;
    private Long sellerId;
    private String sellerNickname;
    private String sellerAvatar;
    private String title;
    private String description;
    private String coverUrl;
    private List<String> imageUrls;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String conditionLevel;
    private String categoryName;
    private String location;
    private Integer status;
    private Integer viewCount;
    private Integer wantCount;
    private Boolean owner;
    private Boolean canBuy;
    private List<UsedOfferVO> offers;
    private LocalDateTime createTime;
}
