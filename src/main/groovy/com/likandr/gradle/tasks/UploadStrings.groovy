package com.likandr.gradle.tasks


import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class UploadStrings extends DefaultTask {

    @Input
    String lokalise_token
    @Input
    String lokalise_id
    @Input
    Project project

    @TaskAction
    def handle() {
        def allStringsDir = project.fileTree(project.rootDir).include("**/strings.xml").files.path
        def allArraysDir = project.fileTree(project.rootDir).include("**/arrays.xml").files.path
        def allPluralsDir = project.fileTree(project.rootDir).include("**/plurals.xml").files.path
        def listOfPaths = allStringsDir + allArraysDir + allPluralsDir
        for (String path in listOfPaths) {
            println("Path: " + path)
        }
        for (String item in listOfPaths) {
            def asd = item.split(Pattern.quote('\\'))
            for (eeee in asd) {
                if (eeee.contains("value")) {
                    def zzz = eeee.split("-")
                    def lang = "en"
                    if (zzz.size() == 2)
                        lang = zzz[1].split("/")[0]
                    String charset = "ISO-8859-1"
                    File uploadFile1 = new File(item)

                    def url = new URL("https://api.lokalise.co/api2/projects/${lokalise_id}/files/upload")
                    def connection = url.openConnection()
                    connection.setRequestMethod("POST")
                    connection.addRequestProperty("content-type", "application/json")
                    connection.addRequestProperty("x-api-token", lokalise_token)
                    connection.doOutput = true
                    def filename = uploadFile1.absolutePath.split(project.rootDir.absolutePath + "/")[1]
                    def json = new JsonBuilder([
                            "filename": filename,
                            "lang_iso": lang,
                            "data"    : uploadFile1.bytes.encodeBase64().toString()
                    ])
                    println("Request body: " + json.toString())
                    def writer = new OutputStreamWriter(connection.outputStream)
                    writer.write(json.toString())
                    writer.flush()
                    writer.close()
                    connection.connect()

                    if (connection.getResponseCode() != 202 && connection.getResponseCode() != 302) {
                        throw new IllegalStateException(
                                "Filename: " + filename + "An error occurred while trying to export from lokalise API: \n\n" +
                                        connection.errorStream.text
                        )
                    } else {
                        println("Success for: " + filename)
                    }
//
//                    try {
//                        MultipartUtility multipart = new MultipartUtility(requestURL, charset)
//                        multipart.addFormField("api_token", lokalise_token)
//                        multipart.addFormField("id", lokalise_id)
//                        multipart.addFormField("lang_iso", lang)
//                        multipart.addFormField("replace", "0")
//
//                        multipart.addFilePart("fileUpload", uploadFile1)
//
//                        List<String> response = multipart.finish()
//                    } catch (IOException ex) {
//                        System.out.println("ERROR: " + ex.getMessage())
//                        ex.printStackTrace()
//                    }

                    ///////
//                    def response = ['curl', '-X', 'POST', 'https://api.lokalise.co/api/project/import',
//                                    '-F', "api_token=" + lokalise_token,
//                                    '-F', "id=" + lokalise_id,
//                                    '-F', "file=@" + item,
//                                    '-F', "replace=0",
//                                    '-F', "lang_iso=" + lang
//                    ].execute().text
//                    println response
                    ///////
                    break
                }
            }
        }
    }

    private String getDefaultLang(lokalise_token, lokalise_id) {
        def jsonSlurper = new JsonSlurper()

        def headers = [
                'Accept'       : 'application/json',
                'x-api-token'  : lokalise_token,
                'X-Total-Count': '100'
        ]
        def response = new URL("https://api.lokalise.com/api2/projects/" + lokalise_id + "/languages").getText(requestProperties: headers)

        ///////
//    def response = ['curl', 'GET', 'https://api.lokalise.co/api/language/list?' +
//            'api_token=' + lokalise_token +
//            '&id=' + lokalise_id
//    ].execute().text
        ///////

        def langsJson = jsonSlurper.parseText(response)
        def lang = ""
        for (item in langsJson.languages) {
            if (item.is_default == "1")
                lang = item.iso
        }
        return lang
    }
}