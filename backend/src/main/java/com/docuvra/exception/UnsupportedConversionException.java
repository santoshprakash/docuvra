package com.docuvra.exception;

public class UnsupportedConversionException extends ConversionException {

    public UnsupportedConversionException() {
        super("Preview conversion is not supported for this file type. Please download the original file.");
    }
}
