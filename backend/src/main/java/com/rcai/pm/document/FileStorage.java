package com.rcai.pm.document;
import org.springframework.core.io.Resource; import org.springframework.web.multipart.MultipartFile;
public interface FileStorage{String save(MultipartFile file);Resource load(String storageKey);}
