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

import br.uff.scicumulus2prov.model.CActivity
import br.uff.scicumulus2prov.model.CWorkflow
import br.uff.scicumulus2prov.model.EWorkflow
import org.openprovenance.prov.model.Activity
import org.openprovenance.prov.model.Agent
import org.openprovenance.prov.model.Entity
import org.openprovenance.prov.model.StatementOrBundle
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * @author Thaylon Guedes Santos
 * @email thaylongs@gmail.com
 */
class SciCumulus2PROV(dao: BasicDao, val workflowTag: String, val execTag: String, outputStream: OutputStream) {

    constructor(dao: BasicDao, workflowTag: String, execTag: String, fileOut: File) : this(dao, workflowTag, execTag, FileOutputStream(fileOut))

    private val conceptualDao = ConceptualDataDAO(dao)
    private val executionDao = ExecutionDataDAO(dao)
    private val document = DocumentBuilder(outputStream)

    fun start() {
        /*Searching Workflow Data From Database*/
        val workflow = conceptualDao.findCWorkflowByTAG(workflowTag)
        val eWorkflow = executionDao.findEWorkflowByTagAndExecTag(workflowTag, execTag)
        /*Process Plan Workflow Entity*/
        val planWorklfow = document.newPlanEntity("entity_workflow_" + workflow.tag, workflow.tag)
                .addAtt("NAME", workflow.tag, null)
                .addAtt("DESCRIPTION", workflow.description, null)
        document.writeElement(planWorklfow)
        /*Process Execution Workflow*/
        val startAndEndTimeOfWorkflow = executionDao.getStartAndEndTimeOfExecutionWorkflow(eWorkflow)
        val workflowAct = document.newActivity("act_" + execTag, "Execute Workflow", startTime = startAndEndTimeOfWorkflow[0], endTime = startAndEndTimeOfWorkflow[1])
        document.writeElement(workflowAct)
        document.writeElement(document.newWasUseddBy(planWorklfow, workflowAct))
        /*Process WActivities*/
        val WActivities = conceptualDao.findCActivitiesByWkfid(workflow.wkfid)

        val allTask = HashMap<String, Activity>()
        val allAWactivities = WActivities.map { cAct ->
            val eAct = executionDao.findEActivityByActAndEWorkflow(cAct, eWorkflow)
            val wActivity = document.newPlanEntity("act_" + eAct.actid, cAct.tag).addAtt("TYPE", cAct.atype, AttributeType.OPERATOR)
            var count = 1
            conceptualDao.getAllFieldOfActovity(cAct.actid).forEach { (fname, ftype) ->
                wActivity.addAtt("column_${count++}", fname, AttributeType.valueOf(ftype.toUpperCase()))
            }
            document.writeElement(wActivity)
            document.writeElement(document.newHadMember(planWorklfow, wActivity))
            executionDao.getEActivationsByEActivity(eAct).forEach {
                val task = document.newActivity("act_task" + it.taskid, "TaskOf:" + cAct.tag, it.starttime, it.endtime)
                task.setType("TASK")
                        .addAtt("status", it.status, null)
                        .addAtt("workspace", it.workspace, null)
                        .addAtt("extractor", it.extractor, null)
                        .addAtt("commandline", it.commandline, null)
                allTask.put("act_task" + it.taskid, task)
                document.writeElement(task)
                /*Create relationship*/
                document.writeElement(document.newWasUseddBy(wActivity, task))
                document.writeElement(document.newWasInformedBy(workflowAct, task))
            }
            wActivity//yield the Execution Activity
        }
        loadAllEntities(workflow, eWorkflow)
        loadAllwasUsedByAndWasGeneratedBy(eWorkflow)
        loadAllwasDerivedFrom(eWorkflow, WActivities)
        loadAllAgentsAndRelationship(eWorkflow, planWorklfow, allAWactivities, allTask)
        document.finishDocument()
    }

    private fun loadAllwasUsedByAndWasGeneratedBy(eworkflow: EWorkflow) {
        val result = executionDao.getKeySpaceData(eworkflow)
        result.rows().forEach { linha ->
            val taskId = "act_task" + linha.getString("taskid")
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

    private fun loadAllAgentsAndRelationship(eworkflow: EWorkflow, planWorklfow: Entity, allAWactivities: List<Entity>, allTask: HashMap<String, Activity>) {
        val scicumulusAgent = document.newAgent("softwareAgent", "SciCumulus")
        val scientistAgent = document.newAgent("scientistAgent", "Scientist")
        document.writeElement(scicumulusAgent)
        document.writeElement(scientistAgent)
        allAWactivities.forEach { document.writeElement(document.newWasAttributedTo(it, scicumulusAgent)) }
        allTask.forEach { document.writeElement(document.newWasAssociatedWith(scicumulusAgent, it.value)) }

        val machinesIDs = HashMap<String, Agent>()
        val usedMachines = executionDao.getAllUsedMachines(eworkflow)
        usedMachines.rows().forEach {
            val machineAgentId = "machine" + it.getString("machine_id")
            if (!machinesIDs.contains(machineAgentId)) {
                val machineAgent = document.newAgent(machineAgentId, "Machine")
                        .addAtt("hostname", it.getString("hostname"), null)
                        .addAtt("address", it.getString("address"), null)
                        .addAtt("mflopspersecond", it.getString("mflopspersecond"), null)
                document.writeElement(machineAgent)
                document.writeElement(document.newWasAttributedTo(planWorklfow, machineAgent))
                document.writeElement(document.newActedOnBehalfOf(machineAgent, scientistAgent))
                machinesIDs.put(machineAgentId, machineAgent)
            }
            document.writeElement(document.newWasAssociatedWith(machinesIDs[machineAgentId]!!, allTask["act_task" + it.getString("taskid")]!!))
        }
    }

    private fun loadAllwasDerivedFrom(eworkflow: EWorkflow, cActivities: List<CActivity>) {
        cActivities.forEach {
            val resultFromInputTableToOutputTable = conceptualDao.getAllDerivedOutputTablesValuesFromInputTables(it)
            resultFromInputTableToOutputTable.ifPresent {
                val tablesInfo = resultFromInputTableToOutputTable.get()
                val outputTableName = tablesInfo.toTable
                tablesInfo.fromData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromInputTablesToOutputTables(eworkflow, outputTableName, inputTableName)
                    for (ids in idsData) {
                        val fromEntityId = document.newEntity("${inputTableName}_${eworkflow.ewkfid}_${ids[0]}")
                        val toEntityId = document.newEntity("${outputTableName}_${eworkflow.ewkfid}_${ids[1]}")
                        document.writeElement(document.newWasDerivedFrom(fromEntityId, toEntityId, fields))
                    }
                }
            }
            val resultFromOutputTableToInputTable = conceptualDao.getAllDerivedInputTablesValuesFromOutputTables(it)
            resultFromOutputTableToInputTable.ifPresent {
                val tablesInfo = resultFromOutputTableToInputTable.get()
                val outputTableName = tablesInfo.fromTable
                tablesInfo.toData.forEach { inputTableName, fields ->
                    val idsData = conceptualDao.getExecutionsIDsFromOutpuTablesToInputTables(eworkflow = eworkflow, outputTableName = outputTableName, inputTableName = inputTableName)
                    for (ids in idsData) {
                        val input_ik = ids[0]
                        val output_ok = ids[1]
                        val fromEntityId = document.newEntity("${outputTableName}_${eworkflow.ewkfid}_$output_ok")
                        val toEntityId = document.newEntity("${inputTableName}_${eworkflow.ewkfid}_$input_ik")
                        val wasDerived = document.newWasDerivedFrom(fromEntityId, toEntityId, fields)
                        document.writeElement(wasDerived)
                    }
                }
            }
        }
    }

    private fun loadAllEntities(workflow: CWorkflow, eworkflow: EWorkflow) {
        val relations = conceptualDao.getAllRelation(workflow)
        relations.forEach { relation ->
            val table = executionDao.getAllDataFrom(eworkflow, relation)
            val colunasValidas = table.columns().filter { it.name != "ik" && it.name != "ewkfid" && it.name != "ok" }
            val containsOkColumn = table.columns().any { it.name == "ok" }
            for (linha in table.rows()) {
                val id = "${relation}_${eworkflow.ewkfid}_" + if (containsOkColumn) linha.getObject("ok") else linha.getObject("ik")
                val entity = document.newEntity(id)
                colunasValidas.forEach { coluna -> entity.addAtt(coluna.name, linha.getString(coluna.index), null) }
                document.writeElement(entity)
            }
        }
    }
}
