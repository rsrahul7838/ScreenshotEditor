package com.images.screenshoteditor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.images.screenshoteditor.service.ImageProcessingService;

import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import java.util.HashMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;


import net.sourceforge.tess4j.Word;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;



@Controller
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;


    // one Spring component uses another Spring component through dependency injection.
    private final ImageProcessingService imageProcessingService;
    private final OcrService ocrService;

    //  Spring automatically provides the ImageProcessingService object to this constructor. This is called constructor-based dependency injection.
    public UploadController(ImageProcessingService imageProcessingService, OcrService ocrService) {
        this.imageProcessingService = imageProcessingService;
        this.ocrService = ocrService;
    }

    @PostMapping("/upload")
    public String uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam(defaultValue = "both") String mode,
            @RequestParam(required = false) String newDate,
            @RequestParam(required = false) String newTime,
            Model model) {

        try {
            // 1. Check whether a file was selected
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select an image.");
                return "index";
            }

            // 2. Limit upload size to 5 MB
            if (file.getSize() > MAX_FILE_SIZE) {
                model.addAttribute(
                        "error", "Image must be smaller than 5 MB.");
                return "index";
            }

            // 3. Check the filename extension
            String filename = file.getOriginalFilename();

            if (filename == null ||
                    !(filename.toLowerCase(Locale.ROOT).endsWith(".png")
                            || filename.toLowerCase(Locale.ROOT).endsWith(".jpg")
                            || filename.toLowerCase(Locale.ROOT).endsWith(".jpeg"))) {

                model.addAttribute(
                        "error", "Please upload a PNG or JPG image.");
                return "index";
            }

            // 4. Decode the file as an actual image
            BufferedImage image = ImageIO.read(file.getInputStream());

            if (image == null) {
                model.addAttribute(
                        "error", "The uploaded file is not a valid image.");
                return "index";
            }

            // 5. Check image dimensions
            long pixels = (long) image.getWidth() * image.getHeight();

            if (pixels > MAX_PIXELS) {
                model.addAttribute(
                        "error", "Image dimensions are too large.");
                return "index";
            }

            // 6. Allow only PNG or JPEG image content
            String contentType = file.getContentType();

            if (!"image/png".equalsIgnoreCase(contentType)
                    && !"image/jpeg".equalsIgnoreCase(contentType)) {

                model.addAttribute(
                        "error", "Only PNG and JPEG images are allowed.");
                return "index";
            }

            // 7. Convert the image into Base64 for browser preview
            String format = filename.toLowerCase(Locale.ROOT)
                    .endsWith(".png") ? "png" : "jpeg";

        //            java.io.ByteArrayOutputStream output =
        //                    new java.io.ByteArrayOutputStream();
        //
        //            ImageIO.write(image, format, output);

        //            ======================

            // Process the uploaded image using our service

        // Convert the processed copy to bytes
            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            ImageIO.write(image, format, output);

            String base64 = Base64.getEncoder()
                    .encodeToString(output.toByteArray());

            model.addAttribute("imageData",
                    "data:image/" + format + ";base64," + base64);

            model.addAttribute("width", image.getWidth());
            model.addAttribute("height", image.getHeight());
            model.addAttribute("message", "Image uploaded successfully!");

        } catch (IOException ex) {
            model.addAttribute(
                    "error", "Could not read the image. Please try another file.");
        }

        return "index";
    }

        @PostMapping(
                value = "/selection",
                consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
        )
        @ResponseBody
        public Map<String, Object> receiveSelection(
                @RequestParam("image") MultipartFile file,
                @RequestParam int x,
                @RequestParam int y,
                @RequestParam int width,
                @RequestParam int height) {

            Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "Select an image smaller than 5 MB.");
                return response;
            }

            BufferedImage original =
                    ImageIO.read(file.getInputStream());

            if (original == null) {
                response.put("success", false);
                response.put("message", "Invalid image file.");
                return response;
            }

            int imageWidth = original.getWidth();
            int imageHeight = original.getHeight();

            // Validate the coordinates against the actual uploaded image.
            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || x >= imageWidth || y >= imageHeight
                    || width > imageWidth - x
                    || height > imageHeight - y) {

                response.put("success", false);
                response.put("message", "Selection is outside the image.");
                return response;
            }

            // Process a separate copy through our service.
            BufferedImage processed =
                    imageProcessingService.addAnnotation(
                            original, x, y, width, height);

            // Encode the processed copy as PNG for browser preview.
            java.io.ByteArrayOutputStream output =
                    new java.io.ByteArrayOutputStream();

            ImageIO.write(processed, "png", output);

            String base64 = java.util.Base64.getEncoder()
                    .encodeToString(output.toByteArray());

            response.put("success", true);
            response.put("message", "Image processed successfully!");
            response.put("imageData", "data:image/png;base64," + base64);
            response.put("width", imageWidth);
            response.put("height", imageHeight);

        } catch (IOException ex) {
            response.put("success", false);
            response.put("message", "Could not process the uploaded image.");
        }

        return response;
    }



    @PostMapping(
            value = "/edit-mockup",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public Map<String, Object> editMockup(
            @RequestParam("image") MultipartFile file,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height,
            @RequestParam String mode,
            @RequestParam(required = false) String newDate,
            @RequestParam(required = false) String newTime
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Please upload an image.");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "Image must be 5 MB or smaller."
                );
            }

            BufferedImage image = ImageIO.read(file.getInputStream());

            if (image == null) {
                throw new IllegalArgumentException(
                        "The uploaded file is not a valid image."
                );
            }

            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || x >= image.getWidth()
                    || y >= image.getHeight()
                    || width > image.getWidth() - x
                    || height > image.getHeight() - y) {
                throw new IllegalArgumentException(
                        "The selected region is outside the image."
                );
            }

            if (!mode.equals("date")
                    && !mode.equals("time")
                    && !mode.equals("both")) {
                throw new IllegalArgumentException(
                        "Invalid editing mode."
                );
            }

            if (!mode.equals("time")) {
                if (newDate == null || newDate.isBlank()) {
                    throw new IllegalArgumentException(
                            "Please choose a date."
                    );
                }

                java.time.LocalDate.parse(newDate);
            }

            if (!mode.equals("date")) {
                if (newTime == null || newTime.isBlank()) {
                    throw new IllegalArgumentException(
                            "Please choose a time."
                    );
                }

                java.time.LocalTime.parse(newTime);
            }

            result.put("success", true);
            result.put("message",
                    "Editing settings received and validated.");
            result.put("mode", mode);
            result.put("newDate", newDate);
            result.put("newTime", newTime);
            result.put("x", x);
            result.put("y", y);
            result.put("width", width);
            result.put("height", height);

        } catch (IllegalArgumentException
                 | java.time.DateTimeException e) {
            result.put("success", false);
            result.put("message", "Invalid input: " + e.getMessage());

        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "Could not read the uploaded image.");
        }

        return result;
    }

    @PostMapping(
            value = "/ocr",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseBody
    public Map<String, Object> recognizeSelectedText(
            @RequestParam("image") MultipartFile file,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height
    ) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "Upload a valid image of 5 MB or smaller."
                );
            }

            BufferedImage original =
                    ImageIO.read(file.getInputStream());

            if (original == null) {
                throw new IllegalArgumentException(
                        "The uploaded file is not a valid image."
                );
            }

            int imageWidth = original.getWidth();
            int imageHeight = original.getHeight();

            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || x >= imageWidth || y >= imageHeight
                    || width > imageWidth - x
                    || height > imageHeight - y) {
                throw new IllegalArgumentException(
                        "The selected region is outside the image."
                );
            }

            BufferedImage cropped = original.getSubimage(
                    x, y, width, height
            );


            String detectedText = ocrService.recognizeText(cropped);

            List<Word> words = ocrService.recognizeWords(cropped);
            List<Map<String, Object>> wordResults = new ArrayList<>();

            for (Word word : words) {
                Rectangle box = word.getBoundingBox();

                if (box == null) {
                    continue;
                }

                Map<String, Object> item = new HashMap<>();
                item.put("text", word.getText());

                // Convert crop-relative coordinates to original-image coordinates.
                item.put("x", x + box.x);
                item.put("y", y + box.y);
                item.put("width", box.width);
                item.put("height", box.height);
                item.put("confidence", word.getConfidence());

                wordResults.add(item);
            }


            Pattern datePattern = Pattern.compile(
                    "\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}"
                            + "|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b"
            );

            Pattern timePattern = Pattern.compile(
                    "\\b(?:[01]?\\d|2[0-3]):[0-5]\\d"
                            + "(?:\\s?[AP]M)?\\b|\\b(?:0?\\d|1[0-2])\\s?[AP]M\\b",
                    Pattern.CASE_INSENSITIVE
            );

            List<Map<String, Object>> candidates = new ArrayList<>();

            for (Map<String, Object> word : wordResults) {
                String text = String.valueOf(word.get("text"));

                String type = null;

                if (datePattern.matcher(text).matches()) {
                    type = "date";
                } else if (timePattern.matcher(text).matches()) {
                    type = "time";
                }

                if (type != null) {
                    Map<String, Object> candidate = new HashMap<>();
                    candidate.put("type", type);
                    candidate.put("text", text);
                    candidate.put("x", word.get("x"));
                    candidate.put("y", word.get("y"));
                    candidate.put("width", word.get("width"));
                    candidate.put("height", word.get("height"));

                    candidates.add(candidate);
                }
            }

            result.put("success", true);
            result.put("message", "OCR completed successfully.");
            result.put("detectedText", detectedText);
            result.put("detectedWords", wordResults);
            result.put("candidates", candidates);
            result.put("dateCandidate",
                    candidates.stream().anyMatch(c -> "date".equals(c.get("type"))));
            result.put("timeCandidate",
                    candidates.stream().anyMatch(c -> "time".equals(c.get("type"))));
            result.put("x", x);
            result.put("y", y);
            result.put("width", width);
            result.put("height", height);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());

        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "Could not read the uploaded image.");

        } catch (net.sourceforge.tess4j.TesseractException e) {
            result.put("success", false);
            result.put("message",
                    "OCR failed. Check the Tesseract installation and language data.");
        }

        return result;
    }

}
