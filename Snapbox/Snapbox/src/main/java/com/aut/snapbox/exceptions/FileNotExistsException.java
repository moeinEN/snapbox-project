package com.aut.snapbox.exceptions;



public class FileNotExistsException extends Exception {

    public FileNotExistsException(String file) {
        super(" file " + file + " does not exist");
    }
}