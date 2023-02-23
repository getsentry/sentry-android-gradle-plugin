package io.sentry.android.gradle

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document

internal class ManifestWriter {

    companion object {
        private const val TAG_APPLICATION = "application"
        private const val TAG_META_DATA = "meta-data"
        private const val ATTR_NAME = "android:name"
        private const val ATTR_VALUE = "android:value"
    }

    fun writeMetaData(
        manifestSource: File,
        manifestTarget: File,
        name: String,
        integrationsList: String
    ) {
        openAndroidManifestXMLDocument(manifestSource) { document ->
            val application = document.getElementsByTagName(TAG_APPLICATION).item(0)
            val metadata = document.createElement(TAG_META_DATA)
            metadata.setAttribute(ATTR_NAME, name)
            metadata.setAttribute(ATTR_VALUE, integrationsList)
            application.appendChild(metadata)

            val factory = TransformerFactory.newInstance()
            val transformer = factory.newTransformer().apply {
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            }
            val source = DOMSource(document)
            val output = StreamResult(manifestTarget)
            transformer.transform(source, output)
        }
    }

    private fun openAndroidManifestXMLDocument(manifest: File, action: (doc: Document) -> Unit) {
        manifest.inputStream().buffered().use { stream ->
            runCatching {
                val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val document = builder.parse(stream)
                action(document)
            }
        }
    }
}
