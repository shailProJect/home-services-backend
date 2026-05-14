package com.homeservices.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

  private final Cloudinary cloudinary;

  private static final long MAX_FILE_SIZE = 500 * 1024;

  public String uploadSecureDocument(MultipartFile file, String folder) throws IOException {

    validateFile(file);

    String randomFileName = UUID.randomUUID().toString();

    Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
        ObjectUtils.asMap("folder", folder, "resource_type", "auto", "public_id", randomFileName,
            "overwrite", false, "invalidate", true, "type", "authenticated", "access_mode",
            "authenticated"));

    return uploadResult.get("public_id").toString();
  }

  private void validateFile(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new RuntimeException("File is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new RuntimeException("File size must be less than 500KB");
    }

    String contentType = file.getContentType();

    if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png")
        || contentType.equals("application/pdf"))) {
      throw new RuntimeException("Only JPG, PNG and PDF files are allowed");
    }
  }

  public String uploadProfilePhoto(MultipartFile file) throws IOException {

    validateProfilePhoto(file);

    String randomFileName = UUID.randomUUID().toString();

    Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
        ObjectUtils.asMap("folder", "profile-photos", "resource_type", "image", "public_id",
            randomFileName, "overwrite", false, "invalidate", true, "secure", true));

    return uploadResult.get("secure_url").toString();
  }

  private void validateProfilePhoto(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new RuntimeException("Profile photo is empty");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new RuntimeException("Profile photo must be less than 500KB");
    }

    String contentType = file.getContentType();

    if (contentType == null
        || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
      throw new RuntimeException("Only JPG and PNG profile photos are allowed");
    }
  }
}
