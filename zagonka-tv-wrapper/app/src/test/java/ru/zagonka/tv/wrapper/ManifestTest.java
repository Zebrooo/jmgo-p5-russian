package ru.zagonka.tv.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class ManifestTest {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    @Test
    public void declaresRecognizerVisibilityAndTvLauncherEntry() throws Exception {
        Element manifest = manifest();

        assertTrue(hasNamed(manifest, "queries", "package", "org.futo.voiceinput.jmgo"));
        assertTrue(hasNamed(manifest, "intent-filter", "category", "android.intent.category.LEANBACK_LAUNCHER"));
        Element application = (Element) manifest.getElementsByTagName("application").item(0);
        assertEquals("false", application.getAttributeNS(ANDROID_NS, "usesCleartextTraffic"));
        assertEquals("false", application.getAttributeNS(ANDROID_NS, "allowBackup"));
    }

    private static boolean hasNamed(Element root, String container, String tag, String name) {
        NodeList containers = root.getElementsByTagName(container);
        for (int index = 0; index < containers.getLength(); index++) {
            NodeList nodes = ((Element) containers.item(index)).getElementsByTagName(tag);
            for (int nodeIndex = 0; nodeIndex < nodes.getLength(); nodeIndex++) {
                if (name.equals(((Element) nodes.item(nodeIndex)).getAttributeNS(ANDROID_NS, "name"))) return true;
            }
        }
        return false;
    }

    private static Element manifest() throws Exception {
        File file = new File("src/main/AndroidManifest.xml");
        if (!file.exists()) file = new File("zagonka-tv-wrapper/app/src/main/AndroidManifest.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(file).getDocumentElement();
    }
}
