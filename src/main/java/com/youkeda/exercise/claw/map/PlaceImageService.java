package com.youkeda.exercise.claw.map;

import com.youkeda.exercise.claw.map.provider.CachingPlaceImageProvider;
import com.youkeda.exercise.claw.map.provider.PlaceImageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 地点图片搜索服务
 *
 * <p>根据地点关键词和城市获取相关图片。
 * 通过 {@link PlaceImageProvider} 接口委托实际图片来源，
 * 并使用 {@link CachingPlaceImageProvider} 做内存缓存。
 *
 * <p>上层 {@link PlaceImageFunction} 调用此服务，不直接暴露给 LLM。
 */
@Service
public class PlaceImageService {

    private static final Logger log = LoggerFactory.getLogger(PlaceImageService.class);

    /** 缓存 TTL（秒）：默认 10 分钟 */
    private static final long CACHE_TTL_SECONDS = 600;

    private final CachingPlaceImageProvider cachingProvider;

    /**
     * 构造函数：接收实际图片来源，自动包装缓存。
     */
    public PlaceImageService(PlaceImageProvider provider) {
        this.cachingProvider = new CachingPlaceImageProvider(provider, CACHE_TTL_SECONDS);
        log.info("PlaceImageService 初始化 | delegate={} | cacheTtl={}s",
                provider.getClass().getSimpleName(), CACHE_TTL_SECONDS);
    }

    /**
     * 根据关键词和城市搜索地点图片 URL
     *
     * @param keyword 地点关键词，如"西湖"、"团建基地"
     * @param city    城市名称，如"杭州"
     * @return 图片 URL 列表（可能为空）
     * @deprecated 请使用 {@link #searchImageBytes(String, String)} 替代
     */
    @Deprecated
    public List<String> searchImages(String keyword, String city) {
        log.info("地点图片搜索(URL) | keyword={} | city={}", keyword, city);
        List<String> urls = cachingProvider.searchImages(keyword, city);
        if (urls == null) {
            urls = Collections.emptyList();
        }
        log.info("地点图片搜索完成(URL) | keyword={} | city={} | urls={}", keyword, city, urls.size());
        return urls;
    }

    /**
     * 根据关键词和城市搜索地点图片并下载为字节数组
     *
     * @param keyword 地点关键词，如"西湖"、"团建基地"
     * @param city    城市名称，如"杭州"
     * @return 图片字节列表（可能为空）
     */
    public List<byte[]> searchImageBytes(String keyword, String city) {
        log.info("地点图片搜索 | keyword={} | city={}", keyword, city);

        List<byte[]> imageBytes = cachingProvider.searchImageBytes(keyword, city);
        if (imageBytes == null) {
            imageBytes = Collections.emptyList();
        }

        log.info("地点图片搜索完成 | keyword={} | city={} | images={}",
                keyword, city, imageBytes.size());
        return imageBytes;
    }
}
