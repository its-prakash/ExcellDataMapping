package com.excelData.xml;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.awt.*;

@RestController
@RequestMapping("/api/xml")
public class XmlController {

    private XmlService xmlService;

    public XmlController(XmlService xmlService){
        this.xmlService = xmlService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<Xml> uploadXmlfile(@RequestPart("file") FilePart filePart){
        return xmlService.processAndSave(filePart);
    }

}
