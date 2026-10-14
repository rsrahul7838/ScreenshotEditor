package com.images.screenshoteditor.controller;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Word;

import java.util.List;


@Service
public class OcrService {

    @Value("${tesseract.datapath:C:/Users/rahulsrivastav/AppData/Local/Tesseract-OCR/tessdata}")
    private String tessDataPath;

    public String recognizeText(BufferedImage image)
            throws TesseractException {

        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(6);

        return tesseract.doOCR(image).trim();
    }

    public List<Word> recognizeWords(BufferedImage image)
            throws TesseractException {

        ITesseract tesseract = new Tesseract();

        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(6);

        return tesseract.getWords(
                image,
                ITessAPI.TessPageIteratorLevel.RIL_WORD
        );
    }
}
