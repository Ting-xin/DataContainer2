package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;

import cn.keking.service.cache.CacheService;
import cn.keking.service.impl.OtherFilePreviewImpl;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.WebUtils;
import fr.opensagres.xdocreport.core.io.IOUtils;
import io.mola.galimatias.GalimatiasParseException;
import jodd.io.NetUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static cn.keking.service.FilePreview.PICTURE_FILE_PREVIEW_PAGE;

/**
 * @author yudian-it
 */
@Controller
public class OnlinePreviewController {

    public static final String BASE64_DECODE_ERROR_MSG = "Base64解码失败，请检查你的 %s 是否采用 Base64 + urlEncode 双重编码了！";
    private final Logger logger = LoggerFactory.getLogger(OnlinePreviewController.class);

    private final FilePreviewFactory previewFactory;
    private final CacheService cacheService;
    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    public OnlinePreviewController(FilePreviewFactory filePreviewFactory, FileHandlerService fileHandlerService, CacheService cacheService, OtherFilePreviewImpl otherFilePreview) {
        this.previewFactory = filePreviewFactory;
        this.fileHandlerService = fileHandlerService;
        this.cacheService = cacheService;
        this.otherFilePreview = otherFilePreview;
    }

    /**
     * 根据下载链接 url 文件预览，该情况仅适用于url中有文件名的情况
     * @param url
     * @param model
     * @param req
     * @return
     */
    @RequestMapping(value = "/onlinePreview")
    public String onlinePreview(String url, Model model, HttpServletRequest req) {
        String fileUrl;
        try {
            fileUrl = new String(Base64.decodeBase64(url), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
        FileAttribute fileAttribute;
        String nameOrUid = fileUrl.split("/")[fileUrl.split("/").length - 1];
        if(nameOrUid.contains(".")) {
            fileAttribute = fileHandlerService.getFileAttribute(fileUrl, req);
        } else {
            fileAttribute = fileHandlerService.getUidFileAttribute(nameOrUid, fileUrl, req);
            fileUrl = fileAttribute.getUrl();
        }

        model.addAttribute("file", fileAttribute);
        FilePreview filePreview = previewFactory.get(fileAttribute);
        logger.info("预览文件url：{}，previewType：{}", fileUrl, fileAttribute.getType());
        return filePreview.filePreviewHandle(fileUrl, model, fileAttribute);
    }

    /*
     * @description:  测试用的，已经遗弃
     * @param: [url, model, req]
     * @return: java.lang.String
     * @author: Tian
     * @date: 2021/12/13 20:12
     */
    // @RequestMapping(value = "/preview/{uid}", method = RequestMethod.GET)
    // public String preview(@PathVariable String uid, Model model, HttpServletRequest req) {
    //     String fileUrl;
    //     try {
    //         uid = new String(Base64.decodeBase64(uid), StandardCharsets.UTF_8);
    //         fileUrl = "http://127.0.0.1:8082/data/" + uid;
    //     } catch (Exception ex) {
    //         String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "url");
    //         return otherFilePreview.notSupportedFile(model, errorMsg);
    //     }
    //     FileAttribute fileAttribute = fileHandlerService.getUidFileAttribute(uid, fileUrl, req);
    //     fileUrl = fileAttribute.getUrl();
    //     model.addAttribute("file", fileAttribute);
    //     FilePreview filePreview = previewFactory.get(fileAttribute);
    //
    //     // filePreview.fileDir = fileAttribute.getPath();
    //     logger.info("预览文件url：{}，previewType：{}", fileUrl, fileAttribute.getType());
    //     return filePreview.filePreviewHandle(fileUrl, model, fileAttribute);
    //
    // }

    /**
     * 图片预览，可以预览多个图片
     * @param urls
     * @param model
     * @param req
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/picturesPreview")
    public String picturesPreview(String urls, Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        String fileUrls;
        try {
            fileUrls = new String(Base64.decodeBase64(urls));
        } catch (Exception ex) {
            String errorMsg = String.format(BASE64_DECODE_ERROR_MSG, "urls");
            return otherFilePreview.notSupportedFile(model, errorMsg);
        }
        logger.info("预览文件url：{}，urls：{}", fileUrls, urls);
        // 抽取文件并返回文件列表
        String[] images = fileUrls.split("\\|");
        List<String> imgUrls = Arrays.asList(images);
        model.addAttribute("imgUrls", imgUrls);

        String currentUrl = req.getParameter("currentUrl");
        if (StringUtils.hasText(currentUrl)) {
            String decodedCurrentUrl = new String(Base64.decodeBase64(currentUrl));
            model.addAttribute("currentUrl", decodedCurrentUrl);
        } else {
            model.addAttribute("currentUrl", imgUrls.get(0));
        }
        return PICTURE_FILE_PREVIEW_PAGE;
    }

    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath  url
     * @param response response
     */
    @RequestMapping(value = "/getCorsFile", method = RequestMethod.GET)
    public void getCorsFile(String urlPath, HttpServletResponse response) {
        logger.info("下载跨域pdf文件url：{}", urlPath);
        try {
            URL url = WebUtils.normalizedURL(urlPath);
            byte[] bytes = NetUtil.downloadBytes(url.toString());
            IOUtils.write(bytes, response.getOutputStream());
        } catch (IOException | GalimatiasParseException e) {
            logger.error("下载跨域pdf文件异常，url：{}", urlPath, e);
        }
    }

    /**
     * 通过api接口入队
     *
     * @param url 请编码后在入队
     */
    @RequestMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        logger.info("添加转码队列url：{}", url);
        cacheService.addQueueTask(url);
        return "success";
    }

}
