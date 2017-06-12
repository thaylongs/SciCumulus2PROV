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

import br.uff.scicumulus2prov.model.CWorkflow
import br.uff.scicumulus2prov.model.EActivity
import br.uff.scicumulus2prov.model.EWorkflow
import org.openprovenance.prov.model.StatementOrBundle
import java.io.File
import java.sql.Timestamp

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class SciCumulus2PROV(dao: BasicDao, val workflowTag: String, val execTag: String, fileOut: File) {

    private val conceptualDao = ConceptualDataDAO(dao)
    private val executionDao = ExecutionDataDAO(dao)
    private val document = DocumentBuilder(fileOut)

    fun start() {
        val workflow = conceptualDao.findCWorkflowByTAG(workflowTag)
        val eworkflow = executionDao.findEWorkflowByTagAndExecTag(workflowTag, execTag)
        val atividades = conceptualDao.findCActivitiesByWkfid(workflow.wkfid)

        val eAtividades = atividades.map { cAct ->
            val eAct = executionDao.findEActivityByActAndEWorkflow(cAct, eworkflow)
            val act = document.newActivity("a" + eAct.actid, cAct.tag, eAct.starttime, eAct.endtime)
            act.setType(cAct.atype)
            conceptualDao.getAllFieldOfActovity(cAct.actid).forEach { field ->
                act.addAttribute("column", field.fname, AttributeType.valueOf(field.ftype.toUpperCase()))
            }
            document.writeElement(act)
            executionDao.getEActivationsByEActivity(eAct).forEach {
                val task = document.newActivity("task" + it.taskid, "TaskOf:" + cAct.tag, it.starttime, it.endtime)
                task.setType("TASK")
                        .addAttribute("status", it.status, null)
                        .addAttribute("workspace", it.workspace, null)
                        .addAttribute("extractor", it.extractor, null)
                        .addAttribute("commandline", it.commandline, null)
                document.writeElement(task)
            }
            eAct
        }
        eAtividades.forEach { loadWasInformedBy(it, eworkflow) }
        loadAllEntities(workflow, eworkflow)
        loadAllwasUsedByAndWasGeneratedBy(eworkflow)
        loadAllwasDerivedFrom(eworkflow, eAtividades)
        loadAllAgents(eAtividades)
        loadAllWasStartedBy(eworkflow)
        document.finishDocument()
    }

    private fun loadAllwasUsedByAndWasGeneratedBy(eworkflow: EWorkflow) {
        val result = executionDao.getKeySpaceData(eworkflow)
        result.rows().forEach { linha ->
            val taskId = "task" + linha.getString("taskid")
            val rname = linha.getString("relationname")
            val relationtype = linha.getString("relationtype")
            val id = "${rname}_${eworkflow.ewkfid}_${linha.getObject("fik")}"
            val obj = when (relationtype) {
                "INPUT" -> document.newWasUseddBy(id, taskId)
                "OUTPUT" -> document.newWasGeneratedBy(id, taskId)
                else -> null
            }
            document.writeElement(obj as StatementOrBundle)
        }
    }

    private fun loadAllWasStartedBy(eworkflow: EWorkflow) {
        val relatiosnIds = executionDao.getAllRelationBetweenActivityAndActivation(eworkflow)
        relatiosnIds.rows().forEach {
            document.writeElement(document.newWasStartedBy("a" + it.getString("actid"), "task" + it.getString("taskid"), it.getObject("starttime") as Timestamp))
        }
    }

    private fun loadAllAgents(eAtividades: List<EActivity>) {
        val agent = document.newAgent("agentSci", "SciCumulus")
        document.writeElement(agent)
        eAtividades.forEach {
            document.writeElement(document.wasAssociatedWith("agentSci", "a" + it.actid))
        }
    }

    private fun loadAllwasDerivedFrom(eworkflow: EWorkflow, eAtividades: List<EActivity>) {
        eAtividades.forEach { eAct ->
            val resultFromInputTableToOutputTable = conceptualDao.getAllDerivedOutputTablesValuesFromInputTables(eAct)
            resultFromInputTableToOutputTable.ifPresent {
                val tablesInfo = resultFromInputTableToOutputTable.get()
                val outputTableName = tablesInfo.toTable
                tablesInfo.fromData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromInputTablesToOutputTables(eworkflow, outputTableName, inputTableName)
                    for (ids in idsData) {
                        val actID = "a" + eAct.actid
                        val fromEntityId = "${inputTableName}_${eworkflow.ewkfid}_${ids[0]}"
                        val toEntityId = "${outputTableName}_${eworkflow.ewkfid}_${ids[1]}"
                        val wasDerived = document.newwasDerivedFrom(fromEntityId, toEntityId, actID, fields)
                        document.writeElement(wasDerived)
                    }
                }
            }
            val resultFromOutputTableToInputTable = conceptualDao.getAllDerivedInputTablesValuesFromOutputTables(eAct)
            resultFromOutputTableToInputTable.ifPresent {
                val tablesInfo = resultFromOutputTableToInputTable.get()
                val outputTableName = tablesInfo.fromTable
                tablesInfo.toData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromOutpuTablesToInputTables(eworkflow = eworkflow, outputTableName = outputTableName, inputTableName = inputTableName)
                    for (ids in idsData) {
                        val actID = "a" + eAct.actid
                        val input_ik = ids[0]
                        val output_ok = ids[1]
                        val fromEntityId = "${outputTableName}_${eworkflow.ewkfid}_$output_ok"
                        val toEntityId = "${inputTableName}_${eworkflow.ewkfid}_$input_ik"
                        val wasDerived = document.newwasDerivedFrom(fromEntityId, toEntityId, actID, fields)
                        document.writeElement(wasDerived)
                    }
                }
            }
        }
    }

    private fun loadAllEntities(workflow: CWorkflow, eworkflow: EWorkflow) {
        val machinesIDs = HashSet<String>()
        val usedMachines = executionDao.getAllUsedMachines(eworkflow)
        usedMachines.rows().forEach {
            val id = "machine" + it.getString("machine_id")
            if (!machinesIDs.contains(id)) {
                val entity = document.addEntity(id)
                        .addAttribute("hostname", it.getString("hostname"), null)
                        .addAttribute("address", it.getString("address"), null)
                        .addAttribute("mflopspersecond", it.getString("mflopspersecond"), null)
                document.writeElement(entity)
                machinesIDs.add(id)
            }
            document.writeElement(document.newWasUseddBy(id, "task" + it.getString("taskid")))
        }
        val relations = conceptualDao.getAllRelation(workflow)
        relations.forEach { relation ->
            val table = executionDao.getAllDataFrom(eworkflow, relation)
            val colunasValidas = table.columns().filter { it.name != "ik" && it.name != "ewkfid" && it.name != "ok" }
            val containsOkColumn = table.columns().any { it.name == "ok" }
            for (linha in table.rows()) {
                val id = "${relation}_${eworkflow.ewkfid}_" + if (containsOkColumn) linha.getObject("ok") else linha.getObject("ik")
                val entity = document.addEntity(id)
                for (coluna in colunasValidas) {
                    entity.addAttribute(coluna.name, linha.getString(coluna.index), null)
                }
                document.writeElement(entity)
            }
        }
    }

    private fun loadWasInformedBy(act: EActivity, eworkflow: EWorkflow) {
        val relations = executionDao.getActivitieDependency(act, eworkflow)
        relations.forEach { document.writeElement(document.newWasInformedBy("a" + it, "a" + act.actid)) }
    }
}
