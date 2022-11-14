package com.wyverno;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {


    public static void main(String[] args) throws IOException {

        Path staticFolder = Paths.get("static");
        Path clientFolder = Paths.get("client");

        FileValidator fileValidator = new FileValidator(staticFolder, clientFolder);
        fileValidator.validateFiles();
    }
}
