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
import org.openprovenance.prov.notation.Utility
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.util.*
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

class DocumentBuilder {

    private val SCICUMULUS_NS = "https://scicumulusc2.wordpress.com"
    private val SCICUMULUS_PREFIX = "sci"

    private val elements = ArrayList<StatementOrBundle>()

    init {
        ns.addKnownNamespaces()
        ns.register(SCICUMULUS_PREFIX, SCICUMULUS_NS)
    }

    fun addEntity(qn: QualifiedNames, id: String): Entity {
        val entity = pFactory.newEntity(qn(qn, id))
        elements.add(entity)
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

        elements.add(act)
        return act
    }

    fun addWasInformedBy(actid: Long, dependency: Long): WasInformedBy? {
        val wasInformedBy = pFactory.newWasInformedBy(null, qn(QualifiedNames.PROV, actid.toString()), qn(QualifiedNames.PROV, dependency.toString()))
        elements.add(wasInformedBy)
        return wasInformedBy
    }

    fun addWasInformedBy(relid: Long, actid: Long, dependency: Long): WasInformedBy? {
        val wasInformedBy = pFactory.newWasInformedBy(qn(QualifiedNames.PROV, relid.toString()), qn(QualifiedNames.PROV, actid.toString()), qn(QualifiedNames.PROV, dependency.toString()))
        elements.add(wasInformedBy)
        return wasInformedBy
    }

    fun build() {
        val document = pFactory.newDocument()
        document.statementOrBundle.addAll(elements)
        document.namespace = ns
        val u = Utility()
        println("****************************************")
        u.writeDocument(document, System.out, pFactory)
        println("****************************************")
    }


}


//fun Entity.setValue(pFactory: ProvFactory, value: Any) {
//    this.value = pFactory.newValue(value.toString(), pFactory.name.XSD_STRING)
//}
//
//fun Activity.addAttribute(pFactory: ProvFactory) {
//    pFactory.add (this, pFactory.newQualifiedName("var", "kmmk", "sddddd"))
//}
