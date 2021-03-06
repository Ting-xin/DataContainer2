package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.dao.BulkDataLinkDao;
import cn.keking.dao.DataListComDao;
import cn.keking.entity.BulkDataLink;
import cn.keking.entity.DataListCom;
import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.cache.CacheService;
import cn.keking.utils.KkFileUtils;
import cn.keking.utils.WebUtils;
import com.aspose.cad.Color;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.sun.star.task.ErrorCodeIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yudian-it
 * @date 2017/11/13
 */
@Component
public class FileHandlerService {

    @Autowired
    BulkDataLinkDao bulkDataLinkDao;

    @Autowired
    DataListComDao dataListComDao;

    private final Logger logger = LoggerFactory.getLogger(FileHandlerService.class);

    private static final String DEFAULT_CONVERTER_CHARSET = System.getProperty("sun.jnu.encoding");
    private String fileDir = ConfigConstants.getFileDir();
    private final CacheService cacheService;

    @Value("${server.tomcat.uri-encoding:UTF-8}")
    private String uriEncoding;


    public FileHandlerService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * @return ???????????????????????????(??????)
     */
    public Map<String, String> listConvertedFiles() {
        return cacheService.getPDFCache();
    }

    /**
     * @return ?????????????????????????????????????????????
     */
    public String getConvertedFile(String key) {
        return cacheService.getPDFCache(key);
    }

    /**
     * @param key pdf????????????
     * @return ??????pdf??????????????????????????????????????????
     */
    public Integer getConvertedPdfImage(String key) {
        return cacheService.getPdfImageCache(key);
    }


    /**
     * ???????????????????????????
     *
     * @param path ???????????????C:\Users\yudian-it\Downloads
     * @return ?????????
     */
    public String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    /**
     * ??????????????????
     *
     * @param absolutePath ????????????
     * @return ????????????
     */
    public String getRelativePath(String absolutePath) {
        return absolutePath.substring(fileDir.length());
    }

    /**
     * ???????????????PDF??????
     *
     * @param fileName pdf?????????
     * @param value    ??????????????????
     */
    public void addConvertedFile(String fileName, String value) {
        cacheService.putPDFCache(fileName, value);
    }

    /**
     * ??????????????????????????????
     *
     * @param pdfFilePath pdf??????????????????
     * @param num         ????????????
     */
    public void addConvertedPdfImage(String pdfFilePath, int num) {
        cacheService.putPdfImageCache(pdfFilePath, num);
    }

    /**
     * ??????redis???????????????????????????
     *
     * @param fileKey fileKey
     * @return ??????????????????url??????
     */
    public List<String> getImgCache(String fileKey) {
        return cacheService.getImgCache(fileKey);
    }

    /**
     * ??????redis???????????????????????????
     *
     * @param fileKey fileKey
     * @param imgs    ??????????????????url??????
     */
    public void putImgCache(String fileKey, List<String> imgs) {
        cacheService.putImgCache(fileKey, imgs);
    }

    /**
     * ?????????????????????????????????(??????????????????)
     *
     * @param outFilePath ??????????????????
     */
    public void doActionConvertedFile(String outFilePath) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(outFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CONVERTER_CHARSET))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (line.contains("charset=gb2312")) {
                    line = line.replace("charset=gb2312", "charset=utf-8");
                }
                sb.append(line);
            }
            // ??????sheet?????????
            sb.append("<script src=\"js/jquery-3.0.0.min.js\" type=\"text/javascript\"></script>");
            sb.append("<script src=\"js/excel.header.js\" type=\"text/javascript\"></script>");
            sb.append("<link rel=\"stylesheet\" href=\"bootstrap/css/bootstrap.min.css\">");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ??????????????????
        try (FileOutputStream fos = new FileOutputStream(outFilePath);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  pdf???????????????jpg?????????
     * @param pdfFilePath pdf????????????
     * @param pdfName pdf????????????
     * @param baseUrl ??????????????????
     * @return ??????????????????
     */
    public List<String> pdf2jpg(String pdfFilePath, String pdfName, String baseUrl) {
        List<String> imageUrls = new ArrayList<>();
        Integer imageCount = this.getConvertedPdfImage(pdfFilePath);
        String imageFileSuffix = ".jpg";
        String pdfFolder = pdfName.substring(0, pdfName.length() - 4);
        String urlPrefix;
        try {
            urlPrefix = baseUrl + URLEncoder.encode(pdfFolder, uriEncoding).replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException", e);
            urlPrefix = baseUrl + pdfFolder;
        }
        if (imageCount != null && imageCount > 0) {
            for (int i = 0; i < imageCount; i++) {
                imageUrls.add(urlPrefix + "/" + i + imageFileSuffix);
            }
            return imageUrls;
        }
        try {
            File pdfFile = new File(pdfFilePath);
            PDDocument doc = PDDocument.load(pdfFile);
            int pageCount = doc.getNumberOfPages();
            PDFRenderer pdfRenderer = new PDFRenderer(doc);

            int index = pdfFilePath.lastIndexOf(".");
            String folder = pdfFilePath.substring(0, index);

            File path = new File(folder);
            if (!path.exists() && !path.mkdirs()) {
                logger.error("?????????????????????{}??????????????????????????????????????????", folder);
            }
            String imageFilePath;
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                imageFilePath = folder + File.separator + pageIndex + imageFileSuffix;
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 105, ImageType.RGB);
                ImageIOUtil.writeImage(image, imageFilePath, 105);
                imageUrls.add(urlPrefix + "/" + pageIndex + imageFileSuffix);
            }
            doc.close();
            this.addConvertedPdfImage(pdfFilePath, pageCount);
        } catch (IOException e) {
            logger.error("Convert pdf to jpg exception, pdfFilePath???{}", pdfFilePath, e);
        }
        return imageUrls;
    }

    /**
     * cad?????????pdf
     * @param inputFilePath cad????????????
     * @param outputFilePath pdf??????????????????
     * @return ??????????????????
     */
    public boolean cadToPdf(String inputFilePath, String outputFilePath)  {
        com.aspose.cad.Image cadImage = com.aspose.cad.Image.load(inputFilePath);
        CadRasterizationOptions cadRasterizationOptions = new CadRasterizationOptions();
        cadRasterizationOptions.setLayouts(new String[]{"Model"});
        cadRasterizationOptions.setNoScaling(true);
        cadRasterizationOptions.setBackgroundColor(Color.getWhite());
        cadRasterizationOptions.setPageWidth(cadImage.getWidth());
        cadRasterizationOptions.setPageHeight(cadImage.getHeight());
        cadRasterizationOptions.setPdfProductLocation("center");
        cadRasterizationOptions.setAutomaticLayoutsScaling(true);
        cadRasterizationOptions.setDrawType(CadDrawTypeMode.UseObjectColor);
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setVectorRasterizationOptions(cadRasterizationOptions);
        File outputFile = new File(outputFilePath);
        OutputStream stream;
        try {
            stream = new FileOutputStream(outputFile);
            cadImage.save(stream, pdfOptions);
            cadImage.close();
            return true;
        } catch (FileNotFoundException e) {
            logger.error("PDFFileNotFoundException???inputFilePath???{}", inputFilePath, e);
            return false;
        }
    }

    /**
     * ??????????????????
     *
     * @param url url
     * @return ????????????
     */
    public FileAttribute getFileAttribute(String url, HttpServletRequest req) {
        FileAttribute attribute = new FileAttribute();
        String suffix;
        FileType type;
        String fileName;
        String fullFileName = WebUtils.getUrlParameterReg(url, "fullfilename");
        if (StringUtils.hasText(fullFileName)) {
            fileName = fullFileName;
            type = FileType.typeFromFileName(fullFileName);
            suffix = KkFileUtils.suffixFromFileName(fullFileName);
        } else {
            fileName = WebUtils.getFileNameFromURL(url);
            type = FileType.typeFromUrl(url);
            suffix = WebUtils.suffixFromUrl(url);
        }
        attribute.setType(type);
        attribute.setName(fileName);
        attribute.setSuffix(suffix);
        attribute.setPath(ConfigConstants.getFileDir());
        url = WebUtils.encodeUrlFileName(url);
        attribute.setUrl(url);
        if (req != null) {
            String officePreviewType = req.getParameter("officePreviewType");
            String fileKey = WebUtils.getUrlParameterReg(url,"fileKey");
            if (StringUtils.hasText(officePreviewType)) {
                attribute.setOfficePreviewType(officePreviewType);
            }
            if (StringUtils.hasText(fileKey)) {
                attribute.setFileKey(fileKey);
            }
        }
        return attribute;
    }

    public FileAttribute getUidFileAttribute(String uid, String url, HttpServletRequest req) {
        FileAttribute attribute = new FileAttribute();
        try {
            String suffix;
            FileType type;
            String fileName;
            String fullFileName;
            String path;

            // uid ??????????????????????????? bulkDataLink, ????????? dataListCom, ????????? ????????? id
            BulkDataLink bulkDataLink = bulkDataLinkDao.findFirstByZipOid(uid);
            if(bulkDataLink == null) {
                DataListCom dataListCom = dataListComDao.findFirstByOid(uid);
                if(dataListCom == null) {
                    throw new Exception("uid is wrong.");
                }
                fullFileName = dataListCom.getFileName();
                path = dataListCom.getPath();
            } else {
                if (bulkDataLink.getDataOids().size() == 1) {
                    String fileOid = bulkDataLink.getDataOids().get(0);
                    DataListCom dataListCom = dataListComDao.findFirstByOid(fileOid);
                    if(dataListCom == null) {
                        throw new Exception("uid is wrong.");
                    }
                    fullFileName = dataListCom.getFileName();
                    path = dataListCom.getPath();
                } else {
                    fullFileName = bulkDataLink.getName() + ".zip";
                    path = bulkDataLink.getPath();
                }
            }
            fileName = fullFileName;
            type = FileType.typeFromFileName(fullFileName);
            suffix = KkFileUtils.suffixFromFileName(fullFileName);


            attribute.setType(type);
            attribute.setName(fileName);
            attribute.setSuffix(suffix);
            // ConfigConstants.setFileDirValue(path);
            attribute.setPath(path);
            if(!url.split("/")[url.split("/").length - 1].contains(".")) {
                url += "/" + fileName;
            }
            url = WebUtils.encodeUrlFileName(url);
            attribute.setUrl(url);
            if (req != null) {
                String officePreviewType = req.getParameter("officePreviewType");
                String fileKey = WebUtils.getUrlParameterReg(url,"fileKey");
                if (StringUtils.hasText(officePreviewType)) {
                    attribute.setOfficePreviewType(officePreviewType);
                }
                if (StringUtils.hasText(fileKey)) {
                    attribute.setFileKey(fileKey);
                }
            }
        } catch (Exception error) {
            logger.error("error: " + error);
        }
        return attribute;
    }

    /**
     * @return ?????????????????????????????????(??????)
     */
    public Map<String, String> listConvertedMedias() {
        return cacheService.getMediaConvertCache();
    }

    /**
     * ????????????????????????????????????
     * @param fileName
     * @param value
     */
    public void addConvertedMedias(String fileName, String value) {
        cacheService.putMediaConvertCache(fileName, value);
    }

    /**
     * @return ???????????????????????????????????????????????????
     */
    public String getConvertedMedias(String key) {
        return cacheService.getMediaConvertCache(key);
    }
}
