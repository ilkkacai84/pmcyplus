package com.rcai.pm.document;
import com.rcai.pm.common.ApiException; import org.springframework.beans.factory.annotation.Value; import org.springframework.core.io.*; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Component; import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*; import java.util.UUID;
@Component public class LocalFileStorage implements FileStorage{
 private final Path root; public LocalFileStorage(@Value("${app.storage.path:./data/files}") String path){root=Path.of(path).toAbsolutePath().normalize();}
 public String save(MultipartFile file){try{Files.createDirectories(root);String key=UUID.randomUUID()+extension(file.getOriginalFilename());Files.copy(file.getInputStream(),root.resolve(key));return key;}catch(Exception e){throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"文件保存失败");}}
 public Resource load(String key){try{Path p=root.resolve(key).normalize();if(!p.startsWith(root)||!Files.exists(p))throw new ApiException(HttpStatus.NOT_FOUND,"文件不存在");return new FileSystemResource(p);}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(HttpStatus.NOT_FOUND,"文件不存在");}}
 private String extension(String n){if(n==null)return "";int i=n.lastIndexOf('.');return i<0||n.length()-i>12?"":n.substring(i).replaceAll("[^A-Za-z0-9.]","");}
}
