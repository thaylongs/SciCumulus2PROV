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
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
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
    STRING("string"), FILE("file"), FLOAT("float"), OPERATOR("operator")
}

val ns: Namespace = Namespace()
val pFactory: ProvFactory = org.openprovenance.prov.xml.ProvFactory()

fun qn(qn: QualifiedNames, name: String?): QualifiedName {
    return ns.qualifiedName(qn.prefix, name, pFactory)
}

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

class DocumentBuilder(outputStream: OutputStream) {

    constructor(fileOut: File) : this(FileOutputStream(fileOut))

    private val SCICUMULUS_NS = "https://scicumulusc2.wordpress.com"
    private val SCICUMULUS_PREFIX = "sci"
    val document = pFactory.newDocument()
    val output = OutputStreamWriter(outputStream, "UTF-8")
    val nc: NotationConstructor
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

    fun newPlanEntity(id: String, label: String): Entity {
        val plan = pFactory.newEntity(qn(QualifiedNames.PROV, id), label)
        plan.setType("PLAN")
        return plan
    }

    fun newEntity(qn: QualifiedNames, id: String): Entity {
        val entity = pFactory.newEntity(qn(qn, id))
        return entity
    }

    fun newEntity(id: String): Entity {
        return newEntity(QualifiedNames.PROV, id)
    }

    fun newActivity(id: String, label: String, startTime: Timestamp, endTime: Timestamp): Activity {
        val from = DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime.toLocalDateTime().format(formatter))
        val end = DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime.toLocalDateTime().format(formatter))
        val act = pFactory.newActivity(qn(QualifiedNames.PROV, id), from, end, listOf())
        act.label.add(pFactory.newInternationalizedString(label))
        return act
    }

    fun newWasInformedBy(actid: Activity, dependency: Activity): WasInformedBy {
        val wasInformedBy = pFactory.newWasInformedBy(null, actid.id, dependency.id)
        return wasInformedBy
    }

    fun newWasGeneratedBy(entityID: String, actid: String): WasGeneratedBy {
        return pFactory.newWasGeneratedBy(null, qn(QualifiedNames.PROV, entityID), null, qn(QualifiedNames.PROV, actid))
    }

    fun newWasUseddBy(entityID: String, actid: String): Used {
        return pFactory.newUsed(qn(QualifiedNames.PROV, actid), qn(QualifiedNames.PROV, entityID))
    }

    fun newWasUseddBy(entityID: Entity, actid: Activity): Used {
        return pFactory.newUsed(actid.id, entityID.id)
    }

    /**
     * @param usedEntityID
     * @param generatedEntity
     * @param actid
     *
     */
    fun newWasDerivedFrom(usedEntityID: Entity, generatedEntity: Entity, usedFields: List<String>): WasDerivedFrom {
        var count = 1
        val res = pFactory.newWasDerivedFrom(generatedEntity.id, usedEntityID.id)
        res.activity = null
        res.generation = null
        res.usage = null
        pFactory.setAttributes(res, usedFields.map { pFactory.newAttribute(qn(QualifiedNames.SCICUMULUS, "column_${count++}"), it, null) })
        return res
    }

    fun newAgent(agentId: String, agentName: String): Agent {
        return pFactory.newAgent(qn(QualifiedNames.PROV, agentId), agentName)
    }

    fun newWasAssociatedWith(agentID: Agent, actId: Activity): WasAssociatedWith {
        return pFactory.newWasAssociatedWith(null, actId.id, agentID.id)
    }

    fun newHadMember(entityIDFrom: Entity, entityIDTarget: Entity): HadMember {
        val hadMember = pFactory.newHadMember(entityIDFrom.id, entityIDTarget.id)
        return hadMember
    }

    fun newWasAttributedTo(entity: Entity, agent: Agent): WasAttributedTo {
        val wasAttributedTo = pFactory.newWasAttributedTo(null, entity.id, agent.id)
        return wasAttributedTo
    }

    fun newActedOnBehalfOf(delegate: Agent, responsible: Agent): ActedOnBehalfOf {
        return pFactory.newActedOnBehalfOf(null, delegate.id, responsible.id)
    }

    fun initDocument() {
        val docNamespace = document.namespace
        Namespace.withThreadNamespace(docNamespace)
        nc.startDocument(document.namespace)
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