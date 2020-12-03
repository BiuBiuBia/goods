package cn.edu.xmu.goods.controller;

import cn.edu.xmu.goods.model.bo.GoodsSpu;
import cn.edu.xmu.goods.model.vo.*;
import cn.edu.xmu.goods.service.BrandService;
import cn.edu.xmu.goods.service.GoodsService;
import cn.edu.xmu.ooad.annotation.Audit;
import cn.edu.xmu.ooad.annotation.Depart;
import cn.edu.xmu.ooad.annotation.LoginUser;
import cn.edu.xmu.ooad.model.VoObject;
import cn.edu.xmu.ooad.util.Common;
import cn.edu.xmu.ooad.util.ResponseCode;
import cn.edu.xmu.ooad.util.ResponseUtil;
import cn.edu.xmu.ooad.util.ReturnObject;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限控制器
 **/
@Api(value = "商品服务", tags = "goods")
@RestController /*Restful的Controller对象*/
@RequestMapping(value = "/goods", produces = "application/json;charset=UTF-8")
public class GoodsController {

    private static final Logger logger = LoggerFactory.getLogger(GoodsController.class);

    @Autowired
    private BrandService brandService;

    @Autowired
    private GoodsService goodsService;


    @Autowired
    private HttpServletResponse httpServletResponse;

    /**
     * 查看商品spu的所有状态
     *
     * @return Object
     */
    @ApiOperation(value = "获得商品spu的所有状态")
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    @GetMapping("/spus/states")
    public Object getGoodsSpuState() {
        logger.debug("getGoodsSpuState");
        GoodsSpu.State[] states = GoodsSpu.State.class.getEnumConstants();
        List<SpuStateVo> spuStateVos = new ArrayList<>();
        for (GoodsSpu.State state : states) {
            spuStateVos.add(new SpuStateVo(state));
        }
        return ResponseUtil.ok(new ReturnObject<List>(spuStateVos).getData());
    }

    /**
     * 获得sku的详细信息
     *
     * @param `id`
     * @return Object
     */
    @ApiOperation(value = "获得sku的详细信息")
    @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "skuId", required = true)
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    @GetMapping("/skus/{id}")
    public Object getSku(@PathVariable Long id) {
        Object returnObject;
        ReturnObject sku = goodsService.findGoodsSkuById(id);
        logger.debug("getSku : skuId = " + id);
        if (!sku.getCode().equals(ResponseCode.RESOURCE_ID_NOTEXIST)) {
            returnObject = ResponseUtil.ok(sku.getData());
        } else {
            returnObject = Common.getNullRetObj(new ReturnObject<>(sku.getCode(), sku.getErrmsg()), httpServletResponse);
        }
        return returnObject;
    }

    /**
     * 查看一条商品SPU的详细信息
     *
     * @param id
     * @return Object
     */
    @ApiOperation(value = "查看一条商品spu的详细信息")
    @ApiImplicitParam(name = "id", required = true, dataType = "Integer", paramType = "path", value = "spuId")
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    @GetMapping("/spus/{id}")
    public Object getGoodsSpuBySpuId(@PathVariable Long id) {
        Object returnObject;
        ReturnObject spu = goodsService.findGoodsSpuById(id);
        logger.debug("findGoodsSpuById: " + spu.getData() + "code = " + spu.getCode());
        if (!spu.getCode().equals(ResponseCode.RESOURCE_ID_NOTEXIST)) {
            returnObject = ResponseUtil.ok(spu.getData());
        } else {
            returnObject = Common.getNullRetObj(new ReturnObject<>(spu.getCode(), spu.getErrmsg()), httpServletResponse);
        }
        return returnObject;
    }

    /**
     * 店家修改商品spu
     *
     * @param bindingResult 校验信息
     * @param spuInputVo    修改信息的SpuInputVo
     * @return Object
     */
    @ApiOperation(value = "店家修改商品spu")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "SpuInputVo", name = "spuInputVo", value = "可修改的用户信息", required = true),

    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    @Audit //需要认证
    @PutMapping("/shops/{shopId}/spus/{id}")
    public Object modifyGoodsSpu(@PathVariable Long shopId, @PathVariable Long id, @Validated @RequestBody SpuInputVo spuInputVo, BindingResult bindingResult, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("modifyGoodsSpu : shopId = " + shopId + " spuId = " + id + " vo = " + spuInputVo);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while modifyGoodsSpu shopId = " + shopId + " spuId = " + id);
            return returnObject;
        }
        //商家只能修改自家商品spu，shopId=0可以修改任意商品信息
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.modifySpuInfo(id, spuInputVo);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限修改本商品的信息");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 店家逻辑删除商品spu
     *
     * @param shopId 店铺id
     * @param id     spuId
     * @return Object
     */
    @ApiOperation(value = "店家删除商品spu")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @DeleteMapping("shops/{shopId}/spus/{id}")
    public Object deleteGoodsSpu(@PathVariable Long shopId, @PathVariable Long id, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("deleteGoodsSpu : shopId = " + shopId + " spuId = " + id);
        }
        //商家只能逻辑删除自家商品spu，shopId=0可以逻辑删除任意商品spu
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.deleteSpuById(id);
            return Common.decorateReturnObject(returnObj);
        } else {
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 店家商品上架
     *
     * @param id
     * @param shopId
     * @return Object
     */
    @ApiOperation(value = "店家商品上架")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @PutMapping("/shops/{shopId}/spus/{id}/onshelves")
    public Object putGoodsOnSales(@PathVariable Long shopId, @PathVariable Long id, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("putGoodsOnSales : shopId = " + shopId + " spuId = " + id);
        }
        //商家只能上架自家商品spu，shopId=0可以上架任意商品spu
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.putGoodsOnSaleById(id);
            return Common.decorateReturnObject(returnObj);
        } else {
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 店家商品下架
     *
     * @param id
     * @param shopId
     * @return Object
     */
    @ApiOperation(value = "店家商品下架")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @PutMapping("/shops/{shopId}/spus/{id}/offshelves")
    public Object putOffGoodsOnSales(@PathVariable Long shopId, @PathVariable Long id, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("putGoodsOnSales : shopId = " + shopId + " spuId = " + id);
        }
        //商家只能上架自家商品spu，shopId=0可以上架任意商品spu
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.putOffGoodsOnSaleById(id);
            return Common.decorateReturnObject(returnObj);
        } else {
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 管理员或店家逻辑删除sku
     *
     * @param shopId 店铺id
     * @param id     spuId
     * @return Object
     */
    @ApiOperation(value = "管理员或店家逻辑删除sku")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "skuId", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @DeleteMapping("shops/{shopId}/skus/{id}")
    public Object deleteGoodsSku(@PathVariable Long shopId, @PathVariable Long id, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("deleteGoodsSku : shopId = " + shopId + " skuId = " + id);
        }
        //商家只能逻辑删除自家商品sku，shopId=0可以逻辑删除任意商品sku
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.deleteSkuById(id);
            return Common.decorateReturnObject(returnObj);
        } else {
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 查看所有品牌
     *
     * @param page
     * @param pageSize
     * @return Object
     */
    @ApiOperation(value = "查看所有品牌")
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    @GetMapping("/brands")
    public Object getAllBrand(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize) {
        logger.debug("getAllBrand: page = " + page + "  pageSize =" + pageSize);
        page = (page == null) ? 1 : page;
        pageSize = (pageSize == null) ? 100 : pageSize;

        logger.debug("getAllBrand: page = " + page + "  pageSize =" + pageSize);
        ReturnObject<PageInfo<VoObject>> returnObject = brandService.findAllBrand(page, pageSize);
        return Common.getPageRetObject(returnObject);
    }

    /**
     * 管理员或店家修改商品sku
     *
     * @param bindingResult 校验信息
     * @param skuInputVo    修改信息的SkuInputVo
     * @return Object
     */
    @ApiOperation(value = "店家修改商品spu")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "SkuInputVo", name = "skuInputVo", value = "可修改的sku信息", required = true)
    })
    //@Audit //需要认证
    @PutMapping("shops/{shopId}/skus/{id}")
    public Object modifySku(@PathVariable Long shopId, @PathVariable Long id, @Validated @RequestBody SkuInputVo skuInputVo, BindingResult bindingResult, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("modifyGoodsSku : shopId = " + shopId + " skuId = " + id + " vo = " + skuInputVo);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while modifyGoodsSku shopId = " + shopId + " skuId = " + id);
            return returnObject;
        }
        //商家只能修改自家商品spu，shopId=0可以修改任意商品信息
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.modifySkuInfo(id, skuInputVo);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限修改本商品的信息");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 查看所有sku
     *
     * @return Object
     */
    @ApiOperation(value = "查询sku")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "shopId", value = "店铺id", required = false),
            @ApiImplicitParam(paramType = "query", dataType = "String", name = "skuSn", value = "skuSn", required = false),
            @ApiImplicitParam(paramType = "query", dataType = "String", name = "spuId", value = "spuId", required = false),
            @ApiImplicitParam(paramType = "query", dataType = "String", name = "spuSn", value = "spuSn", required = false),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "page", value = "页码", required = false),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "pageSize", value = "每页数目", required = false)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    @GetMapping("/sku")
    public Object getSkuList(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer shopId,
            @RequestParam(required = false) String skuSn,
            @RequestParam(required = false) String spuId,
            @RequestParam(required = false) String spuSn
    ) {
        Object object;
        if (page <= 0 || pageSize <= 0) {
            object = Common.getNullRetObj(new ReturnObject<>(ResponseCode.FIELD_NOTVALID), httpServletResponse);
        } else {
            ReturnObject<PageInfo<VoObject>> returnObject = goodsService.findSkuSimple(shopId, skuSn, page, pageSize, spuId, skuSn, spuSn);
            logger.debug("getSkuSimple = " + returnObject);
            object = Common.getPageRetObject(returnObject);
        }

        return object;
    }

    /**
     * 管理员失效商品价格浮动
     *
     * @param id
     * @param shopId
     * @return
     */
    @ApiOperation(value = "管理员失效商品价格浮动")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "floatPriceId", value = "价格浮动id", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @DeleteMapping("/shops/{shopId}/floatprice/{id}")
    public Object invalidFloatPrice(@PathVariable Long id, @PathVariable Long shopId, @Depart Long shopid, @LoginUser Long loginUserId) {
        if (logger.isDebugEnabled()) {
            logger.debug("invalidFloatPrice : shopId = " + shopId + " floatPriceId = " + id);
        }
        //商家只能修改自家商品价格浮动，shopId=0可以修改任意商品价格浮动
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.invalidFloatPriceById(id, loginUserId);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限修改本商品价格浮动的信息");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 管理员修改品牌
     *
     * @param shopId
     * @param id
     * @return Object
     */
    @ApiOperation(value = "管理员修改品牌")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "spuId", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "BrandInputVo", name = "brandInputVo", value = "可修改的品牌信息", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit
    @PutMapping("/shops/{shopId}/brands/{id}")
    public Object modifyBrand(@PathVariable Long shopId, @PathVariable Long id, @Validated @RequestBody BrandInputVo brandInputVo, BindingResult bindingResult, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("modifyBrand : shopId = " + shopId + " brandId = " + id + " vo = " + brandInputVo);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while modifyBrand shopId = " + shopId + " skuId = " + id);
            return returnObject;
        }
        //商家只能修改自家商品spu，shopId=0可以修改任意商品信息
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = brandService.modifyBrandInfo(id, brandInputVo);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限修改本品牌的信息");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 管理员删除品牌
     *
     * @param id
     * @param shopId
     * @return
     */
    @ApiOperation(value = "管理员删除品牌")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "brandId", value = "品牌id", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功")
    })
    //@Audit //需要认证
    @DeleteMapping("/shops/{shopId}/brands/{id}")
    public Object deleteBrand(@PathVariable Long id, @PathVariable Long shopId, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("deleteBrand : shopId = " + shopId + " brandId = " + id);
        }
        //商家只能修改自家品牌，shopId=0可以修改任意品牌
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = brandService.deleteBrandById(id);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限删除品牌");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    @ApiOperation(value = "管理员新增商品类目")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "种类 id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺 id", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "CategoryInputVo", name = "categoryInputVo", value = "类目详细信息", required = true),
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    //@Audit // 需要认证
    @PostMapping("/shops/{shopId}/categories/{id}/subcategories")
    public Object addCategory(@PathVariable Long id, @Validated @RequestBody CategoryInputVo categoryInputVo, BindingResult bindingResult, @PathVariable Long shopId, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("addCategory: CategoryId = " + id);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while addCategory CategoryId = ", id);
            return returnObject;
        }

        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject goodsCategory = goodsService.addCategory(id, categoryInputVo);
            returnObject = ResponseUtil.ok(goodsCategory.getData());
            return returnObject;
        } else {
            logger.error("无权限删除品牌");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 管理员修改商品类目信息
     *
     * @param id:             种类 id
     * @param categoryInputVo 类目详细信息
     * @return Object
     */
    @ApiOperation(value = "管理员修改商品类目信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "种类 id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺 id", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "CategoryInputVo", name = "CategoryInputVo", value = "类目详细信息", required = true),
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    //@Audit // 需要认证
    @PutMapping("/shops/{shopId}/categories/{id}")
    public Object modifyGoodsType(@PathVariable Long id, @Validated @RequestBody CategoryInputVo categoryInputVo, BindingResult bindingResult, @PathVariable Long shopId, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("modifyGoodsType: CategoryId = " + id);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while modifyGoodsType id = ", id);
            return returnObject;
        }
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObj = goodsService.modifyCategory(id, categoryInputVo);
            return Common.decorateReturnObject(returnObj);
        } else {
            logger.error("无权限修改品牌");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 管理员删除商品类目信息
     *
     * @param id: 种类 id
     * @return Object
     */
    @ApiOperation(value = "管理员删除商品类目信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "authorization", value = "Token", required = true, dataType = "String", paramType = "header"),
            @ApiImplicitParam(name = "id", required = true, dataType = "Integer", paramType = "path")
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    //@Audit // 需要认证
    @DeleteMapping("/shops/{shopId}/categories/{id}")
    public Object delCategory(@PathVariable Long id, @PathVariable Long shopId, BindingResult bindingResult, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("deleteCategory: CategoryId = " + id);
        }
        if (shopId.equals(shopid) || shopId == 0) {
            ReturnObject returnObject = goodsService.deleteCategoryById(id);
            return Common.decorateReturnObject(returnObject);
        } else {
            logger.error("无权限修改品牌");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 将SPU加入品牌
     *
     * @param id
     * @param shopId
     * @return
     */
    @ApiOperation(value = "将SPU加入品牌")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "spuId", value = "spuId", required = true),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "id", value = "品牌id", required = true)
    })
    @PostMapping("/shops/{shopId}/spus/{spuId}/brands/{id}")
    public Object spuAddBrand(@PathVariable Long shopId, @PathVariable Long spuId, @PathVariable Long id) {
        ReturnObject returnObject = goodsService.spuAddBrand(shopId, spuId, id);
        return Common.decorateReturnObject(returnObject);
    }

    /**
     * 管理员新增品牌
     *
     * @param id:          店铺 id
     * @param brandInputVo 品牌详细信息
     * @return Object
     */
    @ApiOperation(value = "管理员新增品牌")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "id", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "body", dataType = "brandInputVo", name = "brandInputVo", value = "品牌详细信息", required = true),
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    //@Audit // 需要认证
    @PostMapping("/shops/{id}/brands")
    public Object addBrand(@PathVariable Long id, @Validated @RequestBody BrandInputVo brandInputVo, BindingResult bindingResult, @Depart Long shopid) {
        if (logger.isDebugEnabled()) {
            logger.debug("addBrands: BrandId = " + id);
        }
        // 校验前端数据
        Object returnObject = Common.processFieldErrors(bindingResult, httpServletResponse);
        if (returnObject != null) {
            logger.info("incorrect data received while addBrand shopid = ", id);
            return returnObject;
        }
        if (id.equals(shopid) || id == 0) {
            ReturnObject brandCategory = brandService.addBrand(brandInputVo);
            returnObject = ResponseUtil.ok(brandCategory.getData());
            return returnObject;
        } else {
            logger.error("无权限新增品牌");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 将SPU删除品牌
     *
     * @param shopId
     * @param spuId
     * @param id
     * @return
     */
    @ApiOperation(value = "将SPU删除品牌")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "shopId", value = "店铺id", required = true),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "spuId", value = "spuId", required = true),
            @ApiImplicitParam(paramType = "query", dataType = "Integer", name = "id", value = "品牌id", required = true)
    })
    @DeleteMapping("/shops/{shopId}/spus/{spuId}/brands/{id}")
    public Object spuDeleteBrand(@PathVariable Long shopId, @PathVariable Long spuId, @PathVariable Long id) {
        ReturnObject returnObject = goodsService.spuDeleteBrand(shopId, spuId, id);
        return Common.decorateReturnObject(returnObject);
    }

    /**
     * 查询商品分类关系
     *
     * @param id 种类id
     * @return ReturnObject<List>
     */
    @ApiOperation(value = "查询商品分类关系")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, dataType = "Integer", paramType = "path")
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
    })
    //@Audit //需要认证
    @GetMapping("/categories/{id}/subcategories")
    public Object queryType(@PathVariable Long id) {
        ReturnObject<List> returnObject = goodsService.selectCategories(id);
        return Common.decorateReturnObject(returnObject);
    }

    /**
     * sku上传图片
     *
     * @param shopId
     * @param id
     * @return
     */
    @ApiOperation(value = "sku上传图片")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺 id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "skuId", value = "sku id", required = true),
            @ApiImplicitParam(paramType = "formData", dataType = "file", name = "img", value = "文件", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
            @ApiResponse(code = 506, message = "该目录文件夹没有写入的权限"),
            @ApiResponse(code = 508, message = "图片格式不正确"),
            @ApiResponse(code = 509, message = "图片大小超限")
    })
    @Audit
    @PostMapping("/shops/{shopId}/skus/{id}/uploadImg")
    public Object uploadSkuImg(@PathVariable Long shopId, @PathVariable Long id, @RequestParam("img") MultipartFile multipartFile,
                               @LoginUser @ApiIgnore Long userId, @Depart Long shopid) {
        logger.debug("uploadSkuImg: shopId = " + shopId + " skuId = " + id + " img :" + multipartFile.getOriginalFilename());
        if (shopId.equals(shopid) || shopid == 0) {
            ReturnObject returnObject = goodsService.uploadSkuImg(shopId, id, multipartFile);
            return Common.getNullRetObj(returnObject, httpServletResponse);
        } else {
            logger.error("无权限上传sku照片");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * spu上传图片
     *
     * @param shopId
     * @param id
     * @return
     */
    @ApiOperation(value = "spu上传图片")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺 id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "spuId", value = "spu id", required = true),
            @ApiImplicitParam(paramType = "formData", dataType = "file", name = "img", value = "文件", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
            @ApiResponse(code = 506, message = "该目录文件夹没有写入的权限"),
            @ApiResponse(code = 508, message = "图片格式不正确"),
            @ApiResponse(code = 509, message = "图片大小超限")
    })
    @Audit
    @PostMapping("/shops/{shopId}/spus/{id}/uploadImg")
    public Object uploadSpuImg(@PathVariable Long shopId, @PathVariable Long id, @RequestParam("img") MultipartFile multipartFile,
                               @LoginUser @ApiIgnore Long userId, @Depart Long shopid) {
        logger.debug("uploadSpuImg: shopId = " + shopId + " spuId = " + id + " img :" + multipartFile.getOriginalFilename());
        if (shopId.equals(shopid) || shopid == 0) {
            ReturnObject returnObject = goodsService.uploadSpuImg(shopId, id, multipartFile);
            return Common.getNullRetObj(returnObject, httpServletResponse);
        } else {
            logger.error("无权限上传spu照片");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }

    /**
     * 品牌上传图片
     *
     * @param shopId
     * @param id
     * @return
     */
    @ApiOperation(value = "spu上传图片")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "header", dataType = "String", name = "authorization", value = "Token", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "shopId", value = "店铺 id", required = true),
            @ApiImplicitParam(paramType = "path", dataType = "Long", name = "brandId", value = "brand id", required = true),
            @ApiImplicitParam(paramType = "formData", dataType = "file", name = "img", value = "文件", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = 0, message = "成功"),
            @ApiResponse(code = 506, message = "该目录文件夹没有写入的权限"),
            @ApiResponse(code = 508, message = "图片格式不正确"),
            @ApiResponse(code = 509, message = "图片大小超限")
    })
    @Audit
    @PostMapping("/shops/{shopId}/brand/{id}/uploadImg")
    public Object uploadBrandImg(@PathVariable Long shopId, @PathVariable Long id, @RequestParam("img") MultipartFile multipartFile,
                                 @LoginUser @ApiIgnore Long userId, @Depart Long shopid) {
        logger.debug("uploadBrandImg: shopId = " + shopId + " brandId = " + id + " img :" + multipartFile.getOriginalFilename());
        if (shopId.equals(shopid) || shopid == 0) {
            ReturnObject returnObject = goodsService.uploadBrandImg(shopId, id, multipartFile);
            return Common.getNullRetObj(returnObject, httpServletResponse);
        } else {
            logger.error("无权限上传品牌照片");
            return new ReturnObject<>(ResponseCode.FIELD_NOTVALID);
        }
    }
}