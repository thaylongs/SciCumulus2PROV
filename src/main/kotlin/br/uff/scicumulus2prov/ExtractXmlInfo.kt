/*
 Copyright (c) 2017 Thaylon Guedes Santos and Eduardo Smil Prutchi

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package br.uff.scicumulus2prov

import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.Element
import org.dom4j.io.SAXReader

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
data class DataBaseConnectionInfo(var url: String, var userName: String, var password: String)

object ExtractXmlInfo {

    @Throws(DocumentException::class)
    fun getExpDir(xmlPath: File): File {
        val root = getRootElement(xmlPath)
        var value = root.element("executionWorkflow").attributeValue("expdir")
        if (value.contains("%=WFDIR%")) {
            value = value.replace("%=WFDIR%/", "")
            value = value.replace("%=WFDIR%", "")
            return Paths.get(value).toAbsolutePath().toFile()
        }
        return File(value)
    }

    @Throws(DocumentException::class)
    fun getAllNameActivity(xmlPath: File): List<String> {
        val rootElement = getRootElement(xmlPath)
        val temp = ArrayList<String>()
        val i = rootElement.element("conceptualWorkflow").elementIterator("activity")
        while (i.hasNext()) {
            val next = i.next() as Element
            val type = next.attributeValue("type")
            if (type == null || type != null && type != "MR_QUERY")
                temp.add(next.attributeValue("tag"))
        }
        return temp
    }

    @Throws(DocumentException::class)
    fun getAllActivityDirs(xmlPath: File): List<File> {
        val listFiles = ArrayList<File>()
        val expDir = getExpDir(xmlPath)
        for (activityName in getAllNameActivity(xmlPath)) {
            val fileActivity = File(expDir, activityName)
            println(fileActivity)
            listFiles.add(fileActivity)
        }
        return listFiles
    }

    @Throws(DocumentException::class)
    fun getDataBaseIp(xmlPath: File): String {
        val rootElement = getRootElement(xmlPath)
        return rootElement.element("database").attributeValue("server")
    }

    fun getDataBaseConnectionFromXml(xmlPath: File): Optional<DataBaseConnectionInfo> {
        try {
            val rootElement = getRootElement(xmlPath)
            val elemetInfo = rootElement.element("database")
            var url = "jdbc:postgresql://" + elemetInfo.attributeValue("server") + ':'
            url += elemetInfo.attributeValue("port") + '/'
            url += elemetInfo.attributeValue("name")
            val userName = elemetInfo.attributeValue("username")
            val password = elemetInfo.attributeValue("password")
            return Optional.of(DataBaseConnectionInfo(url, userName, password))
        } catch (ex: DocumentException) {
            Logger.getLogger(ExtractXmlInfo::class.java.name).log(Level.SEVERE, null, ex)
        }
        return Optional.empty()
    }

    @Throws(DocumentException::class)
    private fun getRootElement(xmlPath: File): Element {
        val reader = SAXReader()
        val document = reader.read(xmlPath)
        return document.rootElement
    }
}
