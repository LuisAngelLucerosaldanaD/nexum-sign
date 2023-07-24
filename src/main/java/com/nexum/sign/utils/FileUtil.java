package com.nexum.sign.utils;

import java.io.*;
import java.util.Base64;

public class FileUtil {
    public static void base64ToStore(String file, String path) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(file);
        File fileByt = new File(path);
        FileOutputStream fos = new FileOutputStream(fileByt);
        fos.write(decodedBytes);
        fos.flush();
        fos.close();
    }

    public static void deleteFile(String path) throws ErrorUtil {
        File file = new File(path);
        if (!file.exists()) return;

        boolean isDeleted = file.delete();
        if (isDeleted) return;

        throw new ErrorUtil("No se pudo eliminar el archivo");
    }


    public static String getBase64ToPath(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] fileBytes = new byte[fis.available()];
            fis.read(fileBytes);
            return Base64.getEncoder().encodeToString(fileBytes);
        }
    }
}
