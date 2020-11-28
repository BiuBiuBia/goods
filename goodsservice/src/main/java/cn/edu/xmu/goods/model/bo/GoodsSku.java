package cn.edu.xmu.goods.model.bo;

import cn.edu.xmu.goods.model.po.GoodsSkuPo;
import cn.edu.xmu.goods.model.po.GoodsSpuPo;
import cn.edu.xmu.ooad.model.VoObject;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GoodsSku implements VoObject, Serializable {

    private Long id;

    private Long goodsSpuId;

    private String skuSn;

    private String name;

    private Long originalPrice;

    private String configuration;

    private Long weight;

    private String imageUrl;

    private Integer inventory;

    private String detail;

    private Boolean disabled;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    public GoodsSku(GoodsSkuPo goodsSkuPo, GoodsSpuPo goodsSpuPo) {
        this.id = goodsSkuPo.getId();
        this.goodsSpuId = goodsSpuPo.getId();
        this.skuSn = goodsSkuPo.getSkuSn();
        this.name = goodsSkuPo.getName();
        this.originalPrice = goodsSkuPo.getOriginalPrice();
        this.weight = goodsSkuPo.getWeight();
        this.imageUrl = goodsSkuPo.getImageUrl();
        this.inventory = goodsSkuPo.getInventory();
        this.detail = goodsSkuPo.getDetail();
        this.disabled = goodsSkuPo.getDisabled() == 1;
        this.gmtCreate = goodsSkuPo.getGmtCreated();
        this.gmtModified = goodsSkuPo.getGmtModified();
    }

    @Override
    public Object createVo() {
        return null;
    }

    @Override
    public Object createSimpleVo() {
        return null;
    }

    /**
     * 构造函数
     *
     * @param po Po对象
     */
    public GoodsSku(GoodsSkuPo po) {
        this.id = po.getId();
        this.goodsSpuId = po.getGoodsSpuId();
        this.skuSn = po.getSkuSn();
        this.name = po.getName();
        this.originalPrice = po.getOriginalPrice();
        this.configuration = po.getConfiguration();
        this.weight = po.getWeight();
        this.imageUrl = po.getImageUrl();
        this.inventory = po.getInventory();
        this.detail = po.getDetail();
        this.disabled = po.getDisabled() == 1;
        this.gmtCreate = po.getGmtCreated();
        this.gmtModified = po.getGmtModified();
    }
}
