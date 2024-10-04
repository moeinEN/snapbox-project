package com.aut.snapbox.exceptions;


public class DirNotExistException extends Exception{
    public DirNotExistException(String directory ) {
        super(" directory " + directory + " does not exist");
    }
}
