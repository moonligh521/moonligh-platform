package com.moonligh.consult.framework.ocr;

import java.util.Optional;

/** 图片进、文字出。调用方自己传提示词。失败返回空。 */
public interface OcrClient {

    Optional<String> recognize(byte[] image, String mimeType, String prompt);
}
