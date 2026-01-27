package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件操作相关的安全问题示例
 * ⚠️ 仅供学习和测试使用，请勿在生产环境使用！
 */
public class VulnerableFileHandler {

    /**
     * ❌ 问题1: 文件流未关闭导致资源泄漏
     */
    public String readFile(String filepath) {
        try {
            FileInputStream fis = new FileInputStream(filepath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            // ❌ 忘记关闭流
            return content.toString();
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * ❌ 问题2: 写文件时未关闭流
     */
    public void writeFile(String filepath, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        PrintWriter writer = new PrintWriter(fos);
        
        writer.println(content);
        writer.flush();
        
        // ❌ 未关闭流，可能导致数据丢失
    }

    /**
     * ❌ 问题3: 路径遍历攻击
     */
    public byte[] downloadFile(String filename) throws IOException {
        // 未验证文件名，攻击者可以传入 "../../etc/passwd"
        String filepath = "/var/www/uploads/" + filename;
        File file = new File(filepath);
        
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        
        return data; // ❌ 流未关闭
    }

    /**
     * ❌ 问题4: Zip Slip 漏洞
     */
    public void unzipFile(String zipFilePath, String destDir) throws IOException {
        FileInputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            // ❌ 未验证解压路径，可能被写入任意位置
            File destFile = new File(destDir + File.separator + entry.getName());
            
            FileOutputStream fos = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            // ❌ fos 未关闭
        }
        // ❌ zis 未关闭
    }

    /**
     * ❌ 问题5: 文件描述符泄漏
     */
    public void processMultipleFiles(String[] filepaths) {
        for (String filepath : filepaths) {
            try {
                FileInputStream fis = new FileInputStream(filepath);
                // 处理文件
                int data = fis.read();
                System.out.println("First byte: " + data);
                
                // ❌ 循环中创建大量未关闭的流
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ❌ 问题6: RandomAccessFile 未关闭
     */
    public void readRandomAccess(String filepath) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filepath, "r");
        raf.seek(100);
        byte[] buffer = new byte[50];
        raf.read(buffer);
        
        System.out.println(new String(buffer));
        
        // ❌ RandomAccessFile 未关闭
    }

    /**
     * ❌ 问题7: 临时文件未删除
     */
    public String processTempFile(String data) throws IOException {
        // 创建临时文件
        File tempFile = File.createTempFile("temp_", ".txt");
        // ❌ 未设置 deleteOnExit()
        
        FileWriter writer = new FileWriter(tempFile);
        writer.write(data);
        writer.close();
        
        // 处理完成后未删除临时文件
        return tempFile.getAbsolutePath();
    }

    /**
     * ❌ 问题8: 文件锁未释放
     */
    public void writeWithLock(String filepath, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        java.nio.channels.FileChannel channel = fos.getChannel();
        java.nio.channels.FileLock lock = channel.lock();
        
        fos.write(content.getBytes());
        
        // ❌ 锁未释放
        // ❌ 流未关闭
    }

    /**
     * ❌ 问题9: 缓冲区未刷新
     */
    public void writeWithBuffer(String filepath, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        
        bos.write(content.getBytes());
        
        // ❌ 未调用 flush()
        bos.close(); // 如果这里抛异常，数据可能丢失
    }

    /**
     * ❌ 问题10: 文件权限设置不当
     */
    public void createSecretFile(String filepath, String secretData) throws IOException {
        File file = new File(filepath);
        FileWriter writer = new FileWriter(file);
        writer.write(secretData);
        writer.close();
        
        // ❌ 未设置文件权限，默认可能被其他用户读取
        // 应该设置：file.setReadable(false, false);
    }

    /**
     * ❌ 问题11: 大文件读取未分块，可能导致内存溢出
     */
    public String readLargeFile(String filepath) throws IOException {
        File file = new File(filepath);
        FileInputStream fis = new FileInputStream(file);
        
        // ❌ 一次性读取整个文件到内存
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        
        return new String(data);
    }

    /**
     * ❌ 问题12: 异常处理中资源未关闭
     */
    public void copyFile(String source, String dest) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // ❌ 异常时未关闭流
            return;
        }
        
        // ❌ 只在正常流程关闭，异常时会泄漏
        try {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ❌ 问题13: 使用 finalize 方法关闭资源（不推荐）
     */
    class BadFileHandler {
        private FileInputStream fis;
        
        public BadFileHandler(String filepath) throws IOException {
            fis = new FileInputStream(filepath);
        }
        
        // ❌ 依赖 finalize 是不可靠的
        @Override
        protected void finalize() throws Throwable {
            if (fis != null) {
                fis.close();
            }
            super.finalize();
        }
    }

    /**
     * ❌ 问题14: 文件存在性检查后操作（TOCTOU 竞态条件）
     */
    public void createFileIfNotExists(String filepath, String content) throws IOException {
        File file = new File(filepath);
        
        // ❌ 检查和创建之间存在时间窗口
        if (!file.exists()) {
            // 其他线程可能在这里创建了文件
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        }
    }

    /**
     * ❌ 问题15: 硬编码的文件路径
     */
    public void saveConfig(String config) throws IOException {
        // ❌ 硬编码路径，不同系统可能不兼容
        String configPath = "C:\\Users\\Admin\\config.txt";
        FileWriter writer = new FileWriter(configPath);
        writer.write(config);
        writer.close();
    }

    /**
     * ❌ 问题16: ObjectInputStream 未关闭
     */
    public Object loadObject(String filepath) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filepath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        
        Object obj = ois.readObject();
        
        // ❌ 未关闭流
        return obj;
    }

    /**
     * ❌ 问题17: 多个流嵌套，只关闭了外层流
     */
    public void writeGzipFile(String filepath, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(fos);
        
        gzos.write(content.getBytes());
        gzos.close();
        
        // ❌ 虽然关闭了 gzos，但没有显式关闭 fos
        // 在某些情况下可能导致问题
    }

    /**
     * ❌ 问题18: 目录遍历未检查符号链接
     */
    public void deleteDirectory(File dir) {
        // ❌ 未检查符号链接，可能删除链接指向的文件
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        dir.delete();
    }

    /**
     * ❌ 问题19: 文件名未编码，可能导致问题
     */
    public void downloadFileWithName(String filename) throws IOException {
        // ❌ 文件名可能包含特殊字符，未进行编码
        String filepath = "/downloads/" + filename;
        File file = new File(filepath);
        
        FileInputStream fis = new FileInputStream(file);
        // ... 处理文件
    }

    /**
     * ❌ 问题20: 使用 deleteOnExit 但程序可能异常退出
     */
    public void createTempFileWithCleanup(String content) throws IOException {
        File tempFile = File.createTempFile("temp_", ".txt");
        tempFile.deleteOnExit(); // ❌ 如果JVM异常终止，文件不会被删除
        
        FileWriter writer = new FileWriter(tempFile);
        writer.write(content);
        writer.close();
        
        // 长时间运行的程序中，这会导致文件累积
    }
}

