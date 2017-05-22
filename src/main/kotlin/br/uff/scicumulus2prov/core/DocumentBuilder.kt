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
package br.uff.scicumulus2prov.core

import org.openprovenance.prov.model.*
import org.openprovenance.prov.notation.NotationConstructor
import org.openprovenance.prov.notation.Utility
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import javax.xml.datatype.DatatypeFactory

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
enum class QualifiedNames(var prefix: String) {
    PROV("prov"), SCICUMULUS("sci"),
}

enum class AttributeType(var prefix: String) {
    STRING("string"), FILE("file"), FLOAT("float")
}

val ns: Namespace = Namespace()
val pFactory: ProvFactory = org.openprovenance.prov.xml.ProvFactory()

fun qn(qn: QualifiedNames, name: String): QualifiedName {
    return ns.qualifiedName(qn.prefix, name, pFactory)
}

var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

class DocumentBuilder(fileOut: File) {

    private val SCICUMULUS_NS = "https://scicumulusc2.wordpress.com"
    private val SCICUMULUS_PREFIX = "sci"
    val document = pFactory.newDocument()
    val output = OutputStreamWriter(FileOutputStream(fileOut), "UTF-8")
    private val nc: NotationConstructor
    private val bt: BeanTraversal
    private val u: ProvUtilities = ProvUtilities()

    init {
        ns.addKnownNamespaces()
        ns.register(SCICUMULUS_PREFIX, SCICUMULUS_NS)
        document.namespace = ns
        nc = NotationConstructor(output)
        bt = BeanTraversal(nc, pFactory)
        initDocument()
    }

    fun addEntity(qn: QualifiedNames, id: String): Entity {
        val entity = pFactory.newEntity(qn(qn, id))
        return entity
    }

    fun addEntity(id: String): Entity {
        return addEntity(QualifiedNames.PROV, id)
    }

    fun addActicity(id: String, label: String, startTime: Timestamp, endTime: Timestamp): Activity {
        val from = DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime.toLocalDateTime().format(formatter))
        val end = DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime.toLocalDateTime().format(formatter))
        val act = pFactory.newActivity(qn(QualifiedNames.PROV, id), from, end, listOf())
        act.label.add(pFactory.newInternationalizedString(label))
        return act
    }

    fun addWasInformedBy(actid: Long, dependency: Long): WasInformedBy {
        val wasInformedBy = pFactory.newWasInformedBy(null, qn(QualifiedNames.PROV, actid.toString()), qn(QualifiedNames.PROV, dependency.toString()))
        return wasInformedBy
    }

    fun addWasInformedBy(relid: Long, actid: Long, dependency: Long): WasInformedBy? {
        val wasInformedBy = pFactory.newWasInformedBy(qn(QualifiedNames.PROV, relid.toString()), qn(QualifiedNames.PROV, actid.toString()), qn(QualifiedNames.PROV, dependency.toString()))
        return wasInformedBy
    }


    fun initDocument() {
        val docNamespace = document.getNamespace()
        Namespace.withThreadNamespace(docNamespace)
        nc.startDocument(document.getNamespace())
        nc.flush()
    }

    fun writeElement(element: StatementOrBundle) {
        if (element is Statement) {
            u.doAction(element, bt)
        }
        if (element is Bundle) {
            Namespace.withThreadNamespace(Namespace(document.namespace))
            bt.doAction(element, u)
        }
    }

    fun finishDocument() {
        nc.newDocument(null, null, null)
        nc.flush()
        nc.close()
    }
}