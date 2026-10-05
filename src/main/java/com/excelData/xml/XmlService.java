package com.excelData.xml;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class XmlService {
//    public Flux<Xml> processAndSave(FilePart filePart){
//        System.out.println(filePart);
//        return null;
//    }

//    @Service
//    @RequiredArgsConstructor
//    public class XmlService {

        private XmlRepository xmlRepository;
        public XmlService(XmlRepository xmlRepository){
            this.xmlRepository = xmlRepository;
        }

        public Flux<Xml> processAndSave(FilePart file) {

            return DataBufferUtils.join(file.content())
                    .flatMapMany(dataBuffer -> {

                        try {

                            InputStream inputStream =
                                    dataBuffer.asInputStream();

                            DocumentBuilderFactory factory =
                                    DocumentBuilderFactory.newInstance();

                            DocumentBuilder builder =
                                    factory.newDocumentBuilder();

                            Document document =
                                    builder.parse(inputStream);

                            NodeList employeeNodes =
                                    document.getElementsByTagName("employee");

                            List<Xml> xmls =
                                    new ArrayList<>();

                            for (int i = 0;
                                 i < employeeNodes.getLength();
                                 i++) {

                                Element element =
                                        (Element) employeeNodes.item(i);

                                Xml xml =
                                        new Xml();

                                xml.setName(
                                        element
                                                .getElementsByTagName("name")
                                                .item(0)
                                                .getTextContent()
                                );

                                xml.setSalary(
                                        Integer.parseInt(
                                                element
                                                        .getElementsByTagName("salary")
                                                        .item(0)
                                                        .getTextContent()
                                        )
                                );

                                xml.setCompany(
                                        element
                                                .getElementsByTagName("company")
                                                .item(0)
                                                .getTextContent()
                                );

                                xml.setRole(
                                        element
                                                .getElementsByTagName("role")
                                                .item(0)
                                                .getTextContent()
                                );

                                xmls.add(xml);
                            }

                            return xmlRepository.saveAll(xmls);

                        } catch (Exception e) {

                            return Flux.error(
                                    new RuntimeException(
                                            "XML processing failed", e
                                    )
                            );

                        } finally {

                            DataBufferUtils.release(dataBuffer);
                        }
                    });
//        }
    }
}
