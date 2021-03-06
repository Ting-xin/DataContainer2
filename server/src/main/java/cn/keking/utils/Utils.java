package cn.keking.utils;

/**
 * @Author mingyuan
 * @Date 2020.06.12 10:25
 */


import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    static int count=0;

//    static String[] visualTemplateIds={"4996e027-209b-4121-907b-1ed36a417d22","f73f31ff-2f23-4c7a-a57d-39d0c7a6c4e6","d3605b83-af8d-491c-91b3-a0e0bf3fe714"};

    public static class Method {
        public static String POST = "POST";
        public static String GET = "GET";
    }



    public static String getMd5ByFile(File file) throws FileNotFoundException {
        String value = null;
        FileInputStream in = new FileInputStream(file);
        try {
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    public static boolean isSameDay(Date day1,Date day2){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        String d1=simpleDateFormat.format(day1);
        String d2=simpleDateFormat.format(day2);
        return d1.equals(d2);

    }

    public static String checkLoginStatus(HttpSession httpSession){

        Object object=httpSession.getAttribute("uid");
        if(object==null){
            return null;
        }
        else{
            return object.toString();
        }

    }

    public static JSONObject postJSON(String urlStr, JSONObject jsonParam) {
        try {

            //System.out.println(obj);
            // ??????url??????
            URL url = new URL(urlStr);
            // ??????http??????
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // ??????????????????
            conn.setDoOutput(true);

            conn.setDoInput(true);

            // ??????????????????
            conn.setUseCaches(false);
            // ??????????????????
            conn.setRequestMethod("POST");
            // ?????????????????????
            conn.setRequestProperty("Connection", "Keep-Alive");
            // ?????????????????????:
            conn.setRequestProperty("Charset", "UTF-8");
            //?????????????????????
            byte[] data = (jsonParam.toJSONString()).getBytes();

            // ??????????????????
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));

            // ??????????????????:
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");


            // ??????????????????
            conn.connect();
            OutputStream out = conn.getOutputStream();
            // ????????????????????????
            out.write(data);
            out.flush();
            out.close();

            System.out.println(conn.getResponseCode());
            System.out.println(conn.getResponseMessage());

            // ?????????????????????
            if (conn.getResponseCode() == 200) {
                System.out.println("????????????");
                // ?????????????????????
                InputStream in = conn.getInputStream();
                String a = null;
                try {
                    byte[] data1 = new byte[in.available()];
                    in.read(data1);
                    // ???????????????
                    a = new String(data1);
                    System.out.println(a);
                    return JSONObject.parseObject(a);
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    return null;
                }
            } else {
                System.out.println("no++");
                return null;
            }

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }

    }

    public static JSONObject connentURL(String method, String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(3000);
            connection.connect();
            // ???????????????????????????Reader??????
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));//????????????,??????????????????
            String lines = "";
            String strResponse = "";
            while ((lines = reader.readLine()) != null) {
                strResponse += lines;
            }
            JSONObject jsonResponse = JSONObject.parseObject(strResponse);

            reader.close();

            connection.disconnect();

            return jsonResponse;


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static List<String> saveFiles(List<MultipartFile> files, String path, String uid, String suffix,List<String> result) {
        new File(path).mkdirs();


        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            fileName = "/" + uid + "/" + new Date().getTime() + "_" + fileName;
            result.add(suffix + fileName);
            int size = (int) file.getSize();
            System.out.println(fileName + "-->" + size);

            if (file.isEmpty()) {
                continue;
            } else {
                File dest = new File(path + fileName);
                if (!dest.getParentFile().exists()) { // ?????????????????????????????????
                    dest.getParentFile().mkdir();
                }
                try {
                    file.transferTo(dest);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return result;
    }


    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param sPath ???????????????????????????
     * @return ?????????????????? true??????????????? false???
     */
    public static boolean delete(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // ?????????????????????????????????
        if (!file.exists()) {  // ??????????????? false
            return flag;
        } else {
            // ?????????????????????
            if (file.isFile()) {  // ????????????????????????????????????
                return deleteFile(sPath);
            } else {  // ????????????????????????????????????
                return deleteDirectory(sPath);
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param sPath ???????????????????????????
     * @return ??????????????????????????????true???????????????false
     */
    public static boolean deleteFile(String sPath) {
        Boolean flag = false;
        File file = new File(sPath);
        // ??????????????????????????????????????????
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param sPath ??????????????????????????????
     * @return ????????????????????????true???????????????false
     */
    public static boolean deleteDirectory(String sPath) {
        //??????sPath?????????????????????????????????????????????????????????
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        //??????dir???????????????????????????????????????????????????????????????
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //?????????????????????????????????(???????????????)
        File[] files = dirFile.listFiles();
        for (int i = files.length - 1; i >= 0; i--) {
            //???????????????
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } //???????????????
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //??????????????????
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }


    public static String getUdxSchema(String text,String name){
        int findIndex=text.indexOf(name);
        int startIndex=text.indexOf(">",findIndex+name.length())+1;
        int endIndex=text.indexOf("</DatasetItem>",startIndex);
        return text.substring(startIndex,endIndex);
    }

    public static boolean isChinese(String str) {
        String regEx = "[\u4e00-\u9fa5]";
        Pattern pat = Pattern.compile(regEx);
        Matcher matcher = pat.matcher(str);
        boolean flg = false;
        if (matcher.find())
            flg = true;

        return flg;
    }

    public static String saveBase64Image(String content,String oid,String resourcePath,String htmlLoadPath){
        int startIndex = 0, endIndex = 0, index = 0;
        while (content.indexOf("src=\"data:im", startIndex) != -1) {
            int Start = content.indexOf("src=\"data:im", startIndex) + 5;
            int typeStart = content.indexOf("/", Start) + 1;
            int typeEnd = content.indexOf(";", typeStart);
            String type = content.substring(typeStart, typeEnd);
            startIndex = typeEnd + 8;
            endIndex = content.indexOf("\"", startIndex);
            String imgStr = content.substring(startIndex, endIndex);

            String imageName = "/detailImage/" + oid + "/" + oid + "_" + (index++) + "." + type;
            Utils.base64StrToImage(imgStr, resourcePath + imageName);

            content = content.substring(0, Start) + htmlLoadPath + imageName + content.substring(endIndex, content.length());
        }
        return content;
    }

    //base64????????????????????????
    public static boolean base64StrToImage(String imgStr, String path) {
        if (imgStr == null)
            return false;
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            // ??????
            byte[] b = decoder.decode(imgStr);
            // ????????????
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            //?????????????????????????????????
            File tempFile = new File(path);

            if (!tempFile.getParentFile().exists()) {
                tempFile.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(tempFile);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void count(){
        System.out.println("finish:"+(++count));
    }
}

