package depviz.nuget;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Извлекает прямые зависимости из файла описания пакета NuGet (.nuspec).
 */
public final class NuspecParser {

    private static final String ANY_NAMESPACE = "*";
    private static final String DEPENDENCIES = "dependencies";
    private static final String GROUP = "group";
    private static final String DEPENDENCY = "dependency";
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    /**
     * Разбирает .nuspec и возвращает зависимости, сгруппированные по целевым фреймворкам.
     * В старом формате .nuspec зависимости лежат без групп – они попадают в группу с пустым фреймворком.
     */
    public List<DependencyGroup> parse(byte[] content) throws RepositoryException {
        NodeList containers = readDocument(content).getElementsByTagNameNS(ANY_NAMESPACE, DEPENDENCIES);
        if (containers.getLength() == 0) {
            return List.of();
        }
        List<DependencyGroup> groups = new ArrayList<>();
        List<Dependency> ungrouped = new ArrayList<>();
        for (Element child : childElements((Element) containers.item(0))) {
            if (GROUP.equals(child.getLocalName())) {
                groups.add(new DependencyGroup(child.getAttribute("targetFramework"), readDependencies(child)));
            } else if (DEPENDENCY.equals(child.getLocalName())) {
                ungrouped.add(toDependency(child));
            }
        }
        if (!ungrouped.isEmpty()) {
            groups.add(0, new DependencyGroup("", ungrouped));
        }
        return groups;
    }

    /**
     * Читает XML с запретом DOCTYPE, чтобы файл из сети не мог подгрузить внешние сущности (XXE).
     */
    private Document readDocument(byte[] content) throws RepositoryException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(DISALLOW_DOCTYPE, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            return builder.parse(new ByteArrayInputStream(content));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RepositoryException("Не удалось разобрать файл .nuspec: " + e.getMessage());
        }
    }

    private List<Dependency> readDependencies(Element group) {
        List<Dependency> result = new ArrayList<>();
        for (Element child : childElements(group)) {
            if (DEPENDENCY.equals(child.getLocalName())) {
                result.add(toDependency(child));
            }
        }
        return result;
    }

    private Dependency toDependency(Element element) {
        return new Dependency(element.getAttribute("id"), element.getAttribute("version"));
    }

    private List<Element> childElements(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result.add((Element) nodes.item(i));
            }
        }
        return result;
    }
}
